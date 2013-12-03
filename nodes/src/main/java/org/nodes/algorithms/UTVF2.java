package org.nodes.algorithms;

import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.UTGraph;
import org.nodes.UTNode;
import org.nodes.util.Pair;
import org.nodes.util.Series;

/**
 * VF2 Is an algorithm for graph isomorphism.
 * 
 * Note that this implementation is not thread-safe.
 * 
 * TODO: This implementation doesn't seem to check the tags yet.
 * 
 * @author Peter
 *
 */
public class UTVF2<L, T>
{
	private boolean checkLabels = true;
	private UTGraph<L, T> g1, g2;

	private List<Integer> core1, core2; // * mappings for the current state
	// - Each core list stores the indices of the other graph, mapped to the 
	//   indices of its graph. So core1[3] contain the index of the node in graph2
	//   that is mapped to node 3 in graph1.
	
	private List<Integer> t1, t2; // * free nodes that are neighbors of matched pairs

	public UTVF2(UTGraph<L, T> g1, UTGraph<L, T> g2, boolean checkLabels)
	{	
		this.g1 = g1;
		this.g2 = g2;
		
		core1 = new ArrayList<Integer>(g1.size());
		core2 = new ArrayList<Integer>(g2.size());
		t1 = new ArrayList<Integer>(g1.size());
		t2 = new ArrayList<Integer>(g2.size());
		
		for(int i : Series.series(g1.size()))
		{
			core1.add(null);
			t1.add(null);
		}
		
		for(int i : Series.series(g2.size()))
		{
			core2.add(null);
			t2.add(null);
		}		
		
		this.checkLabels = checkLabels; 

		search(0);
	}
	
	/**
	 * 
	 * @param d
	 * @return true if a 
	 */
	private boolean search(int d)
	{	
		if(full(core2))
			return true; // * Finished
		
		if(! empty(t1) && ! empty(t2))
		{
			// * find the smallest t2 node
			int n2 = min(t2);

			for(Integer n1 : series(t1.size()))
				if(t1.get(n1) != null)
				{
					if(testAndSearch(n1, n2, d))
						return true;
				}
				
			
		} else if(empty(t1) && empty(t2))
		{
			// * find the smallest node in g2 not yet paired
			Integer n2 = -1;
			for(int i : Series.series(g2.size()))
				if(core2.get(i) == null)
				{
					n2 = i;
					break;
				}

			// * for all unpaired g1 nodes
			for(Integer n1 : series(core1.size()))
				if(core1.get(n1) == null)
				{
//					System.out.println(d + ": " + n1 + " " + n2);

					if(testAndSearch(n1, n2, d))
						return true;
				}

		} else
			return false; // * current state cannot result in match
			
		return false;
	}
	
	/**
	 * 
	 * @param n1
	 * @param n2
	 * @param d
	 * @return true if a successful match might still occur from the current state
	 * 		false if backtracking is required.
	 */
	private boolean testAndSearch(int n1, int n2, int d)
	{
		if(feasible(n1, n2))
		{
//			System.out.println("feasible");
			// * add candidates to core
			core1.set(n1, n2);
			core2.set(n2, n1);
			
			// * Add neighbours to the t arrays
			for(Node<L> nn1 : g1.nodes().get(n1).neighbors())
			{
				int nn1i = g1.nodes().indexOf(nn1);
				if(t1.get(nn1i) == null && core1.get(nn1i) == null)
					t1.set(nn1i, d);
			}
			
			for(Node<L> nn2 : g2.nodes().get(n2).neighbors())
			{
				int nn2i = g2.nodes().indexOf(nn2);
				if(t2.get(nn2i) == null && core2.get(nn2i) == null)
					t2.set(nn2i, d);
			}
			
			// * modify t1, t2 accordingly
			Integer t1n1Depth = t1.get(n1);
			t1.set(n1, null);
			Integer t2n2Depth = t2.get(n2);
			t2.set(n2, null);
			
//			System.out.println(" c - " + core1 + " " + core2 + " t - " + t1 + " " + t2);
			if(search(d + 1))
				return true;  // * RETURN Statement
			
			// * Restore the arrays
			// - remove candidates from core
			core1.set(n1, null);
			core2.set(n2, null);
			
			// - modify t1, t2 accordingly
			t1.set(n1, t1n1Depth);
			t2.set(n2, t2n2Depth);
			
			// - restore t1, t2 to depth d
			for(int i : series(t1.size()))
				if(t1.get(i) != null && t1.get(i) >= d)
					t1.set(i, null);
					
			for(int i : series(t2.size()))
				if(t2.get(i) != null && t2.get(i) >= d)
					t2.set(i, null);
			
			// * Optimize: Store the values of t1, t2 added in this scope.
		} 
		
//		else 
//			System.out.println("not feasible");

		return false;
		
	}
	
	/**
	 * A list is "empty" when it contains only nulls.
	 * 
	 * @param list
	 * @return
	 */
	private boolean empty(List<Integer> list)
	{
		for(Integer value : list)
			if(value != null)
				return false;
		
		return true;		
	}
	
	/**
	 * A list is "full" when it contains only non-nulls.
	 * 
	 * @param list
	 * @return
	 */
	private boolean full(List<Integer> list)
	{
		for(Integer value : list)
			if(value == null)
				return false;
		
		return true;		
	}	
	
	/**
	 * Feasibility function. Prunes illogical node matches from the search tree.
	 * 
	 * @param n1
	 * @param n2
	 * @return
	 */
	private boolean feasible(int n1, int n2)
	{
		UTNode<L, T> node1 = g1.nodes().get(n1);
		UTNode<L, T> node2 = g2.nodes().get(n2);
		
		// * check if the labels match
		if(checkLabels && ! node1.label().equals(node2.label()))
			return false;
		
		Set<UTNode<L, T>> 	neighbors1 = new HashSet<UTNode<L, T>>(node1.neighbors()),
							neighbors2 = new HashSet<UTNode<L, T>>(node2.neighbors());
		
		// * check if the degrees match
		if(neighbors1.size() != neighbors2.size())
			return false;
		
		int t1Neighbours = 0, t2Neighbours = 0;
		int o1 = 0, o2 = 0;
		
		// * count the number of neighbours in the core, the t arrays and outside
		for(UTNode<L, T> nn1 : neighbors1)
		{
			int nn1i = nn1.index();
			
			if(core1.get(nn1i) != null)
			{
				// * check if the node that nn1 is matched to is a neighbour of n2
				UTNode<L, T> match = g2.nodes().get(core1.get(nn1i));
				
				if(! match.connected(node2))
					return false;
				
			} else if(t1.get(nn1i) != null)
				t1Neighbours ++;
			else
				o1 ++;
		}
		
		for(UTNode<L, T> nn2 : neighbors2)
		{
			int nn2i = nn2.index();
			
			if(core2.get(nn2i) != null)
			{
				// * check if the node that nn2 is matched to is a neighbour of 
				//   n1
				UTNode<L, T> match = g1.nodes().get(core2.get(nn2i));
				
				if(! match.connected(node1))
					return false;
				
			} else if(t2.get(nn2i) != null)
				t2Neighbours ++;
			else
				o2 ++;
		}	
		
		if(t1Neighbours != t2Neighbours)
			return false;
		
		if(o1 != o2)
			return false;
		
		return true;
	}
	
	/**
	 * True if the two input graphs can be mapped to one another perfectly
	 * @return
	 */
	public boolean matches()
	{
		return full(core1);		
	}
	
	/**
	 * Returns the best mapping found. If {@link match()} returns true, then 
	 * the lists returned contains all nodes of the input graphs. If ...
	 * 
	 * @return
	 */
	public Pair<List<Integer>, List<Integer>> mapping()
	{
		return new Pair<List<Integer>, List<Integer>>(
				Collections.unmodifiableList(core1), 
				Collections.unmodifiableList(core2));
	}
	
	/** 
	 * Returns the first non-null value in the given list.
	 * @param nodes
	 * @return
	 */
	private int min(List<Integer> nodes)
	{
		for(int i : Series.series(nodes.size()))
			if(nodes.get(i) != null)
				return i;
		
		return -1;
	}
}
