package org.nodes.clustering;

import org.nodes.classification.Classified;
import org.nodes.Graph;
import org.nodes.Node;

public interface Clusterer<L>
{
	public Classified<Node<L>> cluster(Graph<L> graph);
}
