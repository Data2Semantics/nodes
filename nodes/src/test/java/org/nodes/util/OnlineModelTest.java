package org.nodes.util;

import static org.junit.Assert.*;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Series.series;

import java.util.Arrays;

import org.junit.Test;
import org.nodes.Global;

public class OnlineModelTest
{

	@Test
	public void test()
	{
		int n = 10000;
		
		OnlineModel<Boolean> om = new OnlineModel<Boolean>(Arrays.asList(true, false));
				
		double cl = 0;
		for(int i : series(n))
		{
			double p = om.observe(Global.random().nextBoolean());
			cl += - log2(p);
		}
		
		assertEquals((double)n, cl, 100.0);
	}
	
	@Test
	public void testTwo()
	{
		for(int rep : series(100))
		{
			double prob = Global.random().nextDouble();
			int n = 10000;
			
			OnlineModel<Integer> om = new OnlineModel<Integer>(Arrays.asList(0, 1));
					
			double cl = 0;
			for(int i : series(n))
			{
				double p = om.observe(Functions.choose(Arrays.asList(prob,  1.0 - prob), 1.0));
				cl += - Functions.log2(p);
			}
			
			double ent = - prob * log2(prob) - (1.0 - prob) * log2(1.0 - prob);
	 		
			assertEquals(n*ent, cl, 300.0);
		}
	}

}
