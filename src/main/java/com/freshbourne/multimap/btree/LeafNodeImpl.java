/**
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 * 
 * (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
 */
package com.freshbourne.multimap.btree;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.freshbourne.io.DynamicDataPage;
import com.freshbourne.io.FixLengthSerializer;
import com.freshbourne.io.HashPage;
import com.freshbourne.io.NoSpaceException;
import com.freshbourne.io.PagePointer;
import com.freshbourne.io.ResourceManager;
import com.freshbourne.io.Serializer;
import com.google.inject.Provider;

/**
 * This B-Tree-Leaf stores entries by storing the keys and values in seperate pages
 * and keeping track only of the pageId and offset.
 * 
 * @author "Robin Wenglewski <robin@wenglewski.de>"
 *
 * @param <K> KeyType
 * @param <V> ValueType
 */
public class LeafNodeImpl<K extends Comparable<? super K>,V> implements LeafNode<K,V> {
	
	private final HashPage page;
	private final FixLengthSerializer<PagePointer, byte[]> pointerSerializer;
	
	// right now, we always store key/value pairs. If the entries are not unique,
	// it could make sense to store the key once with references to all values
	//TODO: investigate if we should do this
	private final int serializedPointerSize;
	private final int maxEntries;
	
	// counters
	private int numberOfEntries = 0;
	
	private int lastKeyPageId = -1;
	private int lastKeyPageRemainingBytes = -1;
	
	private int lastValuePageId = -1;
	private int lastValuePageRemainingBytes = -1;	
	
	//TODO: ensure that the pointerSerializer always creates the same (buffer-)size!
	LeafNodeImpl(
			HashPage page, // the LeafNodes uses this Body for storing links to the pageids & offsets
			Provider<DynamicDataPage<K>> keyProvider,
			Provider<DynamicDataPage<V>> valueProvider,
			FixLengthSerializer<PagePointer, byte[]> pointSerializer){
		
		this.page = page;
		this.pointerSerializer = pointSerializer;
		
		this.serializedPointerSize = pointerSerializer.serializedLength(PagePointer.class);
		maxEntries = page.body().capacity() / serializedPointerSize;
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#add(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void add(K key, V value) throws Exception {
		if(numberOfEntries == maxEntries)
			throw new Exception();
		
//		byte[] keyBytes //= //keySerializer.serialize(key);
//		byte[] valueBytes = valueSerializer.serialize(value);
//		
//		// make sure the generated bytes fit in a page
//		if(keyBytes.length > page.body().capacity())
//			throw new SerializationToLargeException(key);
//		if(valueBytes.length > page.body().capacity())
//			throw new SerializationToLargeException(value);
//		
//		
//		storeKeyAndValueBytes(keyBytes, valueBytes);
	}
	
	/**
	 * @param keyBytes
	 * @param valueBytes
	 * @throws IOException 
	 */
	private void storeKeyAndValueBytes(byte[] keyBytes,
			byte[] valueBytes) throws IOException {
//		RawPage keyPage;
//		RawPage valuePage;
//		
//		if(keyBytes.length > lastKeyPageRemainingBytes){
//			keyPage = resourceManager.newPage();
//		}  else {
//			keyPage = resourceManager.readPage(lastKeyPageId);
//		}
//		
//		if(valueBytes.length > lastValuePageRemainingBytes){
//			try{
//				valuePage = resourceManager.newPage();
//			} finally {
//				//TODO: unfortunately, we dont have this yet.
//				//if(newKeyPage != null)
//					// resourceManager.removePage()
//			}
//		} else {
//			valuePage = resourceManager.readPage(lastValuePageId);
//		}
//		
//		DataPage keyDataPage = new DynamicDataPage(keyPage.body(), pointerSerializer);
//		DataPage valueDataPage = new DynamicDataPage(valuePage.body(), pointerSerializer);
//		
//		// int keyPos = keyDataPage.add(keyBytes);
//		
//		// pagepointer, we use it different: offset is number of the value
//		
		
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.btree.Node#size()
	 */
	@Override
	public int size() {
		return numberOfEntries;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(K key) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#getFirst(java.lang.Object)
	 */
	@Override
	public V getFirst(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#get(java.lang.Object)
	 */
	@Override
	public V[] get(K key) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object)
	 */
	@Override
	public V[] remove(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V remove(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#clear()
	 */
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return the maximal number of Entries
	 */
	public int getMaxEntries() {
		return maxEntries;
	}

	private void storeKey(ByteBuffer b){
		
	}
	
	private void storeValue(ByteBuffer b){
		
	}
}
