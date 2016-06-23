package org.nodes.models;

import static java.lang.Math.pow;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.models.old.BarvinokUSimple;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Global;

public class BarvinokUSimpleTest
{
	// @Test
	public void testFoodweb()
	{
		System.out.println("seed: " + Global.randomSeed());
		
		List<Integer> sequence = asList(7, 8, 5, 1, 1, 2, 8, 10, 4, 2, 4, 5, 3, 
				6, 7, 3, 2, 7, 6, 1, 2, 9, 6, 1, 3, 4, 6, 3, 3, 3, 2, 4, 4); 

		tic();
		BarvinokUSimple model = new BarvinokUSimple(sequence, 7);
		System.out.println("Search completed in " + toc() + " seconds.");
			
		System.out.println("The number of graphs is between  " 
					+ pow(2.0, model.lowerBound()) + " and " + pow(2.0, model.upperBound()) + " bits");
	}

}
