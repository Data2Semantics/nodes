package org.nodes;

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

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

public class LightUGraphTest
{

	@Test
	public void testMapDTGraph()
	{
		UGraph<String> graph = new LightUGraph<String>();
		assertEquals(0, graph.size());
		assertEquals(0, graph.numLinks());
	}

	@Test
	public void testToString()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b");
		graph.add("c");
	
		a.connect(b);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c"),
		              d = graph.add("d"),
		              e = graph.add("e");
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph);
		
		e.disconnect(a);
		
		System.out.println(graph);
		
		System.out.println(a.index());
		a.remove();
		
		System.out.println(graph);	
	}
	
	@Test
	public void testRemove()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c"),
		              d = graph.add("d"),
		              e = graph.add("e");
	
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
	public void testConnected()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c");
	
		a.connect(b);
		a.connect(c);
		
		assertTrue(a.connected(b));
		
		assertFalse(a.connected(a));

		assertTrue(b.connected(a));
		
		assertTrue(a.connected(c));

		assertTrue(c.connected(a));
		
		assertFalse(b.connected(c));
		
		assertFalse(c.connected(b));
	}
	
	@Test
	public void testLinks()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add(null),
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
	public void testLinkRemove()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		
		a.connect(b);
		
		a.connect(c);
		a.connect(c);
		
		ULink<String> link = a.links(c).iterator().next();
		link.remove();
		
		assertEquals(2, graph.numLinks());
		assertEquals(1, a.links(c).size());
		assertTrue(link.dead());

		int n = 0;
		for(ULink<String> l : graph.links())
			n++;
		assertEquals(2, n);
	}
	
	@Test
	public void testEquals()
	{
		UGraph<String> g1 = new LightUGraph<String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"));
		g1.node("b").connect(g1.node("c"));
		
		UGraph<String> g2 = new LightUGraph<String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");

		g2.node("b").connect(g2.node("c"));		
		g2.node("a").connect(g2.node("b"));                    
         
		assertTrue(g1.equals(g2));
		
		g2.node("a").connect(g2.node("c"));
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void testNotEquals()
	{
		UGraph<String> g1 = new LightUGraph<String>();
		
		UTGraph<String, String> g2 = new MapUTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
	
	// @Test
	public void copy()
		throws IOException
	{
		UGraph<String> data = 
				Data.edgeList(new File("/Users/Peter/Documents/datasets/graphs/neural/celegans.txt"), false);
		
		data = Graphs.blank(data, "");
		data = Graphs.toSimpleUGraph(data);

		Data.writeEdgeList(data, new File("/Users/Peter/Documents/datasets/graphs/neural/simple.txt"));
	}
	
	@Test
	public void testCopy()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> a = graph.add("a");
		UNode<String> b = graph.add("b");
		UNode<String> c = graph.add("c");

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

		graph = LightUGraph.copy(graph);

		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
	
			assertEquals(5, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
		
		graph = LightUGraph.copy(graph);

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
		UGraph<String> graph = Examples.yeast();
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(2277, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

		graph = LightUGraph.copy(graph);
		
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(2277, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
		
		graph = LightUGraph.copy(graph);
		
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(2277, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
	}		
	
	@Test
	public void testNumLinks()
	{
		UGraph<String> graph = Examples.yeast();
		graph = LightUGraph.copy(graph);
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;
		
		assertEquals(2277, graph.numLinks());
		assertEquals(graph.numLinks(), numLinks);
	}		
	
	@Test
	public void testnumLinks2()
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
		
		int numLinks = 0;
		for(Link<String> link : graph.links())
			numLinks++;
		
		assertEquals(5, graph.numLinks());
		assertEquals(graph.numLinks(), numLinks);
	}
	
	@Test
	public void testRemove2()
	{
		LightUGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null);
	
		a.connect(b);
		b.connect(c);
		c.connect(d);
		d.connect(a);
				
		b.remove();
		
		assertFalse(graph.get(0).connected(graph.get(0)));
		assertFalse(graph.get(0).connected(graph.get(1)));
		assertTrue (graph.get(0).connected(graph.get(2)));
		assertFalse(graph.get(1).connected(graph.get(0)));
		assertFalse(graph.get(1).connected(graph.get(1)));
		assertTrue (graph.get(1).connected(graph.get(2)));
		assertTrue (graph.get(2).connected(graph.get(0)));
		assertTrue (graph.get(2).connected(graph.get(1)));
		assertFalse(graph.get(2).connected(graph.get(2)));
	}

	@Test
	public void testIndices2()
	{		
		// Note that light graphs have non-persistent nodes, so the indices 
		// don't update after removal  
		
		UGraph<String> in = Examples.yeast();
		
		for(int reps : Series.series(100))
		{
			LightUGraph<String> graph = LightUGraph.copy(in);
			
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
		UGraph<String> graph = Examples.yeast();
		graph = LightUGraph.copy(graph);
	
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
		UGraph<String> graph = new LightUGraph<String>();
		
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
	
	@Test
	public void testNeighborsFast()
	{
		UGraph<String> graph = Examples.yeast();
		graph = LightUGraph.copy(graph);
	
		assertTrue(graph instanceof FastWalkable);
		
		for(Node<String> node : graph.nodes())
		{
			Collection<? extends Node<String>> nbs = node.neighbors();
			
			Collection<? extends Node<String>> col = ((FastWalkable<String,? extends Node<String>>)graph).neighborsFast(node);
			
			assertTrue(col instanceof List<?>);
			
			Set<Node<String>> nbsFast = new HashSet<Node<String>>(col);
			
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
	
	// @Test
	public void temp() throws IOException
	{
		UGraph<String> yeast = Data.edgeList(new File("/Users/Peter/Documents/datasets/graphs/yeast-lit/yeast-lit.txt"), false, false);

		
		int numSelfLoops = 0;
		for(Link<String> link : yeast.links())
			if(link.first().index() == link.second().index())
				numSelfLoops ++;
		System.out.println("num self loops " + numSelfLoops);
		
		FrequencyModel<Pair<Integer, Integer>> fm = new FrequencyModel<Pair<Integer,Integer>>();
		yeast = Graphs.toSimpleUGraph(yeast, fm);

		System.out.println("num multi edges " + fm.total());
		
		Data.writeEdgeList(yeast, new File("/Users/Peter/Documents/datasets/graphs/yeast-lit/yeast-lit-simple.txt"));
		yeast = Data.edgeList(new File("/Users/Peter/Documents/datasets/graphs/yeast-lit/yeast-lit-simple.txt"), false, false);
		
		assertTrue(Graphs.isSimple(yeast));
		
		System.out.println("size " + yeast.size());
		System.out.println("num links " + yeast.numLinks());

	}
}
