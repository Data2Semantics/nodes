package org.nodes.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.models.USequenceEstimator;

public class LogNumTest
{

	@Test
	public void test()
	{
		LogNum sum;
		// * base 2
		sum = LogNum.sum(asList(l(8.0), l(8.0), l(8.0), l(8.0)));
		assertEquals(5.0, sum.logMag(), 0.000000000001);
		assertTrue(sum.positive());
		
		sum = LogNum.sum(asList(l(3.0), l(-5.0)));
		assertEquals(1.0, sum.logMag(), 0.0000000001);
		assertFalse(sum.positive());
		
		sum = LogNum.sum(asList(l(2000, true), l(2001, false)));
		assertEquals(2000.0, sum.logMag(), 0.0000000000001);
		assertFalse(sum.positive());
		
		sum = LogNum.sum(asList(l(2000, true), l(2000, true), l(2001, false)));
		assertEquals(Double.NEGATIVE_INFINITY, sum.logMag(), 0.0);
		assertTrue(sum.positive());
	}
	
	@Test 
	public void skewnessTest()
	{
		List<LogNum> data = Arrays.asList(l(1.0), l(10.0), l(100.0));
		
		System.out.println(LogNum.skewness(data));
		System.out.println(LogNum.mean(data));
		
		data = Arrays.asList(l(1.0), l(10.0), l(88.0));
		
		System.out.println(LogNum.skewness(data));
		System.out.println(LogNum.mean(data));
	}

	public static LogNum l(double value)
	{
		return LogNum.fromDouble(value, 2.0);
	}
	
	public static LogNum l(double lMag, boolean pos)
	{
		return new LogNum(lMag, pos, 2.0);
	}
}
