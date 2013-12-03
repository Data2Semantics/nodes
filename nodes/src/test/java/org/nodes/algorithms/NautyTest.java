package org.nodes.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.Graphs.blank;
import static org.nodes.util.Functions.asSet;
import static org.nodes.util.Functions.natural;

import org.nodes.util.Functions.NaturalComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.util.Functions;
import org.nodes.util.Order;
import org.nodes.util.Series;

public class NautyTest
{
	private UGraph<String> legs()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		graph.add("a");
		graph.add("b");
		graph.add("c");
		
		graph.node("a").connect(graph.node("b"));
		graph.node("b").connect(graph.node("c"));
		
		return graph;
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
	
	private DGraph<String> legsDirected2()
	{
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		graph.add("a");
		graph.add("b");
		graph.add("c");
		
		graph.node("a").connect(graph.node("b"));
		graph.node("c").connect(graph.node("b"));
		
		return graph;
	}
	
	private DTGraph<String, String> legsDT1()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		graph.add("x");
		graph.add("x");
		graph.add("x");
		
		graph.get(0).connect(graph.get(1), "r");
		graph.get(1).connect(graph.get(2), "r");
		graph.get(2).connect(graph.get(0), "b");
		
		return graph;
	}
	
	private DTGraph<String, String> legsDT2()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		graph.add("x");
		graph.add("x");
		graph.add("x");
		
		graph.get(0).connect(graph.get(1), "r");
		graph.get(1).connect(graph.get(2), "r");
		graph.get(0).connect(graph.get(2), "b");
		
		return graph;
	}

	private UGraph<String> graph()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		graph.add("a");
		graph.add("b");
		graph.add("c");
		graph.add("d");
		graph.add("e");
		graph.add("f");
		graph.add("g");
		graph.add("h");
		graph.add("i");

		graph.node("a").connect(graph.node("b"));
		graph.node("b").connect(graph.node("c"));
		graph.node("a").connect(graph.node("d"));
		graph.node("b").connect(graph.node("e"));
		graph.node("c").connect(graph.node("f"));
		graph.node("d").connect(graph.node("e"));
		graph.node("e").connect(graph.node("f"));
		graph.node("d").connect(graph.node("g"));
		graph.node("e").connect(graph.node("h"));
		graph.node("f").connect(graph.node("i"));
		graph.node("g").connect(graph.node("h"));
		graph.node("h").connect(graph.node("i"));
		
		return graph;
	}
	
	private UGraph<String> graphLabeled()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		graph.add("r");
		graph.add("b");
		graph.add("r");
		graph.add("b");
		graph.add("r");
		graph.add("b");
		graph.add("r");
		graph.add("b");
		graph.add("r");

		graph.get(0).connect(graph.get(1));
		graph.get(1).connect(graph.get(2));
		graph.get(0).connect(graph.get(3));
		graph.get(1).connect(graph.get(4));
		graph.get(2).connect(graph.get(5));
		graph.get(3).connect(graph.get(4));
		graph.get(4).connect(graph.get(5));
		graph.get(3).connect(graph.get(6));
		graph.get(4).connect(graph.get(7));
		graph.get(5).connect(graph.get(8));
		graph.get(6).connect(graph.get(7));
		graph.get(7).connect(graph.get(8));
		
		return graph;
	}
	
	private UGraph<String> graphLabeled2()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		graph.add("r");
		graph.add("r");
		graph.add("r");
		graph.add("r");
		graph.add("r");
		graph.add("b");
		graph.add("b");
		graph.add("b");
		graph.add("b");

		graph.get(0).connect(graph.get(1));
		graph.get(1).connect(graph.get(2));
		graph.get(0).connect(graph.get(3));
		graph.get(1).connect(graph.get(4));
		graph.get(2).connect(graph.get(5));
		graph.get(3).connect(graph.get(4));
		graph.get(4).connect(graph.get(5));
		graph.get(3).connect(graph.get(6));
		graph.get(4).connect(graph.get(7));
		graph.get(5).connect(graph.get(8));
		graph.get(6).connect(graph.get(7));
		graph.get(7).connect(graph.get(8));
		
		return graph;
	}
	
	private DGraph<String> graphDirected()
	{
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		graph.add("a");
		graph.add("b");
		graph.add("c");
		graph.add("d");
		graph.add("e");
		graph.add("f");
		graph.add("g");
		graph.add("h");
		graph.add("i");

		graph.node("a").connect(graph.node("b"));
		graph.node("b").connect(graph.node("c"));
		graph.node("a").connect(graph.node("d"));
		graph.node("b").connect(graph.node("e"));
		graph.node("c").connect(graph.node("f"));
		graph.node("d").connect(graph.node("e"));
		graph.node("e").connect(graph.node("f"));
		graph.node("d").connect(graph.node("g"));
		graph.node("e").connect(graph.node("h"));
		graph.node("f").connect(graph.node("i"));
		graph.node("g").connect(graph.node("h"));
		graph.node("h").connect(graph.node("i"));
		
		return graph;
	}
	
	private DTGraph<String, String> graphDT()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		graph.add("a");
		graph.add("b");
		graph.add("c");
		graph.add("d");
		graph.add("e");
		graph.add("f");
		graph.add("g");
		graph.add("h");
		graph.add("i");

		graph.node("a").connect(graph.node("b"), "r");
		graph.node("b").connect(graph.node("c"), "r");
		graph.node("a").connect(graph.node("d"), "r");
		graph.node("b").connect(graph.node("e"), "r");
		graph.node("c").connect(graph.node("f"), "r");
		graph.node("d").connect(graph.node("e"), "r");
		graph.node("e").connect(graph.node("f"), "b");
		graph.node("d").connect(graph.node("g"), "b");
		graph.node("e").connect(graph.node("h"), "b");
		graph.node("f").connect(graph.node("i"), "b");
		graph.node("g").connect(graph.node("h"), "b");
		graph.node("h").connect(graph.node("i"), "b");
		
		return graph;
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRefine1()
	{
		Graph<String> graph = graph();
				
		List<List<Node<String>>> unitPartition = new ArrayList<List<Node<String>>>();
		unitPartition.add(new ArrayList<Node<String>>(graph.nodes()));

		List<List<Node<String>>> expected = new ArrayList<List<Node<String>>>();
		expected.add(asList(graph.node("a"), graph.node("c"), graph.node("g"), graph.node("i")));
		expected.add(asList(graph.node("b"), graph.node("d"), graph.node("f"), graph.node("h")));
		expected.add(asList(graph.node("e")));
		
		assertEquals(expected, Nauty.refine(unitPartition));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRefine1Directed()
	{
		Graph<String> graph = graphDirected();
				
		List<List<Node<String>>> unitPartition = new ArrayList<List<Node<String>>>();
		unitPartition.add(new ArrayList<Node<String>>(graph.nodes()));

		List<List<Node<String>>> expected = new ArrayList<List<Node<String>>>();
		expected.add(asList(graph.node("i")));
		expected.add(asList(graph.node("c"), graph.node("g")));
		expected.add(asList(graph.node("f"), graph.node("h")));
		expected.add(asList(graph.node("a")));
		expected.add(asList(graph.node("e")));
		expected.add(asList(graph.node("b"), graph.node("d")));
		
		assertEquals(expected, Nauty.refine(unitPartition));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRefine2()
	{
		Graph<String> graph = graph();
				
		List<List<Node<String>>> partition = new ArrayList<List<Node<String>>>();
		partition.add(asList(graph.node("a")));
		partition.add(asList(graph.node("c"), graph.node("g"), graph.node("i")));
		partition.add(asList(graph.node("b"), graph.node("d"), graph.node("f"), graph.node("h")));
		partition.add(asList(graph.node("e")));
		
		List<List<Node<String>>> expected = new ArrayList<List<Node<String>>>();
		expected.add(asList(graph.node("a")));
		expected.add(asList(graph.node("c"), graph.node("g")));
		expected.add(asList(graph.node("i")));
		expected.add(asList(graph.node("f"), graph.node("h")));
		expected.add(asList(graph.node("b"), graph.node("d")));
		expected.add(asList(graph.node("e")));
		
		assertEquals(expected, Nauty.refine(partition));
	}

	@Test
	public void testDegree()
	{
		UGraph<String> graph = graph();
				
		List<List<Node<String>>> unitPartition = new ArrayList<List<Node<String>>>();
		unitPartition.add(new ArrayList<Node<String>>(graph.nodes()));
		
		assertEquals(2, Nauty.degree(graph.node("a"), unitPartition.get(0)));
		assertEquals(4, Nauty.degree(graph.node("e"), unitPartition.get(0)));	
	}
	
	@Test
	public void testSearch()
	{
		UGraph<String> graph = Graphs.blank(graph(), "x"), orderedA, orderedB;
		Order order;
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		
		orderedA = Graphs.reorder(graph, order);

		// * re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		order = Nauty.order(graph, new NaturalComparator<String>());		
				
		orderedB = Graphs.reorder(graph, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));
	}
	
	@Test
	public void testSearchLabeled()
	{
		UGraph<String> graph = graphLabeled(), orderedA, orderedB, orderedC,
				other = graphLabeled2();
		
		Order order;
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		
		orderedA = Graphs.reorder(graph, order);

		// * re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		order = Nauty.order(graph, new NaturalComparator<String>());		
				
		orderedB = Graphs.reorder(graph, order);
		
		// * Canonical isomorph for the other graph
		order = Nauty.order(other, new NaturalComparator<String>());
		orderedC = Graphs.reorder(other, order);		
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));
		
		assertFalse(orderedA.equals(orderedC));
		assertFalse(orderedC.equals(orderedA));
		
		assertFalse(orderedB.equals(orderedC));
		assertFalse(orderedC.equals(orderedB));
	}	
	
	
	@Test
	public void testSearchLegs()
	{
		UGraph<String> graph = Graphs.blank(legs(), "x"), orderedA, orderedB;
		Order order;
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		
		orderedA = Graphs.reorder(graph, order);

		// * re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		order = Nauty.order(graph, new NaturalComparator<String>());		
				
		orderedB = Graphs.reorder(graph, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));
	}
	
	@Test
	public void testSearchLegsDirected()
	{
		DGraph<String> graph = blank(legsDirected(), "x"), orderedA, orderedB, orderedC;
		Order order;
	
		DGraph<String> other = blank(legsDirected2(), "x");
		
		System.out.println(blank(legsDirected(), "x") + " " + blank(legsDirected2(), "x"));
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new Functions.NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedA = Graphs.reorder(graph, order);

		// * Re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		
		order = Nauty.order(graph, new NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedB = Graphs.reorder(graph, order);
		
		// * Canonical isomorph for the other graph
		order = Nauty.order(other, new NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedC = Graphs.reorder(other, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));

		assertFalse(orderedA.equals(orderedC));
		assertFalse(orderedC.equals(orderedA));
		
		assertFalse(orderedB.equals(orderedC));
		assertFalse(orderedC.equals(orderedB));
	}
	
	@Test
	public void testSearchLegsDT()
	{
		DTGraph<String, String> graph = legsDT1(), orderedA, orderedB, orderedC;
		Order order;
	
		DTGraph<String, String> other = legsDT2();
		
		System.out.println(legsDT1() + " " + legsDT2());
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedA = Graphs.reorder(graph, order);

		// * Re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		
		order = Nauty.order(graph, new NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedB = Graphs.reorder(graph, order);
		
		// * Canonical isomorph for the other graph
		order = Nauty.order(other, new NaturalComparator<String>());
		System.out.println(order + " " + graph);
		orderedC = Graphs.reorder(other, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));

		assertFalse(orderedA.equals(orderedC));
		assertFalse(orderedC.equals(orderedA));
		
		assertFalse(orderedB.equals(orderedC));
		assertFalse(orderedC.equals(orderedB));
	}	
	
	@Test
	public void testSearchDirected()
	{
		UGraph<String> graph = Graphs.blank(graph(), "x"), orderedA, orderedB;
		Order order;
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		
		orderedA = Graphs.reorder(graph, order);

		// * re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		order = Nauty.order(graph, new NaturalComparator<String>());		
				
		orderedB = Graphs.reorder(graph, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));
	}
	
	@Test
	public void testSearchDT()
	{
		DTGraph<String, String> graph = graphDT(), orderedA, orderedB;
		Order order;
		
		// * Find the canonical order for the graph
		order = Nauty.order(graph, new NaturalComparator<String>());
		
		orderedA = Graphs.reorder(graph, order);

		// * re-order graph and test 
		graph = Graphs.reorder(graph, Order.random(graph.size()));
		order = Nauty.order(graph, new NaturalComparator<String>());		
				
		orderedB = Graphs.reorder(graph, order);
		
		assertTrue(orderedA.equals(orderedB));
		assertTrue(orderedB.equals(orderedA));
	}
}
