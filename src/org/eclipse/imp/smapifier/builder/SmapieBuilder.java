package com.ibm.watson.smapifier.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.watson.safari.x10.X10Plugin;
import com.ibm.watson.smapi.Main;
import com.ibm.watson.smapifier.SmapiePlugin;

public class SmapieBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_ID= SmapiePlugin.kPluginID + ".SmapieBuilder";
	
	private IProject fProject;
	private String fPathPrefix;
	private final DeltaVisitor fDeltaVisitor= new DeltaVisitor();
	private final ResourceVisitor fResourceVisitor= new ResourceVisitor();

	private IProgressMonitor fMonitor;

	
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		fProject = getProject();
		fMonitor = monitor;
		fPathPrefix = fProject.getWorkspace().getRoot().getRawLocation() + fProject.getFullPath().toString();

		System.out.println("Inside SMAP builder");
		IResourceDelta delta= getDelta(fProject);

		if (delta != null) {
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("==> Smapi Scanning resource delta for project '" + fProject.getName() + "'... <==");
		    delta.accept(fDeltaVisitor);
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("Smapi delta scan completed for project '" + fProject.getName() + "'...");
		} else {
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("==> Smapi Scanning for X10 source files in project '" + fProject.getName() + "'... <==");
		    fProject.accept(fResourceVisitor);
		    SmapiePlugin.getDefault().maybeWriteInfoMsg("Smapi X10 source file scan completed for project '" + fProject.getName() + "'...");
		}
		
		IProject[] ret = new IProject[] { fProject };

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
			IFile classFile = getClassFile(file);
			
			if (classFile !=null)
				System.out.println("classFile.exists = " + classFile.exists() + " and existsJava = " + existsJava(file));
			
		    if (isSourceFile(file) && (classFile != null) && classFile.exists() && existsJava(file)){
		    	System.out.println("smapifying " + file.getRawLocation());
		    	Main.smapify(makeJavaFile(file.getRawLocation().toString()), fPathPrefix, classFile.getRawLocation().toString());
		    	
		    }
		} else if (isBinaryFolder(resource))
		    return false;
		return true;
	}
	
	private IFile getClassFile(IFile file){
		IJavaProject javaProject = JavaCore.create(fProject);
		try {
			IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
			for (int i = 0; i < entries.length ; i++){
			
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE){
					if (file.getFullPath().matchingFirstSegments(entries[i].getPath()) == entries[i].getPath().segmentCount()){
						IPath out = entries[i].getOutputLocation();
						String outLocation = null;
						if (out == null)
							outLocation = javaProject.getOutputLocation().removeFirstSegments(1).toString();
						else
							outLocation = out.toString();
						
						int count = file.getFullPath().matchingFirstSegments(entries[i].getPath());
						String rest = file.getFullPath().removeFirstSegments(count).removeFileExtension().toString();
						outLocation += "/" + rest + ".class";
						System.out.println("outLocation " + outLocation);
						return fProject.getFile(outLocation);
					}
				}
			}
		} catch (JavaModelException e) {
			System.err.println(e);
		}
		return null;
	}
	
	private String makeJavaFile(String filename){
		int i = filename.lastIndexOf(".");
		return filename.substring(0, i) + ".java";
	}
	
	protected boolean isSourceFile(IFile file) {
		String exten= file.getFileExtension();

		return file.exists() && exten != null && exten.compareTo("x10") == 0;
	}
	
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

	protected boolean existsJava(IFile file) {
		IPath fullPath = file.getProjectRelativePath(); 
		fullPath = fullPath.removeFileExtension();
		String jname = fullPath.toString() + ".java";
		IFile jfile = fProject.getFile(jname);
		return (jfile.exists());
	}
	
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
	
	private List/*<IPath>*/ getProjectSrcPath() throws JavaModelException {
		List/* <IPath> */srcPath= new ArrayList();
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
