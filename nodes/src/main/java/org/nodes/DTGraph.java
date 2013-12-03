package org.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A directed graph with both nodes and links labeled. The labels of graph links are 
 * called tags. TGraph is short for Tagged Graph
 * 
 * @author Peter
 *
 * @param <L>
 * @param <T>
 */
public interface DTGraph<L, T> extends DGraph<L>, TGraph<L, T>
{
	/**
	 * Returns the first node in the Graph which has the given label 
	 * 
	 * @param label
	 * @return
	 */
	public DTNode<L, T> node(L label);
	
	public Set<? extends DTNode<L, T>> nodes(L label);
	
	public List<? extends DTNode<L, T>> nodes();
	
	@Override
	public DTNode<L, T> get(int i);
	
	public Collection<? extends DTLink<L, T>> links();	
	
	/**
	 * Adds a new node with the given label 
	 */
	public DTNode<L, T> add(L label);
	
	public int numLinks();
	
	/**
	 * Returns the node labels
	 * @return
	 */
	public Set<L> labels();
	
	/**
	 * Checks whether two nodes exist with the given labels that are connected.
	 * 
	 * If multiple pairs of nodes exist with these labels, only one of them 
	 * needs to be connected for the method to return true.
	 *  
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean connected(L from, L to);
		
	/**
	 * The state of a graph indicates whether it has changed. If the value 
	 * returned by this method has changed, then a modification has been made. 
	 * If the value is the same, then with great likelihood, the graph has not 
	 * been modified.
	 * 
	 * @return
	 */
	public long state();
	
	public Class<? extends DTGraph<?, ?>> level();

}
