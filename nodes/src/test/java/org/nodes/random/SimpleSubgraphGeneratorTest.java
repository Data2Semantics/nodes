package org.nodes.random;

import static org.junit.Assert.*;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.Series.series;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Subgraph;
import org.nodes.algorithms.Nauty;
import org.nodes.data.Data;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Generators;
import org.nodes.util.Order;
import org.nodes.util.Series;

public class SimpleSubgraphGeneratorTest
{

	@Test
	public void test() throws IOException
	{
		
		int n = 50000; 
		
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

}
