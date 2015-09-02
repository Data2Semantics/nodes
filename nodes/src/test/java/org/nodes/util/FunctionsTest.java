package org.nodes.util;

import static org.junit.Assert.*;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Min;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.Functions.logMin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FunctionsTest
{

	@Test
	public void testLog2Sum()
	{
		testLog2Sum(2.0, 1.0, 1.0);
		testLog2Sum(1000.0, 1.0, 1000.0);
		
		assertEquals(log2(24), log2Min(5, 3), 0.0);
		
		double a = 5, b = 3, c = 1;
		assertEquals(log2(26.0), log2Min(log2Sum(a, c), b), 0.0);
	}
	
	public void testLog2Sum(double expected, double... values)
	{
		assertEquals(expected, log2Sum(values), 0.000000000000000000001);
		
		List<Double> v = new ArrayList<Double>(values.length);
		for(double val : values)
			v.add(val);
		assertEquals(expected, log2Sum(v), 0.000000000000000000001);
	}
	
	@Test
	public void testMulti()
	{
		for(int i : Series.series(100))
		{
			List<Double> w = Functions.randomMultinomial(5); 
			double sum = 0.0;
			for(double v : w)
				sum += v;
			assertEquals(1.0, sum, 0.0);
		}
		
	}

}
