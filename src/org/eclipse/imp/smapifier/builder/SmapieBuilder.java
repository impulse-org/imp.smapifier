/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/

package org.eclipse.imp.smapifier.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.java.hosted.BuildPathUtils;
import org.eclipse.imp.smapi.Main;
import org.eclipse.imp.smapifier.SMAPifierPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class SmapieBuilder extends IncrementalProjectBuilder {
    public static final String BUILDER_ID= SMAPifierPlugin.kPluginID + ".SmapieBuilder";

    private IProject fProject;

    private IJavaProject fJavaProject;

    private String fPathPrefix;

    private final DeltaVisitor fDeltaVisitor= new DeltaVisitor();
    private final ResourceVisitor fResourceVisitor= new ResourceVisitor();

    private IProgressMonitor fMonitor;

    // RMF 8/4/2006 - This can't be static; different projects in the same workspace
    // could be configured to run the SMAP builder for different file-name extensions.
    private String fFileExten= "";

    public String getOrigExten() {
        return fFileExten;
    }

    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
        fProject= getProject();
        fJavaProject= JavaCore.create(fProject);
        fMonitor= monitor;
        fPathPrefix= fProject.getWorkspace().getRoot().getRawLocation() + fProject.getFullPath().toString();

        // RMF 3/25/2006: Pick up the file name extension from the "args" map.
        // This comes from the .project file, and looks like this:
        //
        // <buildCommand>
        //   <name>org.eclipse.imp.smapifier.SmapieBuilder</name>
        //   <arguments>
        //     <dictionary>
        //       <key>exten</key>
        //       <value>leg</value>
        //     </dictionary>
        //   </arguments>
        // </buildCommand>
        //
        // Presumably it was placed there by whatever configured the SMAPI builder on the project.

        if (args.get("exten") != null)
            fFileExten= (String) args.get("exten");

        IResourceDelta delta= getDelta(fProject);

        if (delta != null) {
//          SMAPifierPlugin.getDefault().maybeWriteInfoMsg("==> SMAPI Scanning resource delta for project '" + fProject.getName() + "'... <==");
            delta.accept(fDeltaVisitor);
//          if (SMAPifierPlugin.getDefault() != null) { // SMAPIE plugin already shut down?
//              SMAPifierPlugin.getDefault().maybeWriteInfoMsg("SMAPI delta scan completed for project '" + fProject.getName() + "'...");
//          }
        } else {
//          SMAPifierPlugin.getDefault().maybeWriteInfoMsg("==> SMAPI Scanning for '." + fFileExten + "' source files in project '" + fProject.getName() + "'... <==");
            fProject.accept(fResourceVisitor);
//          if (SMAPifierPlugin.getDefault() != null) { // SMAPIE plugin already shut down?
//              SMAPifierPlugin.getDefault().maybeWriteInfoMsg("SMAPI source file scan completed for project '" + fProject.getName() + "'...");
//          }
        }

        IProject[] ret= new IProject[] { fProject };

        refresh();
        return ret;
    }

    private final class DeltaVisitor implements IResourceDeltaVisitor {
        public boolean visit(IResourceDelta delta) throws CoreException {
            if (delta.getKind() == IResourceDelta.REMOVED) {
                return false;
            }
            return processResource(delta.getResource());
        }
    }

    private class ResourceVisitor implements IResourceVisitor {
        public boolean visit(IResource res) throws CoreException {
            return processResource(res);
        }
    }

    protected boolean processResource(IResource resource) {
        if (resource instanceof IFile) {
            IFile srcFile= (IFile) resource;

            if (!isSourceFile(srcFile))
                return false;

            String srcFileLoc= srcFile.getRawLocation().toString();
            Set<IFile> classFiles= getClassFiles(srcFile);


            for(IFile classFile: classFiles) {
                String generatedFile= getMainGeneratedFile(srcFile);
                if (generatedFile != null) {
                    Main.smapify(srcFileLoc, fPathPrefix, generatedFile, classFile.getRawLocation().toString());
                }
            }
        }
        return true;
    }

    private boolean isSourceFile(IFile file) {
        // RMF 7/7/2008 - Don't just look at the end of the path, since the path may have
        // no file extension; only look at the file extension, if there is one.
        // (X10DT Bug #500)
        String fileExten= file.getFileExtension();
        return (fileExten != null) && fileExten.equals(fFileExten);
    }

    /**
     * @return the set of <code>IFiles</code> representing all of the .class files that
     * were generated from the given source file
     */
    private Set<IFile> getClassFiles(IFile srcFile) {
        IPath parentPath= srcFile.getParent().getFullPath();
        Set<IFile> ret= new HashSet<IFile>();
        try {
            IClasspathEntry[] entries= fJavaProject.getResolvedClasspath(true);
            for(int i= 0; i < entries.length; i++) {
            	if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE && entries[i].getPath().isPrefixOf(srcFile.getFullPath())
              		  && !BuildPathUtils.isExcluded(srcFile.getFullPath(), entries[i])) {
            	    IPath out= entries[i].getOutputLocation();

            	    if (out == null) // using default output folder
            	        out= fJavaProject.getOutputLocation().removeFirstSegments(1);

            	    IPath parentSrcPath= parentPath.removeFirstSegments(entries[i].getPath().segmentCount());
            	    IPath parentFullPath= out.append(parentSrcPath);
            	    final IResource parent= fProject.findMember(parentFullPath);

            	    // RMF 8/4/2006 - If the SMAP builder ran before the Java builder
            	    // (e.g. due to a misconfigured project), the output folder might
            	    // have been cleaned out, and sub-folders (corresponding to packages)
            	    // won't exist, so parent could be null.
            	    if (parent == null)
            	        continue;

            	    IResource[] members= ((IContainer) parent).members();

            	    for(int j= 0; j < members.length; j++) {
            	        String name= members[j].getName();
            	        if (members[j] instanceof IFile && classBelongsTo(srcFile, name)) {
            	            ret.add((IFile) members[j]);
            	        }
                    }
                }
            }
        } catch (JavaModelException e) {
            SMAPifierPlugin.getInstance().logException(e.getMessage(), e);
        } catch (CoreException e) {
            SMAPifierPlugin.getInstance().logException(e.getMessage(), e);
        }
        return ret;
    }

    /**
     * @param origSrcFile the workspace-relative path to a source file in the top-level
     * language (from which Java source is generated)
     * @return the OS-specific-format filesystem-absolute path to the "main" corresponding .java file
     */
    private String getMainGeneratedFile(final IFile origSrcFile)  {
        IWorkspaceRoot wsRoot = fProject.getWorkspace().getRoot();
        final IPath wsRelativeFilePath = origSrcFile.getFullPath();

        // First, try the same folder as the original src file -- the .java file might live there
        final IPath javaSrcPath = wsRelativeFilePath.removeFileExtension().addFileExtension("java");
        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(wsRoot.getFile(javaSrcPath).getLocationURI());

        if (fileStore.fetchInfo().exists()) {
            return javaSrcPath.toOSString();
        }

        final IPath wsLocation = wsRoot.getRawLocation();
        final IPath projRelJavaFilePath = wsRelativeFilePath.removeFirstSegments(1).removeFileExtension().addFileExtension("java").makeAbsolute();

    	try {
    		for (final IClasspathEntry cpEntry : fJavaProject.getRawClasspath()) {
    			if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE && cpEntry.getPath().isPrefixOf(origSrcFile.getFullPath())
                		  && !BuildPathUtils.isExcluded(origSrcFile.getFullPath(), cpEntry)){
    			    // Skip if this source entry isn't the entry containing 'origSrcFile'.
                    final IPath srcPath = cpEntry.getPath();

                    if (!srcPath.isPrefixOf(wsRelativeFilePath)) {
                        continue;
                    }

    				// Now look in the output locations - the one for this classpath entry, if any,
    				// or, failing that, the project's output location.
    			    final IPath outputLocation;

                    if (cpEntry.getOutputLocation() == null) {
                        outputLocation = fJavaProject.getOutputLocation();
                    } else {
                        outputLocation = cpEntry.getOutputLocation();
                    }

    				final int srcPathCount = srcPath.removeFirstSegments(1).segmentCount(); // discounting the project name
                    final IPath generatedFilePath = wsLocation.append(outputLocation.append(projRelJavaFilePath.removeFirstSegments(srcPathCount)));
    				final String generatedFilePathStr = generatedFilePath.toOSString();

    				if (new File(generatedFilePathStr).exists()) {
    				    return generatedFilePath.toOSString();
    				}
    			}
    		}
    	} catch (CoreException e) {
    		SMAPifierPlugin.getInstance().logException(e.getMessage(), e);
    	}
        return null;
    }
    
    /**
     * @return true if <code>otherFileName</code> names a class file that was generated
     * from the source file <code>srcFile</code>
     */
    private boolean classBelongsTo(IFile srcFile, String otherFileName) {
        if (!otherFileName.endsWith(".class"))
            return false;
        otherFileName= otherFileName.substring(0, otherFileName.indexOf("."));
        String fileName= srcFile.getFullPath().removeFileExtension().lastSegment();
        if (fileName.equals(otherFileName))
            return true;
        if (otherFileName.startsWith(fileName) && (otherFileName.indexOf("$") == fileName.length())) {
            return true;
        }
        return false;
    }

    protected boolean isBinaryFolder(IResource resource) {
        try {
            // RMF The following commented-out version is incorrect for projects
            // whose source and output folders are the project root folder.
            // IPath bin = JavaCore.create(fProject).getOutputLocation();
            // return resource.getFullPath().equals(bin);

            //
            // RMF The right check is: resource path is one of the project's output folders.
            // Not sure the following is quite right...
            //
            // Caching would be a really good idea here: save the set of project
            // output folders in a Set. Seems Path implements equals() properly.
            IPath projPath= fProject.getFullPath();
            boolean projectIsSrcBin= fJavaProject.getOutputLocation().matchingFirstSegments(projPath) == projPath.segmentCount();

            if (projectIsSrcBin)
                return false;

            final IPath resourcePath= resource.getFullPath();

            if (resourcePath.equals(fJavaProject.getOutputLocation())) return true;

            IClasspathEntry[] cp= fJavaProject.getResolvedClasspath(true);

            for(int i= 0; i < cp.length; i++) {
                if (cp[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    if (resourcePath.equals(cp[i].getOutputLocation()))
                        return true;
                }
            }
        } catch (JavaModelException e) {
            SMAPifierPlugin.getInstance().logException(e.getMessage(), e);
        }
        return false;
    }

    private void refresh() throws CoreException {
        for(IPath pathEntry : getProjectSrcPath()) {
            if (pathEntry.segmentCount() == 1)
                // Work around Eclipse 3.1.0 bug 101733: gives spurious exception
                // if folder refers to project itself (happens when a Java project
                // is configured not to use separate src/bin folders).
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=101733
                fProject.refreshLocal(IResource.DEPTH_INFINITE, fMonitor);
            else
                fProject.getWorkspace().getRoot().getFolder(pathEntry).refreshLocal(IResource.DEPTH_INFINITE, fMonitor);
        }
    }

    private List<IPath> getProjectSrcPath() throws JavaModelException {
        List<IPath> srcPath= new ArrayList<IPath>();
        IClasspathEntry[] classPath= fJavaProject.getResolvedClasspath(true);

        for(int i= 0; i < classPath.length; i++) {
            IClasspathEntry e= classPath[i];

            if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE)
                srcPath.add(e.getPath());
        }
        if (srcPath.size() == 0)
            srcPath.add(fProject.getLocation());
        return srcPath;
    }
}
