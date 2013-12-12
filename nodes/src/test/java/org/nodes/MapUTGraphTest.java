package org.nodes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;

public class MapUTGraphTest
{

	@Test
	public void testMapUTGraph()
	{
		UTGraph<String, Double> graph = new MapUTGraph<String, Double>();
	}

	@Test
	public void testToString()
	{
		UTGraph<String, Double> graph = new MapUTGraph<String, Double>();
		
		UTNode<String, Double> a = graph.add("a"),
		                       b = graph.add("b"),
		                       c = graph.add("c");
	
		a.connect(b, 0.5);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		UTGraph<String, Double> graph = new MapUTGraph<String, Double>();
		
		UTNode<String, Double> a = graph.add("a"),
		                       b = graph.add("b"),
		                       c = graph.add("c"),
		                       d = graph.add("d"),
		                       e = graph.add("e");
	
		b.connect(a, 0.5);
		c.connect(a, 0.5);
		d.connect(a, 0.5);
		e.connect(a, 0.5);
		
		System.out.println(graph);
		
		a.disconnect(b);
		
		System.out.println(graph);
		
		a.remove();
		
		System.out.println(graph);	
	}
	
	@Test
	public void testRemove()
	{
		UTGraph<String, Double> graph = new MapUTGraph<String, Double>();
		
		UTNode<String, Double> a = graph.add("a"),
		                       b = graph.add("b"),
		                       c = graph.add("c"),
		                       d = graph.add("d"),
		                       e = graph.add("e");
	
		b.connect(a, 0.5);
		c.connect(a, 0.5);
		d.connect(a, 0.5);
		e.connect(a, 0.5);
		
		System.out.println(graph.numLinks() + " " + graph.size());
		
		assertEquals(4, graph.numLinks());
		assertEquals(5, graph.size());
		
		a.remove();
		
		assertEquals(0, graph.numLinks());
		assertEquals(4, graph.size());
	}
	
	@Test
	public void testConnected()
	{
		UTGraph<String, Double> graph = new MapUTGraph<String, Double>();
		
		UTNode<String, Double> a = graph.add("a"),
		                       b = graph.add("b"),
		                       c = graph.add("c");

	
		a.connect(b, 0.5);
		
		assertTrue(a.connected(b));
		assertFalse(a.connected(a));
		assertTrue(b.connected(a));
		assertFalse(a.connected(c));
		assertFalse(c.connected(a));
		assertFalse(b.connected(c));
		assertFalse(c.connected(b));
	}
	
	@Test 
	public void testIndices()
	{
		UTGraph<String, String> graph = Graphs.k(3, "x");
		
		for(Node<String> node : graph.nodes())
			System.out.println(node.index());
		
	}
	
	@Test
	public void testEquals()
	{
		UTGraph<String, String> g1 = new MapUTGraph<String, String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"), "1");
		g1.node("a").connect(g1.node("b"), "2");
		g1.node("a").connect(g1.node("b"), "2");

		g1.node("b").connect(g1.node("c"), "1");
		
		UTGraph<String, String> g2 = new MapUTGraph<String, String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");
		 
		g2.node("a").connect(g2.node("b"), "2");
		g2.node("a").connect(g2.node("b"), "1");
		g2.node("a").connect(g2.node("b"), "2");
                             
		g2.node("b").connect(g2.node("c"), "1");		
         
		assertEquals(g1, g2);
		
		g2.node("a").connect(g2.node("b"), "2");
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void neighborTest()
	{
		Graph<String> graph = RandomGraphs.random(20, 0.2);
		
		Node<String> node = Functions.choose(graph.nodes());
		
		int degreeSum = 0;
		for(Node<String> neighbor : node.neighbors())
			degreeSum += node.links(neighbor).size();
		
		assertEquals(node.degree(), degreeSum);
	}
}
