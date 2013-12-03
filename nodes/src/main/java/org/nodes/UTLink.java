package org.nodes;

import java.util.Collection;

public interface UTLink<L, T> extends TLink<L, T>, ULink<L>
{
	public UTNode<L, T> first();
	
	public UTNode<L, T> second();
	
	/**
	 * Returns the first node, after one occurrence of the given
	 * node is ignored. If this link link the same node, that node is returned.
	 * 
	 * @param current
	 * @return
	 */
	public UTNode<L, T> other(Node<L> current);
	
	public Collection<? extends UTNode<L, T>> nodes();

	public UTGraph<L, T> graph();
}
