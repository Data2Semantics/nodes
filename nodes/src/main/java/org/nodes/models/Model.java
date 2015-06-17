package org.nodes.models;

import org.nodes.Graph;

public interface Model<L, G extends Graph<L>>
{

	/**
	 * Returns the binary logarithm of the probability of the graph under this
	 * model
	 * 
	 * @return
	 */
	public double logProb(G graph);
}
