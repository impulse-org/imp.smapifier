package com.ibm.watson.smapifier;

import org.eclipse.ui.plugin.*;
import org.eclipse.uide.runtime.IPluginLog;
import org.eclipse.uide.runtime.UIDEPluginBase;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class SmapiePlugin extends UIDEPluginBase  {
	public static final String kPluginID= "com.ibm.watson.smapifier";

	//The shared instance.
	private static SmapiePlugin plugin;
	
	/**
	 * The constructor.
	 */
	public SmapiePlugin() {
		plugin = this;
	}

	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static SmapiePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.ibm.smapie", path);
	}


	public String getID() {
		return kPluginID;
	}


	

	
}
