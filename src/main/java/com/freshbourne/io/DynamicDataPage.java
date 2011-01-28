/*
 * Copyright (c) 2011 Robin Wenglewski <robin@wenglewski.de>
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 */
package com.freshbourne.io;

import com.freshbourne.serializer.FixLengthSerializer;
import com.freshbourne.serializer.Serializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Wraps around a <code>byte[]</code> and can hold values of variable serialized length.
 * 
 * Since its a dynamic data page, values are written first at the end of the body to allow
 * the header to grow.
 * 
 * The header is kept in memory.
 * 
 * @author Robin Wenglewski <robin@wenglewski.de>
 *
 */
public class DynamicDataPage<T> implements DataPage<T>, ComplexPage{
	
	private static final int intSize = 4;
	
	
	/*
	 * build up like this:
	 * NO_ENTRIES_INT | PAGE_POINTER_OFFSETS | DATA
	 * 
	 * the pagepointer in this page contain 
	 * 	id: id of the data element inserted
	 *  offset: in the current page where the data ist stored
	 */
	private final ByteBuffer header;
	
	private final RawPage rawPage;
	
	private final FixLengthSerializer<PagePointer, byte[]> pointSerializer;
	private final Serializer<T, byte[]> entrySerializer;
	
	private int bodyOffset = -1;
	
	
	/**
	 * ByteBuffer.getInt returns 0 if no int could be read. To avoid thinking we already initialized the buffer,
	 * we write down this number instead of 0 if we have no entries.
	 */
	private final int NO_ENTRIES_INT = 345234345;
	
	// id | offset in this page
	private final Map<Integer, Integer> entries;
	
	private boolean valid = false;


	DynamicDataPage(
			RawPage rawPage,
			FixLengthSerializer<PagePointer, byte[]> pointSerializer, 
			Serializer<T, byte[]> dataSerializer){

		this.rawPage = rawPage;
		
        this.header = rawPage.bufferForWriting(0).duplicate();
        this.bodyOffset = rawPage.bufferForWriting(0).limit();
		this.pointSerializer = pointSerializer;
		this.entrySerializer = dataSerializer;
		
		this.entries = new TreeMap<Integer, Integer>();
	}
	
	@Override
	public void initialize() {
		writeAndAdjustHeader();
		this.valid = true;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.btree.DataPage#add(byte[])
	 */
	@Override
	public Integer add(T entry) {
		ensureValid();
		
		byte[] bytes = entrySerializer.serialize(entry);
		
		if(bytes.length > remaining())
			return null;
		
		bodyOffset -= bytes.length;
		rawPage.bufferForWriting(bodyOffset).put(bytes);
		
		int id = generateId();
		entries.put(id, bodyOffset);
		
		writeAndAdjustHeader();
		
		return id;
	}
	
	private int generateId(){
		Random r = new Random();
		int id;
		while(entries.containsKey(id = r.nextInt())){}
		return id;
	}
	
	/**
	 * appends a PagePointer to the header and increases the header size, if possible
	 * @param p
	 */
	private void addToHeader(PagePointer p){
		int size = pointSerializer.serializedLength(PagePointer.class);
		header.position(header.limit()-size);
		header.put(pointSerializer.serialize(p));
		
		if(rawPage.bufferForWriting(0).capacity() - header.position() - bodyUsedBytes() > size)
			header.limit(header.position() + size);
	}
	
	
	private int bodyUsedBytes(){
		return rawPage.bufferForWriting(0).limit() - bodyOffset;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.btree.DataPage#remove(int)
	 */
	@Override
	public void remove(int id)  {
		
		Integer offset = entries.remove(id);
		if(offset == null)
			return;
		
		// move all body elements
		int size = sizeOfEntryAt(offset);
		System.arraycopy(rawPage.bufferForWriting(0).array(), bodyOffset, rawPage.bufferForWriting(0).array(), bodyOffset + size, offset - bodyOffset );
		
		// adjust the entries in the entries array
		for(int key : entries.keySet()){
			if(entries.get(key) < offset)
				entries.put(key, entries.get(key) + size);
		}
		
		
		bodyOffset += size;
		
		// write the adjustments to byte array
		writeAndAdjustHeader();
	}

	/**
	 * Creates a valid header by writing the entries in memory to the header and adjusts the header limit.
	 */
	private void writeAndAdjustHeader() {
		adjustHeaderLimit();
		
		header.position(0);
		header.putInt(entries.size() == 0 ? NO_ENTRIES_INT : entries.size());
		
		for(int key : entries.keySet()){
			header.putInt(key);
			header.putInt(entries.get(key));
		}	
	}
	
	private void adjustHeaderLimit(){
		header.limit( (Integer.SIZE + (Integer.SIZE) * 2 * entries.size()) / 8 );
		if(remaining() > Integer.SIZE / 8)
			header.limit(header.limit() + Integer.SIZE / 8);
		
		rawPage().setModified(true);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.btree.DataPage#get(int)
	 */
	@Override
	public T get(int id) {
		ensureValid();
		
		if(!entries.containsKey(id))
			return null;
		
		Integer offset = entries.get(id);
		
		int size = sizeOfEntryAt(offset);
		byte[] bytes = new byte[size];
		rawPage.bufferForReading(offset).get(bytes);
		
		return entrySerializer.deserialize(bytes);
	}
	
	private void ensureValid() throws InvalidPageException {
		if( !isValid() )
			throw new InvalidPageException(this);
	}
	
	private int nextEntry(int offset){
		int smallestLarger = -1;
		
		for(int o : entries.values()){
			if(o > offset){
				if(o < smallestLarger || smallestLarger == -1)
					smallestLarger = o;
			}
		}
		
		return smallestLarger;
	}
	
	private int sizeOfEntryAt(int offset){
		int smallestLarger = nextEntry(offset);
		
		if(smallestLarger == -1)
			smallestLarger = rawPage.bufferForReading(0).capacity();
		
		return smallestLarger - offset;
	}

		/* (non-Javadoc)
	 * @see com.freshbourne.multimap.btree.DataPage#remaining()
	 */
	@Override
	public int remaining() {
		return rawPage.bufferForReading(0).limit() - header.limit() - bodyUsedBytes();
	}
	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#load()
	 */
	@Override
	public void load() {
		entries.clear();
		
		header.position(0);
		int numberOfEntries = header.getInt();
		
		for(int i = 0; i < numberOfEntries; i++){
			int key = header.getInt();
			entries.put(key, header.getInt());
		}
		
		bodyOffset = rawPage.bufferForReading(0).limit();
		for(int i : entries.values()){
			if(i < bodyOffset)
				bodyOffset = i;
		}
		
		adjustHeaderLimit();
		
		valid = true;
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#isValid()
	 */
	@Override
	public boolean isValid() {
		return valid;
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.io.DataPage#numberOfEntries()
	 */
	@Override
	public int numberOfEntries() throws InvalidPageException {
		ensureValid();
		
		return entries.size();
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#rawPage()
	 */
	@Override
	public RawPage rawPage() {
		return rawPage;
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.io.DataPage#pagePointSerializer()
	 */
	@Override
	public FixLengthSerializer<PagePointer, byte[]> pagePointSerializer() {
		return pointSerializer;
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.io.DataPage#dataSerializer()
	 */
	@Override
	public Serializer<T, byte[]> dataSerializer() {
		return entrySerializer;
	}
	
}
