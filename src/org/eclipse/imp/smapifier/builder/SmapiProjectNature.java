package com.ibm.watson.smapifier.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.imp.core.ProjectNatureBase;
import org.eclipse.imp.runtime.IPluginLog;

import com.ibm.watson.smapifier.SmapiePlugin;

public class SmapiProjectNature extends ProjectNatureBase {
	public static final String k_natureID= SmapiePlugin.kPluginID + ".smapinature";

	private Set/*<String>*/ fExtensions= new HashSet();

	public SmapiProjectNature() { }

	public SmapiProjectNature(String exten) {
	    fExtensions.add(exten);
	}

	public SmapiProjectNature(List/*<String>*/ extens) {
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
		return SmapiePlugin.getDefault();
	}

	protected void refreshPrefs() {
	}

	protected String getUpstreamBuilderID() {
		return "org.eclipse.jdt.core.javabuilder";
	}

	protected Map getBuilderArguments() {
	    Map m= new HashMap();

	    if (fExtensions.size() > 0)
		m.put("exten", fExtensions.iterator().next());

	    return m;
	}
}
