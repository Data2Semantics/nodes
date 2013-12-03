package org.nodes.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Comparator;

import org.junit.Test;

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
	}

}
