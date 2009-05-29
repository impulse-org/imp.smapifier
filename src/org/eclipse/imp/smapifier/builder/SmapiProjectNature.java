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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.imp.builder.ProjectNatureBase;
import org.eclipse.imp.runtime.IPluginLog;

import org.eclipse.imp.smapifier.SmapiePlugin;

public class SmapiProjectNature extends ProjectNatureBase {
	public static final String k_natureID= SmapiePlugin.kPluginID + ".smapinature";

	private Set<String> fExtensions= new HashSet<String>();

	public SmapiProjectNature() { }

	public SmapiProjectNature(String exten) {
	    fExtensions.add(exten);
	}

	public SmapiProjectNature(List<String> extens) {
	    fExtensions.addAll(extens);
	}

	public void addExtension(String extension) {
	    fExtensions.add(extension);
	}

	public String getNatureID() {
		return k_natureID;
	}

	public String getBuilderID() {
		return SmapieBuilder.BUILDER_ID;
	}

	public IPluginLog getLog() {
		return SmapiePlugin.getInstance();
	}

	protected void refreshPrefs() {
	}

	protected String getUpstreamBuilderID() {
		return "org.eclipse.jdt.core.javabuilder";
	}

	protected @SuppressWarnings("unchecked") Map getBuilderArguments() {
	    Map<String,String> m= new HashMap<String, String>();

	    if (fExtensions.size() > 0)
		m.put("exten", fExtensions.iterator().next());

	    return m;
	}
}
