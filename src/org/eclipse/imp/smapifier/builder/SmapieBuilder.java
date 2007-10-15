package org.eclipse.imp.smapifier.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.utils.BreakpointUtils;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.imp.smapi.Main;
import org.eclipse.imp.smapifier.SmapiePlugin;

public class SmapieBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_ID= SmapiePlugin.kPluginID + ".SmapieBuilder";
	
	private IProject fProject;
	private String fPathPrefix;
	private final DeltaVisitor fDeltaVisitor= new DeltaVisitor();
	private final ResourceVisitor fResourceVisitor= new ResourceVisitor();

	private IProgressMonitor fMonitor;

	// RMF 8/4/2006 - This can't be static; different projects in the same workspace
	// could be configured to run the SMAP builder for different file-name extensions.
	private String fFileExten= "x10";

	public String getOrigExten(){
	    return fFileExten;
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {

		fProject = getProject();
		fMonitor = monitor;
		fPathPrefix = fProject.getWorkspace().getRoot().getRawLocation() + fProject.getFullPath().toString();

                // RMF 3/25/2006: Pick up the file name extension from the "args" parameter.
                // This comes from the .project file, and looks like this:
                //
                //   <buildCommand>
                //     <name>org.eclipse.imp.smapifier.SmapieBuilder</name>
                //     <arguments>
                //       <dictionary>
                //         <key>exten</key>
                //         <value>leg</value>
                //       </dictionary>
                //     </arguments>
                //   </buildCommand>
                //
                // Presumably it got there by whatever configured the SMAPI builder on the project.

                if (args.get("exten") != null)
                    fFileExten= (String) args.get("exten");

                if (Main.debug)
                    System.out.println("Inside SMAP builder");
		IResourceDelta delta= getDelta(fProject);

		
		if (delta != null) {
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("==> Smapi Scanning resource delta for project '" + fProject.getName() + "'... <==");
		    delta.accept(fDeltaVisitor);
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("Smapi delta scan completed for project '" + fProject.getName() + "'...");
		} else {
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("==> Smapi Scanning for '." + fFileExten + "' source files in project '" + fProject.getName() + "'... <==");
		    fProject.accept(fResourceVisitor);
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("Smapi source file scan completed for project '" + fProject.getName() + "'...");
		}
		
		IProject[] ret = new IProject[] { fProject };

		if (Main.debug)
		    System.out.println("Leaving SMAP builder");
		refresh();
		return ret;
	}
	
	
	private final class DeltaVisitor implements IResourceDeltaVisitor {
		public boolean visit(IResourceDelta delta) throws CoreException {
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
			
			IFile file= (IFile) resource;
			
			if (!isSourceFile(file))
				return false; 
			
			String filename = file.getRawLocation().toString();
			Set /*IResource*/ classFiles = getClassFiles(file);
			
			BreakpointUtils.resetJavaBreakpoints(file);
				
			for(Iterator t = classFiles.iterator(); t.hasNext(); ){
				IFile classFile = (IFile) t.next();
				
				Main.smapify(filename, fPathPrefix, classFile.getRawLocation().toString());
				
			}
			
		} 
		return true;
	}
	
	private boolean isSourceFile(IFile file) {
		return file.getRawLocation().toString().endsWith(fFileExten);
	}
	
	

	private Set /*IFile*/ getClassFiles(IFile file){
		IJavaProject javaProject = JavaCore.create(fProject);
		IPath parentPath = file.getParent().getFullPath();
		Set<IResource> ret = new HashSet<IResource>();
		try {
			IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
			for (int i = 0; i < entries.length ; i++){
			
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE){
					if (file.getFullPath().matchingFirstSegments(entries[i].getPath()) == entries[i].getPath().segmentCount()){
						IPath out = entries[i].getOutputLocation();
						
					    if (out == null) //using default output folder
							out = javaProject.getOutputLocation().removeFirstSegments(1);
						
						
					    IPath parentSrcPath = parentPath.removeFirstSegments(entries[i].getPath().segmentCount());
					    
					    IPath parentFullPath = out.append(parentSrcPath);
					    final IResource parent= fProject.findMember(parentFullPath);

					    // RMF 8/4/2006 - If the SMAP builder ran before the Java builder,
					    // the output folder might have been cleaned out, and sub-folders
					    // (corresponding to packages) won't exist, so parent could be null.
					    if (parent == null)
						continue;

					    IResource[] members = ((IContainer) parent).members();

					    for(int j = 0; j < members.length; j++){
					    	String name = members[j].getName(); 
					    	if (classBelongsTo(file, name))
					    		ret.add(members[j]);
					    }
					    
					}
				}
			}
		} catch (JavaModelException e) {
			System.err.println(e);
		} catch (CoreException e){
			System.err.println(e);
		}
		return ret;
	}
	
	private boolean classBelongsTo(IFile file, String name){
		if (!name.endsWith(".class"))
			return false;
		name = name.substring(0,name.indexOf("."));
		String fileName = file.getFullPath().removeFileExtension().lastSegment();
		if (fileName.equals(name))
			return true;
		if (name.startsWith(fileName) && (name.indexOf("$") == fileName.length() )){
			return true;
		}
		return false;
	}
	
	/*protected boolean existsSourceFile(IFile file) {

		String exten= file.getFileExtension();

		return file.exists() && exten != null && exten.compareTo(fFileExten) == 0;
	}*/
	
	private boolean isBinaryFolder(IResource resource) {
		try {
		    // RMF The following commented-out version is incorrect for projects
		    // that have their source=output=project root folder.
//			IPath bin = JavaCore.create(fProject).getOutputLocation();
//			return resource.getFullPath().equals(bin);

		    //
		    // RMF The right check is: resource path is one of the project's output folders.
		    // Not sure the following is quite right...
		    //
		    // Caching would be a really good idea here: save the set of project
		    // output folders in a Set. Seems Path implements equals() properly.
		    IPath projPath= fProject.getFullPath();
		    final IJavaProject javaProj= JavaCore.create(fProject);
		    boolean projectIsSrcBin= javaProj.getOutputLocation().matchingFirstSegments(projPath) == projPath.segmentCount();

		    if (projectIsSrcBin) return false;

		    final IPath resourcePath= resource.getFullPath();

		    if (resourcePath.equals(javaProj.getOutputLocation())) return true;

		    IClasspathEntry[] cp= javaProj.getResolvedClasspath(true);

		    for(int i= 0; i < cp.length; i++) {
			if (cp[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
			    if (resourcePath.equals(cp[i].getOutputLocation()))
				return true;
			}
		    }
		} catch (JavaModelException e) {
			System.err.println(e);
		}
		return false;
	}

	/*protected boolean existsJava(IFile file) {
		IPath fullPath = file.getProjectRelativePath(); 
		fullPath = fullPath.removeFileExtension();
		String jname = fullPath.toString();
		int i = jname.indexOf("$");
		if (i != -1)
			jname = jname.substring(0,i) + ".java";
		else
			jname = jname + ".java";
		IFile jfile = fProject.getFile(jname);
		return (jfile.exists());
	}*/
	
	private void refresh() throws CoreException{
		List/*<IPath>*/ projectSrcPath= getProjectSrcPath();

	    for(Iterator iter= projectSrcPath.iterator(); iter.hasNext(); ) {
	    	IPath pathEntry= (IPath) iter.next();
		
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
		IJavaProject javaProject= JavaCore.create(fProject);
		IClasspathEntry[] classPath= javaProject.getResolvedClasspath(true);

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
