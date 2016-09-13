package org.nodes.clustering;

import org.nodes.Graph;
import org.nodes.Node;

import nl.peterbloem.kit.data.classification.Classified;

public interface Clusterer<L>
{
	public Classified<Node<L>> cluster(Graph<L> graph);
}
