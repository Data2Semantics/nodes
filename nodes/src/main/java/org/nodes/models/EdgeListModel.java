package org.nodes.models;

import java.util.List;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.models.DegreeSequenceModel.Prior;
import org.nodes.util.Functions;
import org.nodes.util.OnlineModel;

public class EdgeListModel implements StructureModel<Graph<? extends Object>>, RestrictedToSimple
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
	
	private static double undirected(List<Integer> degrees, DegreeSequenceModel.Prior prior)
	{
		// * number of links
		int m = 0;
		for(int degree : degrees)
			m += degree;
		
		
		double bits = 0.0;		
		bits += Functions.log2Factorial(m);
		bits -= m;
		
		for(int degree : degrees)
			bits -= 2 * Functions.log2Factorial(degree);
		
		switch(prior)
		{
			case NONE:
				break;
			case ML:
				bits += OnlineModel.storeSequenceML(degrees);
				break;
			case COMPLETE:
				bits += Functions.prefix(degrees.size());
				bits += Functions.prefix(Functions.max(degrees));
				bits += OnlineModel.storeSequence(degrees);
				break;
		}
		
		return bits;
	}
	
	private static double undirected(List<Integer> degreesIn, List<Integer> degreesOut, DegreeSequenceModel.Prior prior)
	{
		// * number of links
		int m = 0;
		for(int degree : degreesIn)
			m += degree;
		
		
		double bits = 0.0;		
		bits += Functions.log2Factorial(m);
		
		for(int degree : degreesIn)
			bits -= Functions.log2Factorial(degree);
		for(int degree : degreesOut)
			bits -= Functions.log2Factorial(degree);
		
		switch(prior)
		{
			case NONE:
				break;
			case ML:
				bits += OnlineModel.storeSequenceML(degreesIn);
				bits += OnlineModel.storeSequenceML(degreesOut);
				break;
			case COMPLETE:
				bits += Functions.prefix(degreesIn.size());
				bits += Functions.prefix(Functions.max(degreesIn));
				bits += OnlineModel.storeSequence(degreesIn);
				bits += Functions.prefix(Functions.max(degreesOut));
				bits += OnlineModel.storeSequence(degreesOut);
				break;
		}
		
		return bits;
	}

}
