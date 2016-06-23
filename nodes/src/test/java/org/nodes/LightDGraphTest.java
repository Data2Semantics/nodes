package org.nodes;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.data.Data;
import org.nodes.data.Examples;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class LightDGraphTest
{


	@Test
	public void testMapDTGraph()
	{
		DGraph<String> graph = new LightDGraph<String>();
	}

	@Test
	public void testToString()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add("a"),
		              b = graph.add("b");
		graph.add("c");
	
		a.connect(b);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		LightDGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c"),
		              d = graph.add("d"),
		              e = graph.add("e");
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph);
		System.out.println(e.index());

		
		e.disconnect(a);
		
		System.out.println(graph);
		System.out.println(e.index());

		
		a.remove();
		
		System.out.println(graph);	
		System.out.println(graph.node("e").index());

	}
	
	@Test
	public void testRemove()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null),
		              e = graph.add(null);
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph.numLinks() + " " + graph.size());
		
		assertEquals(4, graph.numLinks());
		assertEquals(5, graph.size());
		
		a.remove();
		
		assertEquals(0, graph.numLinks());
		assertEquals(4, graph.size());
	}
	
	@Test
	public void testRemove2()
	{
		LightDGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null);
	
		a.connect(b);
		b.connect(c);
		c.connect(d);
		d.connect(a);
				
		b.remove();
		
		assertFalse(graph.get(0).connectedTo(graph.get(0)));
		assertFalse(graph.get(0).connectedTo(graph.get(1)));
		assertFalse(graph.get(0).connectedTo(graph.get(2)));
		assertFalse(graph.get(1).connectedTo(graph.get(0)));
		assertFalse(graph.get(1).connectedTo(graph.get(1)));
		assertTrue (graph.get(1).connectedTo(graph.get(2)));
		assertTrue (graph.get(2).connectedTo(graph.get(0)));
		assertFalse(graph.get(2).connectedTo(graph.get(1)));
		assertFalse(graph.get(2).connectedTo(graph.get(2)));
		
		System.out.println(graph);
	}
	
	@Test
	public void testConnected()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b);
		a.connect(c);
		
		assertTrue(a.connected(b));
		assertTrue(a.connectedTo(b));
		
		assertFalse(a.connected(a));
		assertFalse(a.connectedTo(a));

		assertTrue(b.connected(a));
		assertFalse(b.connectedTo(a));
		
		assertTrue(a.connected(c));
		assertTrue(a.connectedTo(c));

		assertTrue(c.connected(a));
		assertFalse(c.connectedTo(a));
		
		assertFalse(b.connected(c));
		assertFalse(b.connectedTo(c));
		
		assertFalse(c.connected(b));
		assertFalse(c.connectedTo(b));
	}
	
	@Test
	public void testLinks()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b);
		
		a.connect(c);
		a.connect(c);
		
		assertEquals(0, a.links(a).size());
		assertEquals(1, a.links(b).size());
		assertEquals(1, b.links(a).size());
		assertEquals(2, a.links(c).size());
		assertEquals(2, c.links(a).size());
	}	
	
	@Test
	public void testEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"));
		g1.node("b").connect(g1.node("c"));
		
		DGraph<String> g2 = new LightDGraph<String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");
		 
		g2.node("a").connect(g2.node("b"));                    
		g2.node("b").connect(g2.node("c"));		
         
		assertTrue(g1.equals(g2));
		
		g2.node("a").connect(g2.node("c"));
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void testNotEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		
		DTGraph<String, String> g2 = new MapDTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
	
	public void testImportBig()
			throws IOException
	{
		DGraph<String> graph = Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/p2p/p2p.30.txt"), true);
		System.out.println(graph.size());
		System.out.println(graph.numLinks());

	}
	
	@Test
	public void testCopy()
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
		
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
	
			assertEquals(5, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

		graph = LightDGraph.copy(graph);

		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
	
			assertEquals(5, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
	}
	
	@Test
	public void testCopy2()
	{
		DGraph<String> graph = Examples.physicians();
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(1098, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

		graph = LightDGraph.copy(graph);
		
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(1098, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
	}		
	
	@Test
	public void testNumLinks2()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add("a");
		DNode<String> b = graph.add("b");
		DNode<String> c = graph.add("c");

		a.connect(a);
		b.connect(c);
		c.connect(a);
		a.connect(c);
		a.connect(c);
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;
		
		assertEquals(5, numLinks);
		assertEquals(graph.numLinks(), numLinks);
	}
	
	/**
	 * 
	 */
	@Test
	public void testIndices2()
	{		
		DGraph<String> in = Examples.physicians();

		for(int x : series(50))
		{
			// Note that light graphs have non-persistent nodes, so node.index() 
			// doesn't update after removal  
			
			LightDGraph<String> graph = LightDGraph.copy(in);
			
			Node<String> node = graph.get(145);
			assertEquals(145, node.index());
			
			graph.get(150).remove(); // edit causes an exception
			
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
				
				if(! a.neighbors().isEmpty())
				{
					Node<String> b = Functions.choose(a.neighbors());
									
					Link<String> link = Functions.choose(a.links(b));
				
					link.remove();
				}
			}
			
			int i = 0;
			for(Node<String> n : graph.nodes())
				assertEquals(i++, n.index());
		}
	}
	
	@Test
	public void testNodeLinks()
	{
		DGraph<String> graph = Examples.physicians();
		graph = LightDGraph.copy(graph);
	
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
		DGraph<String> graph = new LightDGraph<String>();
		
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
		DGraph<String> graph = new LightDGraph<String>();
		
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
	
	@Test
	public void testNeighborsFast()
	{
		DGraph<String> graph = Examples.physicians();
		graph = LightDGraph.copy(graph);
	
		assertTrue(graph instanceof FastWalkable);
		
		for(Node<String> node : graph.nodes())
		{
			Collection<? extends Node<String>> nbs = node.neighbors();
			
			Collection<? extends Node<String>> col = ((FastWalkable<String,? extends Node<String>>)graph).neighborsFast(node);
			Set<Node<String>> nbsFast = new HashSet<Node<String>>(col);
			
			assertTrue(col instanceof List<?>);
			
			List<Integer> nbsList = new ArrayList<Integer>();
			for(Node<String> nod : nbs)
				nbsList.add(nod.index());
			
			List<Integer> nbsFastList = new ArrayList<Integer>();
			for(Node<String> nod : nbsFast)
				nbsFastList.add(nod.index());
			
			Collections.sort(nbsList);
			Collections.sort(nbsFastList);
			
			assertEquals(nbsList, nbsFastList);			
		}
	}
}
