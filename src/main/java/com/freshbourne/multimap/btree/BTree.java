/*
 * Copyright (c) 2011 Robin Wenglewski <robin@wenglewski.de>
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 */

/**
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 * 
 * (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
 */
package com.freshbourne.multimap.btree;

import com.freshbourne.multimap.MultiMap;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.List;

public class BTree<K, V> implements MultiMap<K, V> {

	private final LeafPageManager<K,V> leafPageManager;
	
	private LeafPage<K, V> root;
	
	
	@Inject
	BTree(LeafPageManager<K,V> leafPageManager) throws IOException {
		this.leafPageManager = leafPageManager;
		root = leafPageManager.createPage();
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#size()
	 */
	@Override
	public int getNumberOfEntries() {
		return root.getNumberOfEntries();
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(K key) throws Exception {
		return root.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#get(java.lang.Object)
	 */
	@Override
	public List<V> get(K key) throws Exception {
		return root.get(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#add(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean add(K key, V value) {
		if (root instanceof LeafPage) {
			if (root.add(key, value)) {
				return true;
			} else if (root instanceof LeafPage && root.getNextLeafId() != null) {
				throw new UnsupportedOperationException(
						"push some entries to next leaf");
			} else {
				// allocate new leaf
				LeafPage<K,V> newLeaf = leafPageManager.createPage();
				newLeaf.setNextLeafId(root.rawPage().id());
				root.setNextLeafId(newLeaf.rawPage().id());
				
				// newLeaf.setLastKeyContinuesOnNextPage(root.isLastKeyContinuingOnNextPage());
				
				// move half of the keys to new page
				newLeaf.prependEntriesFromOtherPage(root, root.getNumberOfEntries() >> 1);
				// see on which page we will insert the value
			}
		} else {
			throw new UnsupportedOperationException(
					"innernodes not supported yet");
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object)
	 */
	@Override
	public void remove(K key) throws Exception {
		root.remove(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void remove(K key, V value) throws Exception {
		root.remove(key, value);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#clear()
	 */
	@Override
	public void clear() throws Exception {
		root.clear();
	}

}
