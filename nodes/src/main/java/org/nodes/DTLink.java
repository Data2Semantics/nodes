package org.nodes;

import java.util.Collection;

public interface DTLink<L, T> extends TLink<L, T>, DLink<L>
{
	public Collection<? extends DTNode<L, T>> nodes();

	public DTGraph<L, T> graph();
	
	public DTNode<L, T> first();
	
	public DTNode<L, T> second();
	
	public DTNode<L, T> from();
	
	public DTNode<L, T> to();
	
	/**
	 * Returns the first node, after one occurrence of the given
	 * node is ignored. If this link link the same node, that node is returned.
	 * 
	 * @param current
	 * @return
	 */
	public DTNode<L, T> other(Node<L> current);
	
}
