package org.nodes;

import static org.junit.Assert.*;
import static org.nodes.util.Series.series;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.nodes.algorithms.Nauty;
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

//	@Test
//	public void testJBC()
//	{
//		BaseGraph<String> jbc = Graphs.jbc();
//		
//		System.out.println(jbc);
//	}

}
