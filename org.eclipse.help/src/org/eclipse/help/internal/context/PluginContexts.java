/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.context;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.help.*;
/**
 * Holds mapping of short contextId to IContext
 */
public class PluginContexts {
	private Map map = new HashMap();
	public void put(String shortId, IContext context) {
		map.put(shortId, context);
	}
	public IContext get(String shortId) {
		return (IContext) map.get(shortId);
	}
	
	/**
	 * For unit testing purposes.
	 */
	public Map getMap() {
		return map;
	}
}
