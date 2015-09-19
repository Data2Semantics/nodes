package org.nodes;

import java.util.Collection;
import java.util.List;

/**
 * A FastWalkable graph allows fast, random access to the nodes neighboring a
 * given node, but only as a list containing duplicates 
 * 
 * @author Peter
 */
public interface FastWalkable<L, N extends Node<L>>
{
	/**
	 * Returns a collection of neighbors of the given node. The collection 
	 * either contains no duplicate entries, or contains a duplicate of each 
	 * node for for each link into that node.  
	 * 
	 * The returned list should support fast random access.
	 */
	public List<N> neighborsFast(Node<L> node);
}
