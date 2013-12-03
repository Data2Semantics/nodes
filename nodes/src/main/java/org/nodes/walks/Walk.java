package org.nodes.walks;

import org.nodes.Node;

/**
 * A walk represents a series of nodes. 
 * 
 * The nodes in a walk represent a continuous path in the graph from which the 
 * nodes originate. In other words, there must be a link between any two 
 * subsequent nodes in the walk. This is not enforced in any technical way, but 
 * code using implementations of this interface may assume that this is the 
 * case.
 *  
 * For collections of nodes that do not necessarily form a path in the graph, 
 * Graphs provides lightweight methods to switch between collections of
 * nodes and collections of labels 
 * 
 * @author peter
 *
 * @param <L>
 */
public interface Walk<L> extends Iterable<Node<L>>
{
	public Iterable<L> labels();
}
