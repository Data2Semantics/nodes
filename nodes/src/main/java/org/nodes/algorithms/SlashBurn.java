package org.nodes.algorithms;

import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.nodes.Global;
import org.nodes.DegreeComparator;
import org.nodes.DegreeIndexComparator;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.clustering.ConnectionClusterer.ConnectionClustering;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;
import org.nodes.util.BitString;
import org.nodes.util.MaxObserver;
import org.nodes.util.Series;

/**
 * An implementation of the Slash-and-burn algorithm of Kang and Faloutsos.
 * @author Peter
 *
 * @param <N>
 */
public class SlashBurn<N>
{
	private Graph<N> graph;
	
	// Bitmask. If index i is false, the node has been moved to the order list already
	private BitString mask;
	private List<Integer> head, tail;
	private int k, iterations = 0, lastGCCSize = -1;
	
	private ConnectionClustering<N> clust = null;
	private ClusterSizeComparator comp = new ClusterSizeComparator();

	public SlashBurn(Graph<N> graph, int k )
	{
		this.graph = graph;
		this.mask = BitString.ones(graph.size());
		
		this.head = new ArrayList<Integer>((int) ((graph.size()+1) * (1.0/1024.0)));
		this.tail = new LinkedList<Integer>();
		
		this.k = k;
		
		clust = new ConnectionClustering<N>(graph, mask);
		lastGCCSize = clust.clusterSize(clust.largestClusterIndex());
		check();
		
		// * Add the non-GCC nodes to the tail list
		prependNonGCCClusters(tail);
	}
	
	public BitString mask()
	{
		return mask;
	}
	
	public List<Integer> order()
	{

		List<Integer> fin = new ArrayList<Integer>(graph.size());
		
		for(int i : Series.series(graph.size()))
			fin.add(null);

		int c = 0;
		for(int nodeIndex : head)
			fin.set(nodeIndex, c++);
			
		for(int nodeIndex : Series.series(graph.size()))
			if(mask.get(nodeIndex))
				fin.set(nodeIndex, c++);

		for(int nodeIndex : tail)
			fin.set(nodeIndex, c++);
		
		System.out.println("head: "   + head.size());
		System.out.println("mask: "   + mask.numOnes());
		System.out.println("tail: "   + tail.size());
		
		return fin;
	}
	
	public void iterate()
	{
		Comparator<Node<N>> comp = new DegreeComparator<N>();
		
		// * Find the hubs
		MaxObserver<Node<N>> observer = new MaxObserver<Node<N>>(k, comp);	
		for(int i : clust.largestCluster())
			observer.observe(graph.nodes().get(i));
		
		// * Add the hubs to the head list
		for(Node<N> node : observer.elements())
		{
			head.add(node.index());
			mask.set(node.index(), false);
		}
		
		clust = new ConnectionClustering<N>(graph, mask);
		lastGCCSize = clust.clusterSize(clust.largestClusterIndex());
		
		Global.log().info("iteration " + iterations + ": " + clust.numClusters() + " clusters.");
		
		// * Add the non-GCC nodes to the tail list
		prependNonGCCClusters(tail);

		assert(head.size() + tail.size() + mask.numOnes() == graph.size());
		System.out.println(head.size() + tail.size() + mask.numOnes());
		
		iterations ++;
	}
	
	private void prependNonGCCClusters(List<Integer> list)
	{
		if(clust.numClusters() == 0)
			return;
		
		// ** Create an array of cluster indices by increasing size
		List<Integer> clusters = new ArrayList<Integer>(Series.series(clust.numClusters()));
		clusters.remove(clust.largestClusterIndex());
				
		Collections.sort(clusters, this.comp);
		
		// * for each cluster (except the largest)
		for(int cluster : clusters.subList(0, clusters.size()))
		{	
//			List<Integer> nodes = new ArrayList<Integer>(clust.clusterSize(cluster));
//			for(int i : clust.cluster(cluster))
//				nodes.add(i);
			
			// * Sort each cluster first
			//  Collections.sort(nodes, new DegreeIndexComparator(graph));
			
			for(int node : clust.cluster(cluster))
			{
				list.add(0, node);
				mask.set(node, false);
			}
		}
		
	}
	
	/**
	 * Iterates until done.
	 */
	public void finish()
	{
		while(! done())
			iterate();
	}
	
	public boolean done()
	{
		return lastGCCSize <= k;
	}
	
	public int iterations()
	{
		return iterations;
	}
	
	/**
	 * Returns the wing width ratio, assuming that the last iteration was the 
	 * final one.
	 * 
	 * @return
	 */
	public double wingWidthRatio()
	{
		return (k * iterations) / (double) graph.size();
	}
	
	private class ClusterSizeComparator implements Comparator<Integer>
	{

		@Override
		public int compare(Integer o1, Integer o2)
		{
			int size1 = clust.clusterSize(o1);
			int size2 = clust.clusterSize(o2);
			
			return Double.compare(size1, size2);
		}
		
	}
	
	public void check()
	{
		for(int i : series(graph.size()))
			if(mask.get(i))
			{
				if(clust.clusterOf(i) == null)
					throw new RuntimeException();
			} else {
				if(clust.clusterOf(i) != null)
					throw new RuntimeException();
			}
	}
}
