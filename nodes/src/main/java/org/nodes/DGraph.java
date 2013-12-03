package org.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface DGraph<L> extends Graph<L>
{
	/**
	 * Returns the first node in the Graph which has the given label 
	 * 
	 * @param label
	 * @return
	 */
	public DNode<L> node(L label);
	
	public Collection<? extends DNode<L>> nodes(L label);
	
	public List<? extends DNode<L>> nodes();
	
	@Override
	public DNode<L> get(int i);
	
	public Collection<? extends DLink<L>> links();
	
	/**
	 * Adds a new node with the given label 
	 */
	public DNode<L> add(L label);
		
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

	public Class<? extends DGraph<?>> level();

}
