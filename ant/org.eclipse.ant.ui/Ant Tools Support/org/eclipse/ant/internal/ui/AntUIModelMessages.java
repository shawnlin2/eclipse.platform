/**********************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.ui;

import org.eclipse.osgi.util.NLS;

public class AntUIModelMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.AntUIModelMessages";//$NON-NLS-1$
	
	public static String ImageDescriptorRegistry_Allocating_image_for_wrong_display_1;

	public static String AntUtil_6;
	public static String AntUtil_0;
	public static String AntUtil_1;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntUIModelMessages.class);
	}
}