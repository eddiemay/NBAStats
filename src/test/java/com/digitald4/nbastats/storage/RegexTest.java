package com.digitald4.nbastats.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RegexTest {
	@Test
	public void testJSONLite() {
		String item1 = "123,name1,$20,6.2,9.8,12.131415";
		String item2 = "456,name2,$19.65,5.8,3.4,3.14159";
		String in = String.format("%s|%s", item1, item2);
		String[] items = in.split("\\|");
		assertEquals(2, items.length);
		assertEquals(item1, items[0]);
		assertEquals(item2, items[1]);
	}
}
