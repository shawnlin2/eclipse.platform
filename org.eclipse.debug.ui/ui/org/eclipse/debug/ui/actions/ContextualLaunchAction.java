/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * An action delegate that builds a context menu with applicable launch shortcuts
 * for a specific launch mode.
 * <p>
 * This class can be subclassed and contributed as an object contribution pop-up
 * menu extension action. When invoked, it becomes a sub-menu that dynamically
 * builds a list of applicable launch shortcuts for the current selection.
 * Each launch shortcut may have optional information to support a context menu action. The extra
 * </p>
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.0
 */
public abstract class ContextualLaunchAction implements IObjectActionDelegate, IMenuCreator {

	private IStructuredSelection fSelection;
	private IAction fDelegateAction;
	private String fMode;
	// default launch group for this mode (null category)
	private ILaunchGroup fGroup = null;
	// map of launch groups by (non-null) categories, for this mode
	private Map fGroupsByCategory = null;
	// whether to re-fill the menu (reset on selection change)
	private boolean fFillMenu = true;
	
	/**
	 * Constructs a contextual launch action for the given launch mode.
	 * 
	 * @param mode launch mode
	 */
	public ContextualLaunchAction(String mode) {
		fMode = mode;
		ILaunchGroup[] groups = DebugUITools.getLaunchGroups();
		fGroupsByCategory = new HashMap(3);
		for (int i = 0; i < groups.length; i++) {
			ILaunchGroup group = groups[i];
			if (group.getMode().equals(mode)) {
				if (group.getCategory() == null) {
					fGroup = group;
				} else {
					fGroupsByCategory.put(group.getCategory(), group);
				}
			}
		}
	}
	
	/*
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// We don't have a need for the active part.
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#dispose()
	 */
	public void dispose() {
		// nothing to do
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent) {
		// never called
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent) {
		//Create the new menu. The menu will get filled when it is about to be shown. see fillMenu(Menu).
		Menu menu = new Menu(parent);
		/**
		 * Add listener to repopulate the menu each time
		 * it is shown because MenuManager.update(boolean, boolean) 
		 * doesn't dispose pulldown ActionContribution items for each popup menu.
		 */
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				if (fFillMenu) {
					Menu m = (Menu)e.widget;
					MenuItem[] items = m.getItems();
					for (int i=0; i < items.length; i++) {
						items[i].dispose();
					}
					fillMenu(m);
					fFillMenu = false;
				}
			}
		});
		return menu;
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// Never called because we become a menu.
	}
	
	/*
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// if the selection is an IResource, save it and enable our action
		if (selection instanceof IStructuredSelection) {
			fFillMenu = true;
			if (action != null) {
				if (fDelegateAction != action) {
					fDelegateAction = action;
					fDelegateAction.setMenuCreator(this);
				}
				// save selection and enable our menu
				fSelection = (IStructuredSelection) selection;
				action.setEnabled(true);
				return;
			}
		}
		action.setEnabled(false);
	}

	/**
	 * Fill pull down menu with the pages of the JTabbedPane
	 */
	private void fillMenu(Menu menu) {
		// lookup appropriate launch config types and build launch actions for them.
		// Retrieve the current perspective and the registered shortcuts
		String activePerspID = getActivePerspectiveID();
		if (activePerspID == null || fSelection == null) {
			return;
		}
		
		IEvaluationContext context = createContext();
		// gather all shortcuts and run their filters so that we only run the
		// filters one time for each shortcut. Running filters can be expensive.
		// Also, only *LOADED* plugins get their filters run.
		List /* <LaunchShortcutExtension> */ allShortCuts = getLaunchConfigurationManager().getLaunchShortcuts();
		Iterator iter = allShortCuts.iterator();
		List filteredShortCuts = new ArrayList(10);
		while (iter.hasNext()) {
			LaunchShortcutExtension ext = (LaunchShortcutExtension) iter.next();
			try {
				if (isApplicable(ext, context)) {
					filteredShortCuts.add(ext);
				}
			} catch (CoreException e) {
				// not supported
			}
		}
		iter = filteredShortCuts.iterator();
		int accelerator = 1;
		String category = null;
		while (iter.hasNext()) {
			LaunchShortcutExtension ext = (LaunchShortcutExtension) iter.next();
			Set modes = ext.getModes(); // supported launch modes
			Iterator modeIter = modes.iterator();
			while (modeIter.hasNext()) {
				String mode = (String) modeIter.next();
				if (mode.equals(fMode)) {
					category = ext.getCategory();
					populateMenuItem(mode, ext, menu, accelerator++);
				}
			}
		}
		if (accelerator > 1) {
			new MenuItem(menu, SWT.SEPARATOR);
		}
		ILaunchGroup group = fGroup;
		if (category != null) {
			group = (ILaunchGroup) fGroupsByCategory.get(category);
		}
		IAction action = new OpenLaunchDialogAction(group.getIdentifier());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	/**
	 * @return an Evaluation context with default variable = selection
	 */
	private IEvaluationContext createContext() {
		// create a default evaluation context with default variable of the user selection
		List selection = getSelectedElements();
		IEvaluationContext context = new EvaluationContext(null, selection);
		context.addVariable("selection", selection); //$NON-NLS-1$
		
		return context;
	}
	
	/**
	 * @return current selection as a List.
	 */
	private List getSelectedElements() {
		ArrayList result = new ArrayList();
		Iterator iter = fSelection.iterator();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		return result;
	}
	
	/**
	 * Evaluate the enablement logic in the contextualLaunch
	 * element description. A true result means that we should
	 * include this shortcut in the context menu.
	 * @return true iff shortcut should appear in context menu
	 */
	private boolean isApplicable(LaunchShortcutExtension ext, IEvaluationContext context) throws CoreException {
		Expression expr = ext.getContextualLaunchEnablementExpression();
		return ext.evalEnablementExpression(context, expr);
	}

	/**
	 * Add the shortcut to the context menu's launch submenu.
	 */
	private void populateMenuItem(String mode, LaunchShortcutExtension ext, Menu menu, int accelerator) {
		LaunchShortcutAction action = new LaunchShortcutAction(mode, ext);
		action.setActionDefinitionId(ext.getId());
		String helpContextId = ext.getHelpContextId();
		if (helpContextId != null) {
			WorkbenchHelp.setHelp(action, helpContextId);
		}
		StringBuffer label= new StringBuffer();
		if (accelerator >= 0 && accelerator < 10) {
			//add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
		}
		String contextLabel= ext.getContextLabel(mode);
		// replace default action label with context label if specified.
		label.append((contextLabel != null) ? contextLabel : action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	/**
	 * Return the ID of the currently active perspective.
	 * 
	 * @return the active perspective ID or <code>null</code> if there is none.
	 */
	private String getActivePerspectiveID() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IPerspectiveDescriptor persp = page.getPerspective();
				if (persp != null) {
					return persp.getId();
				}
			}
		}
		return null;
	}
	/**
	 * Returns the launch configuration manager.
	*
	* @return launch configuration manager
	*/
	private LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}

}
