package org.nodes;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.nodes.data.Examples;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Series;

public class MapDTGraphTest
{

	@Test
	public void testMapDTGraph()
	{
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();
	}

	@Test
	public void testToString()
	{
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();

		DTNode<String, Double> a = graph.add("a"),
				b = graph.add("b"),
				c = graph.add("c");

		a.connect(b, 0.5);

		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();

		DTNode<String, Double> a = graph.add("a"),
				b = graph.add("b"),
				c = graph.add("c"),
				d = graph.add("d"),
				e = graph.add("e");

		b.connect(a, 0.5);
		c.connect(a, 0.5);
		d.connect(a, 0.5);
		e.connect(a, 0.5);

		System.out.println(graph);

		e.disconnect(a);

		System.out.println(graph);

		a.remove();

		System.out.println(graph);	
	}

	@Test
	public void testRemove()
	{
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();

		DTNode<String, Double> a = graph.add("a"),
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
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();

		DTNode<String, Double> a = graph.add("a"),
				b = graph.add("b"),
				c = graph.add("c");


		a.connect(b, 0.5);

		assertTrue(a.connected(b));
		assertTrue(a.connectedTo(b));

		assertFalse(a.connected(a));
		assertFalse(a.connectedTo(a));

		assertTrue(b.connected(a));
		assertFalse(b.connectedTo(a));

		assertFalse(a.connected(c));
		assertFalse(a.connectedTo(c));

		assertFalse(c.connected(a));
		assertFalse(c.connectedTo(a));

		assertFalse(b.connected(c));
		assertFalse(b.connectedTo(c));

		assertFalse(c.connected(b));
		assertFalse(c.connectedTo(b));

	}

	@Test
	public void testConnected2()
	{
		Graph<String> graph = RandomGraphs.randomDirected(20, 0.2);

		for(Node<String> node : graph.nodes())
			for(Node<String> neighbor : node.neighbors())
			{
				assertTrue(node.connected(neighbor));

			}
	}

	@Test
	public void testEquals()
	{
		UTGraph<String, String> g1 = new MapUTGraph<String, String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		g1.add("d");


		g1.node("a").connect(g1.node("b"), "1");
		g1.node("a").connect(g1.node("b"), "2");
		g1.node("a").connect(g1.node("b"), "2");

		g1.node("b").connect(g1.node("c"), "1");

		g1.node("c").connect(g1.node("d"), "1");
		g1.node("c").connect(g1.node("d"), "2");
		g1.node("d").connect(g1.node("c"), "2");

		UTGraph<String, String> g2 = new MapUTGraph<String, String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");
		g2.add("d");

		g2.node("a").connect(g2.node("b"), "2");
		g2.node("a").connect(g2.node("b"), "1");
		g2.node("a").connect(g2.node("b"), "2");

		g2.node("b").connect(g2.node("c"), "1");		

		g2.node("c").connect(g2.node("d"), "2");
		g2.node("c").connect(g2.node("d"), "1");
		g2.node("d").connect(g2.node("c"), "2");

		assertEquals(g1, g2);

		g2.node("a").connect(g2.node("b"), "2");

		assertFalse(g1.equals(g2));
	}

	private DGraph<String> legsDirected()
	{
		DGraph<String> graph = new MapDTGraph<String, String>();

		graph.add("a");
		graph.add("b");
		graph.add("c");

		graph.node("a").connect(graph.node("b"));
		graph.node("b").connect(graph.node("c"));

		return graph;
	}	

	@Test
	public void directionTest()
	{
		DGraph<String> graph = legsDirected(); // a -> b -> c

		FrequencyModel<Boolean> counts = new FrequencyModel<Boolean>();

		for(DNode<String> current : graph.nodes())
			for(DNode<String> neighbor : current.neighbors())
				counts.add(current.connectedTo(neighbor));

		assertEquals(0, (int)(counts.frequency(true) - counts.frequency(false)));
	}

	@Test
	public void containsTest()
	{
		DGraph<String> graph = new MapDTGraph<String, String>();

		graph.add("a");
		graph.add("b");
		graph.add("c");

		Set<Node<String>> set = new LinkedHashSet<Node<String>>();
		Node<String> b = graph.node("b");
		set.add(b);

		assertTrue(set.contains(b));

		graph.node("a").connect(graph.node("b"));

		assertTrue(set.contains(b));

		graph.node("b").connect(graph.node("c"));

		assertTrue(set.contains(b));
	}

	@Test
	public void neighborTest()
	{
		Graph<String> graph = RandomGraphs.randomDirected(20, 0.2);

		Node<String> node = Functions.choose(graph.nodes());

		int degreeSum = 0;
		for(Node<String> neighbor : node.neighbors())
		{
			degreeSum += node.links(neighbor).size();
			System.out.println(degreeSum);
		}

		assertEquals(node.degree(), degreeSum);
	}

	@Test
	public void neighborTestIn()
	{
		DGraph<String> graph = RandomGraphs.randomDirected(20, 0.2);

		DNode<String> node = Functions.choose(graph.nodes());

		System.out.println(graph);

		System.out.println(node.neighbors());
		
		int inDegreeSum = 0;
		for(DNode<String> neighbor : node.neighbors()) {
			inDegreeSum += node.linksIn(neighbor).size();

			System.out.println(node.linksIn(neighbor));
		}
		System.out.println("Indegree: " + node.inDegree());

		assertEquals(node.inDegree(), inDegreeSum);
	}	

	@Test
	public void neighborTestOut()
	{
		DGraph<String> graph = RandomGraphs.randomDirected(20, 0.2);

		DNode<String> node = Functions.choose(graph.nodes());

		int outDegreeSum = 0;
		for(DNode<String> neighbor : node.neighbors())
			outDegreeSum += node.linksOut(neighbor).size();

		assertEquals(node.outDegree(), outDegreeSum);
	}
	
	@Test
	public void testNumLinks()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		DTNode<String, String> a = graph.add("a");
		DTNode<String, String> b = graph.add("b");
		DTNode<String, String> c = graph.add("c");
		
		a.connect(a, "self");
		b.connect(c, "1");
		c.connect(a, "2");
		a.connect(c, "2");
		a.connect(c, "2");
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;
		
		assertEquals(5, numLinks);
		assertEquals(graph.numLinks(), numLinks);
		
	}
	
	@Test
	public void testIndices2()
	{		
		DGraph<String> graph = Examples.physicians();
		
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
		DGraph<String> graph = Examples.physicians();
		graph = MapDTGraph.copy(graph);
	
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
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		DNode<String> a = graph.add("");
		DNode<String> b = graph.add("");
		DNode<String> c = graph.add("");

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
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		DNode<String> a = graph.add("a");
		DNode<String> b = graph.add("b");
		DNode<String> c = graph.add("c");

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
