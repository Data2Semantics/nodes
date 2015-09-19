package org.nodes;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.nodes.data.Examples;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;
import org.nodes.util.Series;

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
	public void testEquals2()
	{		
		UGraph<String> g1 = new MapUTGraph<String, String>();
		UNode<String> 
				a = g1.add(""),
				b = g1.add(""),
				c = g1.add("");
		
		UGraph<String> g2 = new MapUTGraph<String, String>();
		UNode<String> 
				d = g2.add(""),
				e = g2.add(""),
				f = g2.add("");
		
		d.connect(e);
		
		System.out.println(g1.equals(g2));
		System.out.println(g1.hashCode() + " " + g2.hashCode());
		
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
	
	@Test
	public void testSelfLoops()
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
		
		UTNode<String, String> a = graph.add("a");
		
		a.connect(a, "tag");
		
		assertEquals(1, a.links().size());
		assertEquals(1, a.links(a).size());
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;
		
		assertEquals(1, graph.numLinks());
		assertEquals(graph.numLinks(), numLinks);
	}
	
	
	@Test
	public void testNumLinks()
	{
		UGraph<String> graph = Examples.yeast();
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;

		assertEquals(2277, graph.numLinks());
		assertEquals(graph.numLinks(), numLinks);
	}
	
	@Test
	public void testIndices2()
	{		
		UGraph<String> graph = Examples.yeast();
		
		Node<String> node = graph.get(145);
		
		graph.get(146).remove();
		
		assertEquals(145, node.index());
		
		graph.get(144).remove();
		
		assertEquals(144, node.index());
		
		graph.get(144).remove();
		
		boolean exThrown = false;
		try {
			System.out.println(node.index());
		} catch(Exception e)
		{
			exThrown = true;
		}

		assertTrue(exThrown);
		
		// * Do some random removals
		for(int i : Series.series(10))
		{
			// - random node
			graph.get(Global.random().nextInt(graph.size())).remove();
			
			// - random link
			Node<String> a = graph.get(Global.random().nextInt(graph.size()));
			Node<String> b = Functions.choose(a.neighbors());
			
			Link<String> link = Functions.choose(a.links(b));
			link.remove();
		}
		
		int i = 0;
		for(Node<String> n : graph.nodes())
			assertEquals(i++, n.index());
	}
	
	@Test
	public void testNodeLinks()
	{
		UGraph<String> graph = Examples.yeast();
		graph = MapUTGraph.copy(graph);
	
		for(Node<String> node : graph.nodes())
		{
			Collection<? extends Node<String>> nbs = node.neighbors();
						
			for(Node<String> neighbor : nbs)
				assertTrue(node.links(neighbor).size() > 0);
		}
	}
	
	@Test
	public void testNodeLinks2()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> a = graph.add("");
		UNode<String> b = graph.add("");
		UNode<String> c = graph.add("");

		a.connect(a);
		b.connect(c);
		c.connect(a);
		a.connect(c);
		a.connect(c);
		
		{
			Node<String> node = graph.get(0);
			Collection<? extends Node<String>> nbs = node.neighbors();
			assertEquals(2, nbs.size());
			
			assertEquals(1, node.links(graph.get(0)).size());
			assertEquals(0, node.links(graph.get(1)).size());			
			assertEquals(3, node.links(graph.get(2)).size());
		}
		
		{
			Node<String> node = graph.get(1);
			Collection<? extends Node<String>> nbs = node.neighbors();
			assertEquals(1, nbs.size());
			
			assertEquals(0, node.links(graph.get(0)).size());
			assertEquals(0, node.links(graph.get(1)).size());			
			assertEquals(1, node.links(graph.get(2)).size());
		}
		
		{
			Node<String> node = graph.get(2);
			Collection<? extends Node<String>> nbs = node.neighbors();
			assertEquals(2, nbs.size());
			
			assertEquals(3, node.links(graph.get(0)).size());
			assertEquals(1, node.links(graph.get(1)).size());			
			assertEquals(0, node.links(graph.get(2)).size());
		}
	}
	
	@Test
	public void testNeighbors()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a");
		UNode<String> b = graph.add("b");
		UNode<String> c = graph.add("c");

		a.connect(a);
		b.connect(c);
		c.connect(a);
		a.connect(c);
		a.connect(c);
		
		Set<Node<String>> aNbsExpected = new HashSet<Node<String>>(Arrays.asList(a, c));
		Set<Node<String>> bNbsExpected = new HashSet<Node<String>>(Arrays.asList(c));
		Set<Node<String>> cNbsExpected = new HashSet<Node<String>>(Arrays.asList(b, a));
		
		Set<Node<String>> aNbsActual = new HashSet<Node<String>>(a.neighbors());
		Set<Node<String>> bNbsActual = new HashSet<Node<String>>(b.neighbors());
		Set<Node<String>> cNbsActual = new HashSet<Node<String>>(c.neighbors());

		assertEquals(aNbsExpected, aNbsActual);
		assertEquals(bNbsExpected, bNbsActual);
		assertEquals(cNbsExpected, cNbsActual);
	}	
}
