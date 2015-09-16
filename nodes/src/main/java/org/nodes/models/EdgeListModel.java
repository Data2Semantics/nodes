package org.nodes.models;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.compression.EdgeListCompressor;

public class EdgeListModel implements StructureModel<Graph<? extends Object>>
{
	private boolean withPrior;
	
	public EdgeListModel(boolean withPrior)
	{
		super();
		this.withPrior = withPrior;
	}

	@Override
	public double codelength(Graph<? extends Object> graph)
	{
		if(graph instanceof UGraph<?>)
			return EdgeListCompressor.undirected((UGraph<?>) graph, withPrior);
		
		if(graph instanceof DGraph<?>)
			return EdgeListCompressor.directed((DGraph<?>) graph, withPrior);
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
	}

}
