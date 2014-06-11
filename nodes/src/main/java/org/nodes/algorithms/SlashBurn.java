package org.nodes.algorithms;

import static org.nodes.util.Series.series;
import static org.nodes.util.Functions.Dir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Global;
import org.nodes.DegreeComparator;
import org.nodes.DegreeIndexComparator;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.clustering.ConnectionClusterer.ConnectionClustering;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;
import org.nodes.util.BitString;
import org.nodes.util.Functions;
import org.nodes.util.MaxObserver;
import org.nodes.util.Order;
import org.nodes.util.Pair;
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
	
	private List<List<Integer>> islands = null;

	private Comparator<Node<N>> centralityComparator;

	public SlashBurn(Graph<N> graph, int k)
	{
		this(graph, k, new DegreeComparator<N>());
	}
	
	public SlashBurn(Graph<N> graph, int k, List<List<Integer>> islands)
	{
		this(graph, k, new DegreeComparator<N>(), islands);
	}
	
	public SlashBurn(Graph<N> graph, int k, Comparator<Node<N>> centralityComparator)
	{
		this(graph, k, centralityComparator, null);
	}
	
	/**
	 * 
	 * @param graph
	 * @param k
	 * @param centralityComparator
	 * @param islands A list to which the islands (non-greatest component clusters) are added
	 */
	public SlashBurn(Graph<N> graph, int k, Comparator<Node<N>> centralityComparator, List<List<Integer>> islands)
	{
		this.graph = graph;
		this.mask = BitString.ones(graph.size());
		
		this.head = new ArrayList<Integer>((int) ((graph.size()+1) * (1.0/1024.0)));
		this.tail = new LinkedList<Integer>();
		
		this.k = k;
		this.centralityComparator = centralityComparator;
		
		this.islands = islands;
		
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
	
	@Deprecated
	public List<Integer> orderInts()
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
		
		return fin;
	}
	
	public Order order()
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
		
		return new Order(fin);
	}
	
	public int headSize()
	{
		return head.size();
	}
	
	public int tailSize()
	{
		return tail.size();
	}
	
	public void iterate()
	{		
		// * Find the hubs
		MaxObserver<Node<N>> observer = new MaxObserver<Node<N>>(k, centralityComparator);	
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
			if(islands != null)
			{
				List<Integer> clusterIndices = new ArrayList<Integer>(clust.cluster(cluster));
				islands.add(clusterIndices);
			}
			
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

	public static <L, T> List<DTNode<L, T>> getHubs(DTGraph<L, T> graph, int k, boolean useSignatures)
	{
		return getHubs(graph, k, -1, useSignatures);
	}
	
	/**
	 * Returns the hubs in a given graph, according to a run of the slashburn algorithm.
	 * 
	 * @param graph
	 * @param k The number of hubs to slash per iteration 
	 * @param iterations The maximum number of iteration (the algorithm may finish earlier)
	 * @param useSignatures A signature is a combination if direction and tag for a given link
	 * 		If true, the degree of a node is the size of the largest wet of links with the same 
	 * 		signature. If false, the basic degree is used. 
	 * @return
	 */
	public static <L, T> List<DTNode<L, T>> getHubs(DTGraph<L, T> graph, int k, int iterations,  boolean useSignatures)
	{
		Comparator<Node<L>> comp = useSignatures ? 
				new SignatureCompWrapper<L, T>() : new DegreeComparator<L>();
				
		SlashBurn<L> sb = new SlashBurn<L>(graph, k, comp);
		
		int i = 0;
		while(!sb.done() && (i < iterations || iterations == -1) )
		{
			sb.iterate();
			i++;
		}
				
		List<DTNode<L, T>> hubs = new ArrayList<DTNode<L,T>>(sb.headSize());
		Order order = sb.order();
				
		for(int newIndex : series(sb.headSize()))
			hubs.add(graph.get(order.originalIndex(newIndex)));
		
		return hubs;
	}
	
	/**
	 * The prime signature of a node in a directed, tagged graph is the combination 
	 * of 'in/out' and a tag which occurs most frequently among all links connected
	 * to the node.
	 * 
	 * @param node
	 * @return A pair
	 */
	public static <L, T> Pair<Dir, T> primeSignature(DTNode<L, T> node)
	{
		FrequencyModel<Pair<Dir, T>> frequencies = new FrequencyModel<Pair<Dir,T>>();
		
		for(DTLink<L, T> link : node.links())
		{
			// * Ignore self links
			if(link.from().equals(link.to()))
				continue;
			
			Dir dir = link.from().equals(node) ? Dir.OUT : Dir.IN;
			frequencies.add(new Pair<Dir, T>(dir, link.tag()));
		}
		
		return frequencies.maxToken();
	}
	
	/**
	 * @param node
	 * @return
	 */
	public static <L, T> int primeDegree(DTNode<L, T> node)
	{
		FrequencyModel<Pair<Dir, T>> frequencies = new FrequencyModel<Pair<Dir,T>>();
		
		for(DTLink<L, T> link : node.links())
		{
			// * Ignore self links
			if(link.from().equals(link.to()))
				continue;
			
			Dir dir = link.from().equals(node) ? Dir.OUT : Dir.IN;
			frequencies.add(new Pair<Dir, T>(dir, link.tag()));
		}
		
		return (int)frequencies.frequency(frequencies.maxToken());
	}
	
	/**
	 * Compares nodes based on degree, filtered by tag and direction. When using this
	 * comparator, the low degree nodes are place below the high degree nodes.
	 * 
	 * @author Peter
	 *
	 * @param <L>
	 * @param <T>
	 */
	public static class SignatureComparator<L, T> implements Comparator<DTNode<L, T>>
	{

		@Override
		public int compare(DTNode<L, T> first, DTNode<L, T> second)
		{
			int firstFreq = primeDegree(first), secondFreq = primeDegree(second);
			
			return Double.compare(firstFreq, secondFreq);
		}
		
	}
	
	/**
	 * To resolve casting issues
	 */
	private static class SignatureCompWrapper<L, T> implements Comparator<Node<L>>
	{

		@Override
		@SuppressWarnings("unchecked")
		public int compare(Node<L> first, Node<L> second)
		{
			DTNode<L, T> f = (DTNode<L, T>)first;
			DTNode<L, T> s = (DTNode<L, T>)second;
			
			int firstFreq = primeDegree(f), secondFreq = primeDegree(s);
			
			return Double.compare(firstFreq, secondFreq);
		}
		
	}
}
