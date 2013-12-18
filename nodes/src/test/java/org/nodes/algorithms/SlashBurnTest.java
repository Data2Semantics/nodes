package org.nodes.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

public class SlashBurnTest
{

	@Test
	public void testGetHubs()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		DTNode<String, String> a = graph.add("a"), b = graph.add("b");
		
		b.connect(a, "0");
		
		graph.add("x").connect(a, "0");
		graph.add("x").connect(a, "0");
		graph.add("x").connect(a, "0");

		graph.add("x").connect(b, "1");
		graph.add("x").connect(b, "2");
		graph.add("x").connect(b, "3");
		graph.add("x").connect(b, "4");
		graph.add("x").connect(b, "5");
		graph.add("x").connect(b, "6");
		graph.add("x").connect(b, "7");
		graph.add("x").connect(b, "8");
		graph.add("x").connect(b, "9");
		graph.add("x").connect(b, "10");
		graph.add("x").connect(b, "11");
		graph.add("x").connect(b, "12");

		
		assertEquals(a, SlashBurn.getHubs(graph, 1, 1, true).get(0));
		assertEquals(b, SlashBurn.getHubs(graph, 1, 1, false).get(0));

	}

}
