package com.ibm.watson.smapifier.builder;

import org.eclipse.uide.core.ProjectNatureBase;
import org.eclipse.uide.runtime.IPluginLog;

import com.ibm.watson.smapifier.SmapiePlugin;

public class SmapiProjectNature extends ProjectNatureBase {
	public static final String k_natureID= SmapiePlugin.kPluginID + ".smapinature";

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

	protected String getDownstreamBuilderID() {
		return "org.eclipse.jdt.core.javabuilder";
	}

}
