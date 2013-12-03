package org.nodes;

import static org.junit.Assert.*;

import java.util.Arrays;

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

}
