package org.eclipse.imp.smapifier.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import org.eclipse.imp.smapifier.builder.SmapiProjectNature;

/**
 * Action to enable the SMAP builder on the currently-selected project.<br>
 * RMF 8/4/2006 --
 * This looks generic, but at the moment is hard-wired in a trivial way for X10.
 * See the associated action definition in plugin.xml, whose label makes that obvious.
 * Previously, the SMAP builder was generic, and relied on builder arguments (defined
 * in the .project) to specify the file-name extension of the files to process. Noone
 * was specifying builder arguments, and the builder defaulted to X10, so it worked.
 * Now the SMAP nature ctor takes a file-name extension argument, but we only have
 * this one action to manually enable the SMAP builder on a project. Until such time
 * as we enhance the action to ask what files to process, we leave it hard-wired for
 * X10, which is the only language prepared to actually generate what SMAPI needs.
 */
public class SmapifyAction extends Action implements IActionDelegate {
	private IProject fProject;

	public SmapifyAction() {
	}

	public void run(IAction action) {
		// RMF To make this generic, really need to pick up the file extension from somewhere... perhaps post a dialog?
		new SmapiProjectNature("x10").addToProject(fProject);
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
