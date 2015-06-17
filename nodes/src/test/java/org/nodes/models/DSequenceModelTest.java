package org.nodes.models;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Series;

public class DSequenceModelTest
{

	@Test
	public void test1()
	{
		DGraph<?> graph = RandomGraphs.randomDirected(1000, 0.1);
		System.out.println("sampled");
		
		DSequenceModel<String> model = new DSequenceModel<String>(
				graph, false);
		
		System.out.println("The size of the compressed graph is between " 
				+ model.bitsLowerBound() + " and " + model.bitsUpperBound() + " bits");

	}

}
