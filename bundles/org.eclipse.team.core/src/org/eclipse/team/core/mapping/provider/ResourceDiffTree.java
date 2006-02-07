/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.mapping.provider;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.diff.provider.DiffTree;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;

/**
 * Implementation of {@link IResourceDiffTree}.
 * <p>
 * This class is not intended to be subclassed by clients
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public class ResourceDiffTree extends DiffTree implements IResourceDiffTree {

	/**
	 * Get the resource for the diff node that was obtained from an
	 * {@link IResourceDiffTree}.
	 * @param node the diff node.
	 * @return the resource for the diff node
	 */
	public static IResource getResourceFor(IDiff node) {
		if (node instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) node;
			return rd.getResource();
		}
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			IDiff child = twd.getLocalChange();
			if (child != null)
				return getResourceFor(child);
			child = twd.getRemoteChange();
			if (child != null)
				return getResourceFor(child);
		}
		Assert.isLegal(false);
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IResourceDiffTree#getDiff(org.eclipse.core.resources.IResource)
	 */
	public IDiff getDiff(IResource resource) {
		return getDiff(resource.getFullPath());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IResourceDiffTree#getResource(org.eclipse.team.core.diff.IDiffNode)
	 */
	public IResource getResource(IDiff diff) {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IResourceDiff localChange = ((IResourceDiff)twd.getLocalChange());
			if (localChange != null)
				return localChange.getResource();
			return ((IResourceDiff)twd.getRemoteChange()).getResource();
		} else {
			return ((IResourceDiff)diff).getResource();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IResourceDiffTree#accept(org.eclipse.team.core.diff.IDiffVisitor, org.eclipse.core.resources.mapping.ResourceTraversal[])
	 */
	public void accept(ResourceTraversal[] traversals, IDiffVisitor visitor) {
		IDiff[] diffs = getDiffs(traversals);
		for (int i = 0; i < diffs.length; i++) {
			IDiff node = diffs[i];
			try {
				visitor.visit(node);
			} catch (CoreException e) {
				// Ignore: to be removed
			}
		}
	}

	public IDiff[] getDiffs(final ResourceTraversal[] traversals) {
		final Set result = new HashSet();
		accept(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new IDiffVisitor() {
			public boolean visit(IDiff delta) {
				for (int i = 0; i < traversals.length; i++) {
					ResourceTraversal traversal = traversals[i];
					if (traversal.contains(getResource(delta))) {
						result.add(delta);
					}
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE);
		return (IDiff[]) result.toArray(new IDiff[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IResourceDiffTree#getDiffs(org.eclipse.core.resources.IResource, int)
	 */
	public IDiff[] getDiffs(IResource resource, int depth) {
		return getDiffs(new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, depth, IResource.NONE) } );
	}

	private IResource internalGetResource(IPath fullPath, boolean container) {
		if (container)
			return ResourcesPlugin.getWorkspace().getRoot().getFolder(fullPath);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fullPath);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IResourceDiffTree#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) {
		List result = new ArrayList();
		IPath[] paths = getChildren(resource.getFullPath());
		for (int i = 0; i < paths.length; i++) {
			IPath path = paths[i];
			IDiff node = getDiff(path);
			if (node == null) {
				result.add(internalGetResource(path, true));
			} else {
				result.add(getResource(node));
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IResourceDiffTree#getAffectedResources()
	 */
	public IResource[] getAffectedResources() {
		List result = new ArrayList();
		IDiff[] nodes = getDiffs();
		for (int i = 0; i < nodes.length; i++) {
			IDiff node = nodes[i];
			result.add(getResource(node));
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.provider.DiffTree#add(org.eclipse.team.core.diff.IDiffNode)
	 */
	public void add(IDiff delta) {
		Assert.isTrue(delta instanceof IResourceDiff || delta instanceof IThreeWayDiff);
		super.add(delta);
	}
	
	/**
	 * Remove the diff associated with the given resource from
	 * the tree.
	 * @param resource the resource
	 */
	public void remove(IResource resource) {
		remove(resource.getFullPath());
	}

	public boolean hasMatchingDiffs(ResourceTraversal[] traversals, final FastDiffFilter filter) {
		final RuntimeException found = new RuntimeException();
		try {
			accept(traversals, new IDiffVisitor() {
				public boolean visit(IDiff delta) {
					if (filter.select(delta)) {
						throw found;
					}
					return false;
				}
			
			});
		} catch (RuntimeException e) {
			if (e == found)
				return true;
			throw e;
		}
		return false;
	}	
}
