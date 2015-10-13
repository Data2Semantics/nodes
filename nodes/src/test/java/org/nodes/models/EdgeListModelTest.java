package org.nodes.models;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.Graph;
import org.nodes.random.RandomGraphs;

public class EdgeListModelTest
{

	@Test
	public void test()
	{
		Graph<String> graph = RandomGraphs.random(1000, 10000);
		
		System.out.println("er " + new ERSimpleModel(false).codelength(graph));
		
		System.out.println("el ");
	}

}
