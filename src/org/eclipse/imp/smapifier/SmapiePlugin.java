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

package org.eclipse.imp.smapifier;

import org.eclipse.imp.runtime.PluginBase;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class SmapiePlugin extends PluginBase  {
	public static final String kPluginID= "org.eclipse.imp.smapifier";

	//The shared instance.
	private static SmapiePlugin plugin;
	
	/**
	 * The constructor.
	 */
	public SmapiePlugin() {
		plugin = this;
	}

    public String getID() {
        return kPluginID;
    }

    @Override
    public String getLanguageID() {
        throw new UnsupportedOperationException("Someone called SmapiePlugin.getLanguageID()");
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
	public static SmapiePlugin getInstance() {
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
}
