package org.nodes.models;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.models.old.BarvinokDFull;
import org.nodes.random.RandomGraphs;

public class BarvinokDFullTest
{
	// @Test
	public void test1()
	{
		DGraph<?> graph = RandomGraphs.randomDirected(1000, 0.1);
		System.out.println("sampled");
		
		BarvinokDFull model = new BarvinokDFull(graph, 7);
		
		System.out.println("The size of the compressed graph is between " 
				+ model.lowerBound() + " and " + model.upperBound() + " bits");

	}

}
