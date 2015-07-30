package org.nodes.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nodes.Global;

public class MaxObserverTest
{

	@Test
	public void test()
	{
		MaxObserver<Integer> mo = new MaxObserver<Integer>(5);

		for(int i : Series.series(15))
			mo.observe(i);
		
		System.out.println(mo.elements());
		
		Comparator<Integer> rev = Collections.reverseOrder();
		mo = new MaxObserver<Integer>(5, rev);

		for(int i : Series.series(15))
			mo.observe(i);
		
		System.out.println(mo.elements());
		
		mo = new MaxObserver<Integer>(0);

		for(int i : Series.series(15))
			mo.observe(i);
		
		assertEquals(Collections.emptyList(), mo.elements());
	}
	
	@Test
	public void partitionTest()
	{
		List<Integer> ints = Arrays.asList(3, 4, 56, 7, 2, 10, 7, 97, 8, 3, 3, 54);
		
		Comparator<Integer> comp = Functions.natural();
		MaxObserver.partition(ints, 0, 12, 5, comp);
		
		System.out.println(ints);
		
		int pivotIndex = ints.indexOf(10);
		for(int i : Series.series(ints.size()))
			if(i < pivotIndex)
				assertTrue(ints.get(i) < 10);
			else
				assertTrue(ints.get(i) >= 10);
	}
	
	@Test
	public void partitionTestPartial()
	{
		List<Integer> ints = Arrays.asList(30, 40, 56, 7, 2, 10, 7, 1, 1, 1, 1, 1);
		
		Comparator<Integer> comp = Functions.natural();
		MaxObserver.partition(ints, 2, 9, 5, comp);
		
		System.out.println(ints);
		
		int pivotIndex = ints.indexOf(10);
		for(int i : Series.series(2, 9))
			if(i < pivotIndex)
				assertTrue(ints.get(i) < 10);
			else
				assertTrue(ints.get(i) >= 10);
	}
	
	@Test
	public void selectTest()
	{
		List<Integer> ints = Arrays.asList(0, 1, 1, 2, 1, 2, 3, 4, 5, 6, 7, 4, 5, 6, 2, 3, 8, 9);
		
		Comparator<Integer> comp = Collections.reverseOrder();
		MaxObserver.select(3, ints, 0, ints.size(), comp);
		
		
		assertEquals(
				new HashSet<Integer>(asList(7, 8, 9)),
				new HashSet<Integer>(ints.subList(0, 3))
				);
		System.out.println(ints);
	}
	
	@Test 
	public void compare()
	{
		int n = 1000000;
		int k = 10000;
		
		List<Integer> list = new ArrayList<Integer>();
		for(int index : Series.series(n))
			list.add(Global.random().nextInt(n));
		
		Comparator<Integer> comp = Collections.reverseOrder();
		
		Functions.tic();
		MaxObserver<Integer> obs = new MaxObserver<Integer>(k);
			obs.observe(list);
		System.out.println("Max observer finished in " + Functions.toc() + " seconds.");
		System.out.println("maxobserver result " + obs.elements(false));
		
		Functions.tic();
		List<Integer> elements = MaxObserver.quickSelect(k, list, comp, false);
		System.out.println("QuickSelect finished in " + Functions.toc() + " seconds.");
		System.out.println("quickselect result " + elements);
	}
}
