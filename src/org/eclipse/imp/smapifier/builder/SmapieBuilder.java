package com.ibm.watson.smapifier.builder;

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

	
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		fProject = getProject();
		
		fPathPrefix = fProject.getWorkspace().getRoot().getRawLocation() + fProject.getFullPath().toString();
		
		
		System.out.println("inside Smap builder");
		IResourceDelta delta= getDelta(fProject);

		if (delta != null) {
		    //X10Plugin.getInstance().maybeWriteInfoMsg("==> Scanning resource delta for project '" + fProject.getName() + "'... <==");
		    delta.accept(fDeltaVisitor);
		    //X10Plugin.getInstance().maybeWriteInfoMsg("X10 delta scan completed for project '" + fProject.getName() + "'...");
		} else {
		    //X10Plugin.getInstance().maybeWriteInfoMsg("==> Scanning for X10 source files in project '" + fProject.getName() + "'... <==");
		    fProject.accept(fResourceVisitor);
		    //X10Plugin.getInstance().maybeWriteInfoMsg("X10 source file scan completed for project '" + fProject.getName() + "'...");
		}
		
		IProject[] ret = new IProject[1];
		ret[0] = fProject;
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
					if (entries[i].getPath().append(file.getName()).equals(file.getFullPath())){
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
		return resource.getFullPath().lastSegment().equals("bin");
	}

	protected boolean existsJava(IFile file) {
		IPath fullPath = file.getProjectRelativePath(); 
		fullPath = fullPath.removeFileExtension();
		String jname = fullPath.toString() + ".java";
		IFile jfile = fProject.getFile(jname);
		return (jfile.exists());
	}
}
