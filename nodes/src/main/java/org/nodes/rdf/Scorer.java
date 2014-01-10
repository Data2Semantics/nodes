package org.nodes.rdf;

import org.nodes.Node;

public interface Scorer
{
	/**
	 * How much this node is wirth at the given distance to
	 * some instance node.
	 * 
	 * @param node
	 * @param depth
	 * @return
	 */
	public double score(Node<String> node, int depth);
}
