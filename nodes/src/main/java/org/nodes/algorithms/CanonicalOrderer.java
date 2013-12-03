package org.nodes.algorithms;

import java.util.List;

import org.nodes.Graph;
import org.nodes.util.Order;

public interface CanonicalOrderer
{
	/**
	 * Provides a canonical ordering for the nodes of a graph.
	 * 
	 * Given two isomorphic graphs, the returned orderings should result in the 
	 * same graph. 
	 * 
	 * 
	 * @author Peter
	 *
	 */
	public <T> Order order(Graph<T> graph);

}
