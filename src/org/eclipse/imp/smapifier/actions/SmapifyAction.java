package com.ibm.watson.smapifier.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import com.ibm.watson.smapifier.builder.SmapiProjectNature;


public class SmapifyAction extends Action implements IActionDelegate {
    private IProject fProject;
    
	public SmapifyAction(){
		
	}

	public void run(IAction action) {
		new SmapiProjectNature().addToProject(fProject);
		
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
		    IStructuredSelection ss= (IStructuredSelection) selection;
		    Object first= ss.getFirstElement();
		    if (first instanceof IProject) {
			fProject= (IProject) first;
		    } else if (first instanceof IJavaProject) {
			fProject= ((IJavaProject) first).getProject();
		    }
		}
	}
}
