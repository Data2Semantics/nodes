package org.nodes;

import static org.junit.Assert.*;
import static org.nodes.util.Series.series;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.algorithms.Nauty;
import org.nodes.data.Examples;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Series;

public class GraphsTest
{

	@Test
	public void testSingle()
	{
		
		
		UTGraph<String, String> single = Graphs.single("x");
		
		System.out.println(single);
		assertEquals(1, single.size());
	}
	
	@Test
	public void testLine()
	{
		UTGraph<String, String> line = Graphs.line(3, "x");
		
		System.out.println(line);
		assertEquals(3, line.size());
		
		UTNode<String, String> node = line.node("x");
		System.out.println(line.nodes().get(1).links(line.nodes().get(2)));
	}
	
	@Test
	public void testLadder()
	{
		int n = 3;
		Graph<String> ladder = Graphs.ladder(3, "x");
		
		System.out.println(ladder);
		assertEquals(n + 2 * (n - 1), ladder.numLinks());
		assertEquals(n*2, ladder.size());
	}
	
	@Test
	public void testAdd()
	{
		UTGraph<String, String> empty = new MapUTGraph<String, String>();
		UTGraph<String, String> k3 = Graphs.k(3, "x");
		
		Graphs.add(empty, k3);
		
		System.out.println(k3);
		System.out.println(empty);
				
		Graphs.add(empty, k3);
		
		System.out.println(empty);
			
	}
	
	@Test
	public void testAll()
	{
		Collection<UGraph<String>> all5 = Graphs.all(5, "");
		
		assertEquals(1024, all5.size());
		int sum = 0;
		for(UGraph<String> graph : all5)
			sum++;
		assertEquals(1024, sum);
		
		System.out.println("All size three graphs");
		for(UGraph<String> graph : Graphs.all(3, ""))
			System.out.println(graph);
		System.out.println();
		
		// * iso classes
		{
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(1, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 1);
		} {
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(2, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 2);
		} {
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(3, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 4);
		} {
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(4, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 11);
		} {
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(5, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 34);
		} {
			Set<Graph<String>> set = new HashSet<Graph<String>>();
			for(Graph<String> graph : Graphs.all(6, ""))
				set.add(Nauty.canonize(graph));
			assertEquals(set.size(), 156);
		}
	}
	
	@Test
	public void testAllIsoConnected()
	{
		assertEquals(1, Graphs.allIsoConnected(0, "").size());
		assertEquals(1, Graphs.allIsoConnected(1, "").size());
		assertEquals(1, Graphs.allIsoConnected(2, "").size());
		assertEquals(2, Graphs.allIsoConnected(3, "").size());
		assertEquals(6, Graphs.allIsoConnected(4, "").size());
		assertEquals(21, Graphs.allIsoConnected(5, "").size());
		assertEquals(112, Graphs.allIsoConnected(6, "").size());
	}
	
	@Test
	public void testSimple()
	{
		UGraph<String> graph = RandomGraphs.random(50, 75);
		
		assertTrue(Graphs.isSimple(graph));
		
		DGraph<String> dgraph = RandomGraphs.randomDirected(50, 0.2);
		
		assertTrue(Graphs.isSimple(dgraph));

	}
	
	@Test
	public void testSimpleExamples()
	{
		// System.out.println("kingjames: " +  Graphs.isSimple(Examples.kingjames()));
		UGraph<String> graph = Examples.yeast();
		System.out.println("yeast: " +      Graphs.hasSelfLoops(graph));
		System.out.println("yeast: " +      Graphs.hasMultiEdges(graph));
		
		for(ULink<String> link : graph.links())
			if(link.first().index() == link.second().index())
				System.out.println(link);

		// System.out.println("physicians: " + Graphs.isSimple(Examples.physicians()));
		// System.out.println("citations: " +  Graphs.isSimple(Examples.citations()));
		
	}

//	@Test
//	public void testJBC()
//	{
//		BaseGraph<String> jbc = Graphs.jbc();
//		
//		System.out.println(jbc);
//	}

}
