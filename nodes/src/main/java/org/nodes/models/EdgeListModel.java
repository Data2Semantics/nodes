package org.nodes.models;

import static org.nodes.Graphs.degrees;
import static org.nodes.Graphs.inDegrees;
import static org.nodes.Graphs.outDegrees;

import java.util.List;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.UGraph;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.models.DSequenceEstimator.D;
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
		return codelength(graph, DegreeSequenceModel.Prior.COMPLETE);
	}
	
	public static double codelength(Graph<?> graph, DegreeSequenceModel.Prior prior)
	{
		if(graph instanceof UGraph<?>)
		{
			List<Integer> degrees = degrees(graph);
			return undirected(degrees, prior);
		}
		
		if(graph instanceof DGraph<?>)
		{
			DGraph<?> dgraph = (DGraph<?>)graph;
			List<Integer> inDegrees = inDegrees(dgraph);
			List<Integer> outDegrees = outDegrees(dgraph);
			
			return directed(inDegrees, outDegrees, prior);
		}
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");	
	}
	
	public static double undirected(List<Integer> degrees, DegreeSequenceModel.Prior prior)
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
	
	public static double directed(List<Integer> degreesIn, List<Integer> degreesOut, DegreeSequenceModel.Prior prior)
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

	public static double directed(List<D> degrees, Prior prior) 
	{
		return directed(DSequenceEstimator.in(degrees), DSequenceEstimator.out(degrees), prior);
	}

}
