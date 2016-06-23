package org.nodes.clustering;

import java.util.List;

import org.nodes.classification.Classified;
import org.nodes.clustering.KMedioids;

import nl.peterbloem.kit.Series;

import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.algorithms.FloydWarshall;

public class GraphKMedoids<L> implements Clusterer<L>
{
	private static int ITS = 50;
	private int k = 7;


	public GraphKMedoids()
	{
	}

	public GraphKMedoids(int k)
	{
		this.k = k;
	}

	@Override
	public Classified<Node<L>> cluster(Graph<L> graph)
	{
		FloydWarshall<L> fw = new FloydWarshall<L>(graph);
		List<Node<L>> nodes = (List<Node<L>>) graph.nodes();
		
		KMedioids<Node<L>> meds = 
				new KMedioids<Node<L>>(nodes, fw, k);
		
		meds.iterate(ITS);
		return meds.clustered();
	}
}
