package org.nodes;

/**
 * A graph which is rooted has a specific single node which is identified as a 
 * kind of starting point. 
 * 
 * The most common type of rooted graph is a tree.
 *  
 * @author peter
 *
 * @param <L>
 * @param <N>
 */
public interface Rooted<L> extends Graph<L>
{
	public Node<L> root();
}
