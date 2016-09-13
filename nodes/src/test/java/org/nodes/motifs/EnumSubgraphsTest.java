package org.nodes.motifs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.UGraph;
import org.nodes.UNode;

public class EnumSubgraphsTest {

	/**
	 * Test the graph from figure 2 of the paper
	 */
	@Test
	public void test() 
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> n0 = graph.add(null);
		UNode<String> n1 = graph.add(null);
		UNode<String> n2 = graph.add(null);
		UNode<String> n3 = graph.add(null);
		UNode<String> n4 = graph.add(null);
		UNode<String> n5 = graph.add(null);
		UNode<String> n6 = graph.add(null);
		UNode<String> n7 = graph.add(null);
		UNode<String> n8 = graph.add(null);
		
		n0.connect(n1);
		n1.connect(n2);
		n2.connect(n0);
		
		n0.connect(n3);
		n0.connect(n4);
		
		n1.connect(n5);
		n1.connect(n6);
		
		n2.connect(n7);
		n2.connect(n8);
		
		AllSubgraphs as = new AllSubgraphs(graph, 3);
		
		List<Set<Integer>> subgraphList = new ArrayList<Set<Integer>>();
		Set<Set<Integer>> subgraphSet = new LinkedHashSet<Set<Integer>>();

		for(Set<Integer> sub : as)
		{
			subgraphList.add(sub);
			subgraphSet.add(sub);
		}
		
		assertEquals(16, subgraphList.size());
		assertEquals(subgraphSet.size(), subgraphList.size());
	}

	@Test
	public void testD() 
	{
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		DNode<String> n0 = graph.add(null);
		DNode<String> n1 = graph.add(null);
		DNode<String> n2 = graph.add(null);
		DNode<String> n3 = graph.add(null);
		DNode<String> n4 = graph.add(null);
		DNode<String> n5 = graph.add(null);
		DNode<String> n6 = graph.add(null);
		DNode<String> n7 = graph.add(null);
		DNode<String> n8 = graph.add(null);
		
		n0.connect(n1);
		n1.connect(n2);
		n2.connect(n0);
		
		n0.connect(n3);
		n0.connect(n4);
		
		n1.connect(n5);
		n1.connect(n6);
		
		n2.connect(n7);
		n2.connect(n8);
		
		AllSubgraphs as = new AllSubgraphs(graph, 3);
		
		List<Set<Integer>> subgraphList = new ArrayList<Set<Integer>>();
		Set<Set<Integer>> subgraphSet = new LinkedHashSet<Set<Integer>>();

		for(Set<Integer> sub : as)
		{
			subgraphList.add(sub);
			subgraphSet.add(sub);
		}
		
		assertEquals(16, subgraphList.size());
		assertEquals(subgraphSet.size(), subgraphList.size());
	}
}
