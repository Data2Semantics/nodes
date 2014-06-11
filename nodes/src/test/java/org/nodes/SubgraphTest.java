package org.nodes;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class SubgraphTest
{

	public DTGraph<String, String> graph()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		graph.add("58");
		graph.add("1000");
		graph.add("1001");

		graph.node("58").connect(graph.node("1000"), "iwb");
		graph.node("58").connect(graph.node("1001"), "iwb");

		return graph;
	}
	
	@Test
	public void testDTSubgraph()
	{
		DTGraph<String, String> graph = graph();
		
		System.out.println(Subgraph.dtSubgraph(graph(), Arrays.asList(graph.get(0), graph.get(1))));
	}
	
	@Test
	public void testExample()
	{
		// We're using a DTGraph, but we don't care about the specifics
		Graph<String> graph = new MapDTGraph<String, String>();
				
		Node<String>  a = graph.add("a"),
		                         b = graph.add("b"),
		                         c = graph.add("c");

		a.connect(c);
		graph.node("a").connect(graph.node("b"));

		System.out.println(graph);
	}
	
	@Test
	public void testJBC()
	{
		DGraph<String> graph = Graphs.jbcDirected();
				
		List<Integer> nodes = Arrays.asList(13, 15, 16);
		
		DGraph<String> subgraph = Subgraph.dSubgraphIndices(graph, nodes);
		System.out.println(subgraph);
		
		assertEquals(3, subgraph.size());
		assertEquals(2, subgraph.numLinks());
	}

}
