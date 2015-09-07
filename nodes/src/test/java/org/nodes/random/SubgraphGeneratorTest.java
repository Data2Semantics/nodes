package org.nodes.random;

import static org.junit.Assert.*;
import static org.nodes.util.Series.series;

import org.junit.Test;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.algorithms.Nauty;
import org.nodes.util.Series;

public class SubgraphGeneratorTest
{

	@Test
	public void testGenerate()
	{
		Graph<String> graph = Graphs.star(20, "x");		
		Graph<String> subTarget = Graphs.line(3, "x");
		subTarget = Nauty.canonize(subTarget);
		
		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(graph, 3);
		
		
//		for(int i : series(100))
//		{
//			Graph<String> sub = Subgraph.subgraph(graph, gen.generate().nodes());
//			sub = Nauty.canonize(sub);
//			
//			assertEquals(subTarget, sub);
//		}
	}

}
