package org.nodes.clustering;

import static org.nodes.util.Series.series;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nodes.classification.Classification;
import org.nodes.classification.Classified;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.util.FrequencyModel;
import org.nodes.util.BitString;
import org.nodes.util.Series;

/**
 * Clusters a graph by (weakly) connected components
 * 
 * @author Peter
 *
 * @param <N>
 */
public class ConnectionClusterer<N> implements Clusterer<N>
{
	
	@Override
	public Classified<Node<N>> cluster(Graph<N> graph)
	{
		ConnectionClustering<N> c = new ConnectionClustering<N>(graph);
		return c.clustered();
	}

	/**
	 * Returns a copy of the (weakly) largest connected component.
	 * @return
	 */
	public static <N> Graph<N> largest(Graph<N> graph)
	{
		ConnectionClustering<N> cc = new ConnectionClustering<N>(graph);
		Collection<Integer> largest = cc.largestCluster();
		
		return Subgraph.subgraphIndices(graph, largest);
	}

	public static class ConnectionClustering<N>
	{
		private List<Integer> clusters;
		private List<List<Integer>> clusterLists;
		
		private int maxCluster = -1;
		private BitString mask = null;
		
		private Graph<N> data;
		
		/**
		 * Clusters the nodes for which the bit in the given bitstring is true.
		 * 
		 * The remaining nodes will have their cluster set to null.
		 * 
		 * @param data
		 * @param mask
		 */
		public ConnectionClustering(Graph<N> data, BitString mask)
		{	
			if(mask != null && data.size() != mask.size())
				throw new IllegalArgumentException("Mask size ("+mask.size()+") should match number of nodes in graph ("+data.size()+").");
			
			this.data = data;
			this.mask = mask;
			
			clusters = new ArrayList<Integer>();
			for(int i : Series.series(data.size()))
				clusters.add(null);
			
			clusterLists = new ArrayList<List<Integer>>();
			
			for(Node<N> node : data.nodes())
				search(data, node);
			
			for(List<Integer> members : clusterLists)
			 	Collections.sort(members);
			
			// check();
		}
		
		public ConnectionClustering(Graph<N> data)
		{	
			this(data, null);
		}
	
		/**
		 * If the given node has no assigned cluster yet, it assigns it a new 
		 * cluster, searches for all nodes reachable from it, and assigns them the 
		 * same cluster. 
		 * 
		 * @param data
		 * @param node
		 */
		private void search(Graph<N> data, Node<N> node)
		{
			if(maskedOut(node))
				return;
						
			if(clusters.get(node.index()) != null)
				return;
			
			Deque<Node<N>> stack = new LinkedList<Node<N>>();
			stack.add(node);
			
			maxCluster ++;
			clusterLists.add(new LinkedList<Integer>());
			
			int cluster = maxCluster;
			
			// * Search depth-first for all nodes connected to this component
			
			while(stack.size() > 0)
			{
				Node<N> current = stack.getLast();
				
				set(current.index(), cluster);
				
				// * search for the first unassigned neighbour node
				boolean complete = true;
				for(Node<N> neighbour : current.neighbors())
					if(! maskedOut(neighbour) && clusters.get(neighbour.index()) == null)
					{
						complete = false;
						stack.add(neighbour);
						break;
					}
				
				if(complete)
					stack.removeLast();

			}
		}
		
		private boolean maskedOut(Node<N> node)
		{
			if(mask == null)
				return false;
			
			return ! mask.get(node.index());
		}
		
		private void set(int nodeIndex, int cluster)
		{
			if(clusters.get(nodeIndex) != null)
				return;
			
			clusters.set(nodeIndex, cluster);
			clusterLists.get(cluster).add(nodeIndex);
		}
		
		public int numClusters()
		{
			return maxCluster + 1;
		}
		
		/**
		 * 
		 * @return the index of the largest cluster, -1 if no clusters were assigned.
		 */
		public int largestClusterIndex()
		{
			int index = -1;
			
			for(int i : Series.series(numClusters()))
				if(index == -1 || clusterLists.get(i).size() > clusterLists.get(index).size())
					index = i;
				
			return index;
		}
		
		public int clusterSize(int i)
		{
			return clusterLists.get(i).size();
		}
		
		public Collection<Integer> cluster(int cluster)
		{
//			List<Integer> indices = new ArrayList<Integer>(clusterSize(cluster));
//			
//			for(int i : Series.series(clusters.size()))
//				if(clusters.get(i) != null && clusters.get(i) == cluster)
//					indices.add(i);
//			
//			return indices;
			
			return Collections.unmodifiableList(clusterLists.get(cluster));
		}
		
		public Integer clusterOf(int index)
		{
			return clusters.get(index);
		}
		
		public Collection<Integer> largestCluster()
		{
			return cluster(largestClusterIndex());
		}
		
		public Classified<Node<N>> clustered()
		{
			return Classification.combine(new ArrayList<Node<N>>(data.nodes()), clusters);
		}
		
		private void check()
		{
			int c = 0;
			for(int cluster : series(clusterLists.size()))
				for(int index : clusterLists.get(cluster))
				{
					if(clusters.get(index) != cluster)
						throw new RuntimeException("...");
					
					c++;
				}

			for(List<Integer> members : clusterLists)
			{
				Set<Integer> set = new HashSet<Integer>(members);
				System.out.println(set.size() + " " + members.size());
			}
			
			
			if(mask != null && c != mask.numOnes())
				throw new RuntimeException("..." + c + " " + mask.numOnes());
		}
	}
	
}
