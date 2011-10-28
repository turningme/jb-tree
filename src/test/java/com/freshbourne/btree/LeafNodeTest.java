/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 *
 * http://creativecommons.org/licenses/by-nc/3.0/
 *
 * For alternative conditions contact the author.
 *
 * Copyright (c) 2011 "Robin Wenglewski <robin@wenglewski.de>"
 */
package com.freshbourne.btree;


import com.freshbourne.comparator.IntegerComparator;
import com.freshbourne.io.AutoSaveResourceManager;
import com.freshbourne.io.DataPageManager;
import com.freshbourne.io.ResourceManagerBuilder;
import com.freshbourne.serializer.FixedStringSerializer;
import com.freshbourne.serializer.IntegerSerializer;
import com.freshbourne.serializer.PagePointSerializer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.*;

public class LeafNodeTest {

	private final static File file = new File("/tmp/LeafNodeTest");
	private LeafNode<Integer, String>         leafStr;
	private LeafNode<Integer, Integer>        leafInt;
	private LeafPageManager<Integer, String>  lpmStr;
	private LeafPageManager<Integer, Integer> lpmInt;

	private int key1 = 1;
	private int key2 = 2;

	private String value1 = "val1";
	private String value2 = "value2";
	private AutoSaveResourceManager rm;

	LeafNodeTest(){
		this.rm = new ResourceManagerBuilder().file(file).buildAutoSave();
	}

	@BeforeMethod
	public void setUp() throws IOException {
		lpmStr = BTree.create(rm, IntegerSerializer.INSTANCE, FixedStringSerializer.INSTANCE, IntegerComparator.INSTANCE).getLeafPageManager();
		leafStr = lpmStr.createPage();

		lpmInt = BTree.create(rm, IntegerSerializer.INSTANCE, IntegerSerializer.INSTANCE, IntegerComparator.INSTANCE).getLeafPageManager();
		leafInt = lpmInt.createPage();
	}

	@Test public void shouldBeAbleToInsertAndGet() {
		leafStr.insert(key1, value1);
		assertTrue(leafStr.containsKey(key1));
		assertEquals(1, leafStr.getNumberOfEntries());
		assertEquals(1, leafStr.get(key1).size());
		assertEquals(value1, leafStr.get(key1).get(0));
	}

	@Test public void shouldBeAbleToGetLastKeyAndPointer() {
		leafStr.insert(key1, value1);
		assertNotNull(leafStr.getLastLeafKey());
		assertNotNull(leafStr.getLastLeafKeySerialized());

		leafStr.insert(key2, value2);
		assertNotNull(leafStr.getLastLeafKey());
		assertNotNull(leafStr.getLastLeafKeySerialized());
	}

	@Test public void shouldAlwaysWorkAfterReload() {
		for (int i = 0; i < 5; i++) {
			leafStr.insert(key1, value1);
		}
		leafStr.insert(key2, value2);
		assertEquals(6, leafStr.getNumberOfEntries());
		leafStr.load();
		assertEquals(6, leafStr.getNumberOfEntries());
		assertEquals(1, leafStr.get(key2).size());

	}

	@Test public void shouldAtSomePointReturnAValidAdjustmentAction() {
		AdjustmentAction<Integer, String> action;
		do {
			action = leafStr.insert(key1, value1);
		} while (action == null);

		DataPageManager<Integer> keyPageManager = new DataPageManager<Integer>(rm, PagePointSerializer.INSTANCE, IntegerSerializer.INSTANCE);

		assertNotNull(leafStr.getLastLeafKey());
		assertEquals(AdjustmentAction.ACTION.INSERT_NEW_NODE, action.getAction());

		assertNotNull(action.getSerializedKey());


		// this should still work and not throw an exception
		stateTest(leafStr);
		LeafNode<Integer, String> newLeaf = lpmStr.getPage(action.getPageId());
		;
		stateTest(newLeaf);
	}

	private void stateTest(LeafNode<Integer, String> leaf) {
		Integer k = leaf.getLastLeafKey();
		assertNotNull(leaf.get(k));
		assertTrue(leaf.containsKey(k));

		// all keys should be accessible
		for (int i = 0; i < leaf.getNumberOfEntries(); i++) {
			Integer key = leaf.getKeyAtPosition(i);
			assertNotNull(k);
			assertTrue(leaf.containsKey(key));

		}
		assertEquals(k, leaf.getKeyAtPosition(leaf.getNumberOfEntries() - 1));

	}

	@Test
	public void iterators() {
		fillLeaf(leafInt, 10);

		Iterator<Integer> iterator = leafInt.getIterator(-5, 5);
		for (int i = 0; i <= 5; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());

		iterator = leafInt.getIterator(5, 15);
		for (int i = 5; i < 10; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());

		iterator = leafInt.getIterator(0, 9);
		for (int i = 0; i < 10; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());


		iterator = leafInt.getIterator(5, null);
		for (int i = 5; i < 10; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());

		iterator = leafInt.getIterator(null, 5);
		for (int i = 0; i <= 5; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());

		iterator = leafInt.getIterator(null, null);
		for (int i = 0; i < 10; i++)
			assertEquals(i, (int) iterator.next());
		assertFalse(iterator.hasNext());
	}


	private void fillLeaf(LeafNode<Integer, Integer> leaf, int count) {
		for (int i = 0; i < count; i++) {
			leaf.insert(i, i);
		}
	}

	@Test
	public void shouldContainAddedEntries() {
		leafStr.insert(key1, value1);
		assertTrue(leafStr.containsKey(key1));
		assertEquals(1, leafStr.get(key1).size());
		assertEquals(value1, leafStr.get(key1).get(0));
		assertEquals(1, leafStr.getNumberOfEntries());

		leafStr.insert(key1, value2);
		assertTrue(leafStr.containsKey(key1));
		assertEquals(2, leafStr.get(key1).size());
		assertTrue(leafStr.get(key1).contains(value1));
		assertTrue(leafStr.get(key1).contains(value2));
		assertEquals(2, leafStr.getNumberOfEntries());

		leafStr.insert(key2, value2);
		assertTrue(leafStr.containsKey(key2));
		assertEquals(1, leafStr.get(key2).size());
		assertTrue(leafStr.get(key1).contains(value2));
		assertTrue(leafStr.get(key1).contains(value1));
		assertTrue(leafStr.get(key1).size() == 2);
		assertEquals(3, leafStr.getNumberOfEntries());
	}

	@Test
	public void removeWithValueArgumentShouldRemoveOnlyThisValue() {
		leafStr.insert(key1, value1);
		leafStr.insert(key1, value2);
		leafStr.insert(key2, value2);

		assertEquals(3, leafStr.getNumberOfEntries());
		leafStr.remove(key1, value2);
		assertEquals(1, leafStr.get(key1).size());
		assertEquals(value1, leafStr.get(key1).get(0));
		assertEquals(value2, leafStr.get(key2).get(0));
	}


	@Test
	public void prependEntriesShouldWork() {
		LeafNode<Integer, String> leaf2 = lpmStr.createPage();

		int totalInserted = 0;

		// fill leafStr
		for (int i = 0; i < leafStr.getMaximalNumberOfEntries(); i++) {
			assertThat(leafStr.insert(i, "val")).isNull();
			totalInserted++;
		}

		// in testPrepend, one entry is inserted
		testPrepend(leafStr, leaf2);
		totalInserted++;

		assertThat(leafStr.getNumberOfEntries() + leaf2.getNumberOfEntries()).isEqualTo(totalInserted);

		// should work again, when we have to actually move some entries in leaf2
		for (int i = leafStr.getNumberOfEntries(); i < leafStr
				.getMaximalNumberOfEntries(); i++) {
			assertThat(leafStr.insert(-1 * i, "val")).isNull();
			totalInserted++;
		}

		testPrepend(leafStr, leaf2);
		totalInserted++;
		assertThat(leafStr.getNumberOfEntries() + leaf2.getNumberOfEntries()).isEqualTo(totalInserted);

	}

	/** in testPrepend, one entry is inserted* */
	private void testPrepend(LeafNode<Integer, String> leaf1, LeafNode<Integer, String> leaf2) {
		leaf1.setNextLeafId(leaf2.getId());

		// insert key so that move should happen
		AdjustmentAction<Integer, String> action = leaf1.insert(1, "value");

		// an update key action should be passed up
		assertNotNull(action);

		// make sure leafStr structures are in tact
		assertEquals(leaf1.getLastLeafKey(), leaf1.getKeyAtPosition(leaf1.getNumberOfEntries() - 1));

		for (int key : leaf1.getKeySet()) {
			assertNotNull(leaf1.get(key));
		}

		for (int key : leaf2.getKeySet()) {
			assertNotNull(leaf2.get(key));
		}
	}


}
