package org.nodes.rdf;

import org.nodes.Node;

public class DepthScorer implements Scorer
{

	@Override
	public double score(Node<String> node, int depth)
	{
		return - depth;
	}

}
