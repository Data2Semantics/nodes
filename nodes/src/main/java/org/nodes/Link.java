package org.nodes;

import java.util.Collection;

/**
 * Represents a link in a graph. 
 * 
 * @author Peter
 *
 * @param <L>
 */
public interface Link<L>
{
	public Node<L> first();
	
	public Node<L> second();
	
	public Collection<? extends Node<L>> nodes();
	
	public Graph<L> graph();
	
	/**
	 * Removes this link from the network. If multiple links exist between the 
	 * two nodes of this link, only one should be removed.
	 * 
	 * If this method is called during an iteration through the links of this 
	 * network (including walks), it may lead to a ConcurrentModificationException 
	 */
	public void remove();
	
	public boolean dead();
	
	/**
	 * Returns the first node, after one occurrence of the given
	 * node is ignored. If this link link the same node, that node is returned.
	 * 
	 * @param current
	 * @return
	 */
	public Node<L> other(Node<L> current);
}
