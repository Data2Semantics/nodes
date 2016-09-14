package org.nodes;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.nodes.data.Data;
import org.nodes.data.Examples;
import org.nodes.random.RandomGraphs;
import org.omg.Messaging.SyncScopeHelper;

import nl.peterbloem.kit.FileIO;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class DiskDGraphTest 
{
	public static File DIR = new File("./tmp/");
	public static File r()
	{
		DIR.mkdirs();
		int id = (new Random()).nextInt(1000000000);
		return new File(DIR, "graph" + id + ".db");
	}

	@Test
	public void testDiskDGraph()
	{
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
	}

	@Test
	public void testToString()
	{
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
		DNode<String> a = graph.add("a"),
		              b = graph.add("b");
		graph.add("c");
	
		a.connect(b);
		
		System.out.println(graph);
	}
	
	@Test
	public void starTest()
	{
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null),
		              e = graph.add(null);
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
				
		assertEquals(4, graph.numLinks());
		assertEquals(5, graph.size());
		
		a.remove();
		
		assertEquals(0, graph.numLinks());
		assertEquals(4, graph.size());
	}
	
	@Test
	public void testRemove2()
	{
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		
		System.out.println(graph.links());
	}
	
	@Test
	public void testLinks2()
	{
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());

		DNode<String> 	a = graph.add(null),
	              		b = graph.add(null),
	              		c = graph.add(null),
	              		d = graph.add(null),
	              		e = graph.add(null),
	              		f = graph.add(null);
		
		a.connect(b);
		b.connect(c);
		c.connect(a);
		
		b.connect(e);
		c.connect(e);
		c.connect(d);
		
		e.connect(d);
		f.connect(d);
		e.connect(f);
		
		List<DLink<String>> links = new ArrayList<DLink<String>>((int)graph.numLinks());
		for(DLink<String> link : graph.links())
			links.add(link);
		
		assertEquals(9, links.size());
		
		System.out.println(graph.links());
	}	
	
	@Test
	public void testEquals()
	{
		Global.randomSeed();
		
		DGraph<String> g1 = new DiskDGraph(r());
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"));
		g1.node("b").connect(g1.node("c"));
		
		DGraph<String> g2 = new DiskDGraph(r());
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
		Global.randomSeed();
		DGraph<String> g1 = new DiskDGraph(r());
		
		DTGraph<String, String> g2 = new MapDTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
	
	public void testImportBig()
			throws IOException
	{
		Global.randomSeed();
		DGraph<String> graph = DiskDGraph.fromFile(new File("/Users/Peter/Documents/datasets/graphs/p2p/p2p.txt"), DIR);

		System.out.println(graph.size());
		System.out.println(graph.numLinks());
	}
	
	
	@Test
	public void testImportDB()
		throws IOException
	{
		Global.randomSeed();

		FileIO.copy("graphs/p2p/p2p.txt", DIR);
		File dbFile = r();
		
		DiskDGraph diskGraph = DiskDGraph.fromFile(new File(DIR, "p2p.txt"), DIR, dbFile);
		DGraph<String> copy = LightDGraph.copy(diskGraph);
		diskGraph.close();

		diskGraph = DiskDGraph.fromDB(dbFile);
		
//		for(int i : series(copy.size()))
//		{
//			String a = diskGraph.get(i).out() + " " + diskGraph.get(i).in();
//			
//			String b = copy.get(i).out() + " " + copy.get(i).in();
//			
//			if(a.equals(b))
//			{
//				System.out.println("d " + a);
//				System.out.println("m " + b);
//			}	
//		}

		assertEquals(copy, diskGraph);
		
	}

	/**
	 * 
	 * 
	 * @throws IOException
	 */
	@Test
	public void testImport()
			throws IOException
	{
		Global.randomSeed();

		FileIO.copy("graphs/p2p/p2p.txt", DIR);
		
		DGraph<String> diskGraph = DiskDGraph.fromFile(new File(DIR, "p2p.txt"), DIR);
		assertEquals(diskGraph.size(), new ArrayList<DNode<String>>(diskGraph.nodes()).size());
		
		List<DLink<String>> links = new ArrayList<DLink<String>>((int)diskGraph.numLinks());
		for(DLink<String> link : diskGraph.links())
			links.add(link);
		
		assertEquals(diskGraph.numLinks(), links.size());

		DGraph<String> memGraph  = Data.edgeListDirectedUnlabeled(new File(DIR, "p2p.txt"), true);
				
//		for(int i : series(memGraph.size()))
//		{
//			String a = diskGraph.get(i).out() + " " + diskGraph.get(i).in();
//			
//			String b = memGraph.get(i).out() + " " + memGraph.get(i).in();
//			
//			if(! a.equals(b))
//			{
//				System.out.println("d " + a);
//				System.out.println("m " + b);
//			}	
//		}

		assertEquals(memGraph, diskGraph);
	}
	
	@Test
	public void testCopy()
	{
		Global.randomSeed();
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

		graph = DiskDGraph.copy(graph, r());

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
		Global.randomSeed();
		DGraph<String> graph = Examples.physicians();
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(1098, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

		graph = DiskDGraph.copy(graph, r());
		
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
	
	@Test
	public void testIndices2()
	{	
		Global.randomSeed();
		DGraph<String> in = Examples.physicians();

		for(int x : series(5))
		{
			// Note that light graphs have non-persistent nodes, so node.index() 
			// doesn't update after removal  
			
			DiskDGraph graph = DiskDGraph.copy(in, r());
			System.out.println(".");
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
			System.out.println(".");
			
			int i = 0;
			for(Node<String> n : graph.nodes())
				assertEquals(i++, n.index());
			System.out.println('_');
		}
	}
	
	@Test
	public void testNodeLinks()
	{
		Global.randomSeed();
		DGraph<String> graph = Examples.physicians();
		graph = DiskDGraph.copy(graph, r());
	
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		Global.randomSeed();
		DGraph<String> graph = new DiskDGraph(r());
		
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
		Global.randomSeed();
		DGraph<String> graph = Examples.physicians();
		graph = DiskDGraph.copy(graph, r());
	
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
	
	/**
	 * Test subgraph extraction
	 * 
	 */
	@Test
	public void testJBC()
	{
		DGraph<String> graph = Graphs.jbcDirected();
		graph = DiskDGraph.copy(graph, r());
		
		List<Integer> nodes = Arrays.asList(13, 15, 16);
		
		DGraph<String> subgraph = Subgraph.dSubgraphIndices(graph, nodes);
		System.out.println(subgraph);
		
		assertEquals(3, subgraph.size());
		assertEquals(2, subgraph.numLinks());
	}
	
	@After
	public void cleanup()
	{
		for(File file : DIR.listFiles())
			file.delete();
	}
}
