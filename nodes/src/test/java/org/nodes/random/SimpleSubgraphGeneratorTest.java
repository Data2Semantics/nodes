package org.nodes.random;

import static nl.peterbloem.kit.Functions.dot;
import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapUTGraph;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.data.Data;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Generators;
import nl.peterbloem.kit.Order;
import nl.peterbloem.kit.Series;

public class SimpleSubgraphGeneratorTest
{

	@Test
	public void test() throws IOException
	{
		
		int n = 500; 
		
		tic();
		DGraph<String> graph = Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/p2p/p2p.30.txt"), true);
		System.out.println("graph loaded. size: "+graph.size()+", num links: "+graph.numLinks()+"");
		
		SimpleSubgraphGenerator sgen = new SimpleSubgraphGenerator(graph, Generators.uniform(3, 7));
		FrequencyModel<DGraph<String>> fm = new FrequencyModel<DGraph<String>>();
		
		Comparator<String> comp = Functions.natural();

		for(int i : series(n))
		{
			if(i > 0 && i % (n/500) == 0)
				System.out.print(".");
			if(i > 0 && i % (n/10) == 0)
				System.out.println();
			
			List<Integer> indices =  sgen.generate();
			DGraph<String> sub =  Subgraph.dSubgraphIndices(graph,indices);
			
			Order canonical = Nauty.order(sub, comp);
			sub = Graphs.reorder(sub, canonical);
			
			List<Integer> occurrence = canonical.apply(indices); 
			
			fm.add(sub);
		}
		
		System.out.println("\n time taken : " + toc());
		
		fm.print(System.out);
//
//		tic();
//		graph = Data.edgeListDirected(new File("/Users/Peter/Documents/datasets/graphs/p2p/p2p.30.txt"), true);
//		System.out.println("graph loaded. size: "+graph.size()+", num links: "+graph.numLinks()+"");
//
//		SubgraphGenerator<String> gen = new SubgraphGenerator<String>(graph, Generators.uniform(3, 7));
//		for(int i : series(n))
//			Subgraph.subgraphIndices(graph, gen.generate().indices());
//		
//		System.out.println("\ntime taken : " + toc());
		
	}
	
	@Test
	public void testUniqueness()
	{
		for(int i : series(100))
		{
			UGraph<String> graph = RandomGraphs.random(5, 4);
			SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(graph, Generators.uniform(3, 4));
			
			List<Integer> sub = gen.generate();
			Set<Integer>  set = new LinkedHashSet<Integer>(sub);
			
			assertEquals(3, sub.size());
			assertEquals(set.size(), sub.size());
		}
	}
	
	@Test
	public void testUniquenessBig()
	{
		for(int i : series(2))
		{
			UGraph<String> graph = RandomGraphs.random(100, 200);
			SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(graph, Generators.uniform(2, 7));
			
			List<Integer> sub = gen.generate();
			Set<Integer>  set = new LinkedHashSet<Integer>(sub);
			
			assertEquals(set.size(), sub.size());
			
			dot(i, 1000);
		}
		
		for(int i : series(5))
		{
			UGraph<String> graph = RandomGraphs.random(1000, 2000);
			SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(graph, Generators.uniform(2, 7));
			for(int j : series(100))
			{
				List<Integer> sub = gen.generate();
				Set<Integer>  set = new LinkedHashSet<Integer>(sub);
				
				assertEquals(set.size(), sub.size());
			}
			
			dot(i, 5);
		}
	}
	
	@Test
	public void testSmall()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> a = graph.add("");
		UNode<String> b = graph.add("");
		UNode<String> c = graph.add("");
		UNode<String> d = graph.add("");
		UNode<String> e = graph.add("");
		
		a.connect(b);
		b.connect(c);
		c.connect(a);
		
		d.connect(e);
		
		SimpleSubgraphGenerator gen = 
				new SimpleSubgraphGenerator(graph, Generators.uniform(3, 3));
				
		assertNull(gen.randomNeighborExhaustive(Arrays.asList(3, 4)));
		assertNull(gen.randomNeighborExhaustive(Arrays.asList(0, 1, 2)));

		graph = new MapUTGraph<String, String>();
		
		a = graph.add("");
		b = graph.add("");
		c = graph.add("");
		d = graph.add("");
		e = graph.add("");
		
		b.connect(c);
		c.connect(d);
		d.connect(e);
		e.connect(b);
		
		gen = new SimpleSubgraphGenerator(graph, Generators.uniform(3, 3));
		
		System.out.println(gen.addNeighbor(Arrays.asList(0)));
		

	}
	
	@Test(expected=RuntimeException.class)
	public void testTooSmall()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> a = graph.add("");
		UNode<String> b = graph.add("");
		UNode<String> c = graph.add("");
		UNode<String> d = graph.add("");
		UNode<String> e = graph.add("");
		
		b.connect(c);
		c.connect(d);
		d.connect(e);
		e.connect(b);
		
		SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(graph, Generators.uniform(5, 6));

		// - This should throw a RuntimeException
		gen.generate();
	}

}
