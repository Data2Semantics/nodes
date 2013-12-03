package org.nodes;

public interface DLink<L> extends Link<L>
{

	public DNode<L> first();
	
	public DNode<L> second();
	
	public DNode<L> from();
	
	public DNode<L> to();
	
	/**
	 * Returns the first node, after one occurrence of the given
	 * node is ignored. If this link link the same node, that node is returned.
	 * 
	 * @param current
	 * @return
	 */
	public DNode<L> other(Node<L> current);
}
