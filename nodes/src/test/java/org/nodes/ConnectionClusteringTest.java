package org.nodes;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import static org.nodes.clustering.ConnectionClusterer.ConnectionClustering;
import static org.nodes.util.Series.series;

import org.nodes.Global;
import org.nodes.classification.Classified;
import org.nodes.algorithms.FloydWarshall;
import org.nodes.random.RandomGraphs;
import org.nodes.util.BitString;
import org.nodes.util.Series;

public class ConnectionClusteringTest
{

	@Test
	public void testLargestCluster()
	{
		UTGraph<String, String> graph = Graphs.jbc();
		
		graph.add("lone");
		
		ConnectionClustering<String> c = new ConnectionClustering<String>(graph);
		
		for(int i : Series.series(c.numClusters()))
		{
			System.out.println(Subgraph.subgraphIndices(graph, c.cluster(i)));
		}
		System.out.println(Subgraph.subgraphIndices(graph, c.largestCluster()));

	}

	@Test
	public void testMask()
	{
		UTGraph<String, String> graph = Graphs.line(3, "x");
		graph.add("lone");
		
		BitString maskA = BitString.ones(graph.size());
		BitString maskB = BitString.ones(graph.size());
		maskB.set(1, false);
		
		System.out.println(graph);
		
		ConnectionClustering<String> c = new ConnectionClustering<String>(graph);
		ConnectionClustering<String> cA = new ConnectionClustering<String>(graph, maskA);
		ConnectionClustering<String> cB = new ConnectionClustering<String>(graph, maskB);
		
		assertEquals(2, c.numClusters());
		assertEquals(2, cA.numClusters());
		assertEquals(3, cB.numClusters());
	}
	
	@Test 
	public void testLarge()
	{
		int n = 200;
		
		Graph<String> graph = RandomGraphs.preferentialAttachment(n, 3);
		
		// * get a list of nodes sorted by degree
		List<Node<String>> nodes = new ArrayList<Node<String>>(graph.nodes());
		Collections.sort(nodes, Collections.reverseOrder(new DegreeComparator<String>()));
		for(int i : Series.series((int) (0.1 * n)))
			nodes.get(i).remove();
		
		ConnectionClustering<String> c = new ConnectionClustering<String>(graph);
		System.out.println("number of clusters: " + c.numClusters());

		check(graph, c);
	}
	
	@Test 
	public void testLargeMasked()
	{
		Graph<String> graph = RandomGraphs.preferentialAttachment(1000, 3);
		BitString mask = BitString.random(graph.size(), 0.7);
		
		ConnectionClustering<String> c = new ConnectionClustering<String>(graph, mask);
		System.out.println("number of clusters: " + c.numClusters());

		check(graph, c, mask);
		
	}
	
	private <N> void check(Graph<N> graph, ConnectionClustering<N> clust)
	{
		for(Link<N> link : graph.links())
			assertTrue(clust.clusterOf(link.first().index()) == clust.clusterOf(link.second().index()));
		
		FloydWarshall<N> fw = new FloydWarshall<N>(graph);
		for(Node<N> node : graph.nodes())
			for(Node<N> other : graph.nodes())
			{
				double distance = fw.distance(node, other);
				if(distance < Double.POSITIVE_INFINITY)
					assert(clust.clusterOf(node.index()) == clust.clusterOf(other.index()));
				else
					assert(clust.clusterOf(node.index()) != clust.clusterOf(other.index()));
			}
	}
	
	private <N> void check(Graph<N> graph, ConnectionClustering<N> clust, BitString mask)
	{
		for(Link<N> link : graph.links())
			if(mask.get(link.first().index()) && mask.get(link.second().index()))
				assertTrue(clust.clusterOf(link.first().index()) == clust.clusterOf(link.second().index()));
		

	}
	
	@Test 
	public void testMaskedNulls()
	{
		Global.randomSeed();
		Graph<String> graph = RandomGraphs.preferentialAttachment(1000, 3);
		BitString mask = BitString.random(graph.size(), 0.01);
		
		ConnectionClustering<String> c = new ConnectionClustering<String>(graph, mask);
		for(int i : series(graph.size()))
			if(mask.get(i))
				assertTrue(c.clusterOf(i) != null);
			else
				assertTrue(c.clusterOf(i) == null);
	}
	

}
