package org.nodes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.random.FractalGenerator;
import org.nodes.util.Series;

public class FractalGeneratorTest
{

	@Test
	public void testGraph()
	{
		FractalGenerator gen = new FractalGenerator(2, 1, 0.0);
		
		for(int i : Series.series(4))
		{
			gen.iterate();
			System.out.println(gen.graph().size() + " " + gen.graph().numLinks());
			System.out.println(gen.graph());
		}
		
	}

	@Test
	public void testPredictors()
	{
		int offspring = 4;
		int links = 2;
		int depth = 4;
		
		int n = FractalGenerator.size(offspring, links, depth);
		int l = FractalGenerator.numLinks(offspring, links, depth);
		
		FractalGenerator gen = new FractalGenerator(offspring, links, 1.0);
		
		for(int i : Series.series(depth))
			gen.iterate();

		assertEquals(gen.graph().size(), n);
		assertEquals(gen.graph().numLinks(), l);

	}
}
