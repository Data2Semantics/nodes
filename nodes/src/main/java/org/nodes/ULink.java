package org.nodes;

import java.util.Collection;

public interface ULink<L> extends Link<L>
{
	@Override
	public UNode<L> first();
	
	@Override
	public UNode<L> second();

	/**
	 * Returns the first node, after one occurrence of the given
	 * node is ignored. If this link link the same node, that node is returned.
	 * 
	 * @param current
	 * @return
	 */
	public UNode<L> other(Node<L> current);
	
	@Override
	public Collection<? extends UNode<L>> nodes();
	
}
