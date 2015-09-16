package org.nodes.models;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.models.DSequenceEstimator.in;
import static org.nodes.models.DSequenceEstimator.out;

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
import org.nodes.util.bootstrap.LogNormalCI;

public class DegreeSequenceModel implements StructureModel<Graph<? extends Object>>, RestrictedToSimple
{
	/**
	 * Denotes how a degree sequence model should deal with the uncertianty of 
	 * its estimate: whether to lower/upperbound it (by a confidence interval)
	 * or to simply go for the best possible estimate.  
	 */
	public static enum Margin {LOWERBOUND, MEAN, UPPERBOUND}; 
	
	/**
	 * Denotes how to encode the parameters. 
	 * NONE: doesn't encode the parameters
	 * ML: Encodes them with maximum likelihood distribution (still cheating)
	 * COMPLETE: KT estimator
	 */
	public static enum Prior {NONE, ML, COMPLETE}; 
	
	private int iterations;
	private double alpha;
	private Margin margin;
	private Prior prior;
		
	/**
	 * 
	 * @param iterations
	 * @param alpha
	 * @param safe 
	 */
	public DegreeSequenceModel(int iterations, double alpha, Prior prior, Margin margin)
	{
		this.iterations = iterations;
		this.alpha = alpha;
		this.margin = margin;
		this.prior = prior;
	}

	@Override
	public double codelength(Graph<? extends Object> graph)
	{
		LogNormalCI ci = graph instanceof DGraph<?> ? directed((DGraph<?>)graph) : undirected((UGraph<?>)graph); 
		
		double priorBits = graph instanceof DGraph<?> ? prior((DGraph<?>) graph, prior) : prior((UGraph<?>) graph, prior);

		if(margin == Margin.LOWERBOUND)
			return priorBits + ci.lowerBound(priorBits);
		if(margin == Margin.MEAN)
			return priorBits + ci.mlMean();
		if(margin == Margin.UPPERBOUND)
			return priorBits + ci.lowerBound(priorBits);
		
		throw new IllegalStateException();
	}
	
	private LogNormalCI directed(DGraph<?> graph)
	{
		int numThreads = Runtime.getRuntime().availableProcessors();
		DSequenceEstimator<? extends Object> model = new DSequenceEstimator<Object>(graph);
		model.nonuniform(iterations, numThreads);
		return new LogNormalCI(model.logSamples());
	}
	 
	private LogNormalCI undirected(UGraph<?> graph)
	{
		int numThreads = Runtime.getRuntime().availableProcessors();
		USequenceEstimator<? extends Object> model = new USequenceEstimator<Object>(graph);
		model.nonuniform(iterations, numThreads);
		return new LogNormalCI(model.logSamples());
	}
	
	public static double prior(DGraph<?> graph, Prior prior)
	{
		if(prior == Prior.NONE)
			return 0.0;
		
		if(prior == Prior.ML)
			return OnlineModel.storeSequenceML(Graphs.inDegrees(graph)) + OnlineModel.storeSequenceML(Graphs.outDegrees(graph));
		
		if(prior == Prior.COMPLETE)
		{
			List<Integer> in = Graphs.inDegrees(graph);
			List<Integer> out = Graphs.outDegrees(graph);
			
			int maxIn = Functions.max(in);
			int maxOut = Functions.max(out);
	
			return 
				prefix(maxIn) + prefix(maxOut) + 
				OnlineModel.storeSequence(Graphs.inDegrees(graph)) + 
				OnlineModel.storeSequence(Graphs.outDegrees(graph));
		}
		
		throw new IllegalStateException();
	}
	
	public static double prior(UGraph<?> graph, Prior prior)
	{
		if(prior == Prior.NONE)
			return 0.0;
		
		if(prior == Prior.ML)
			return OnlineModel.storeSequenceML(Graphs.degrees(graph));
		
		if(prior == Prior.COMPLETE)
		{
			List<Integer> degrees = Graphs.degrees(graph);
			
			int max = Functions.max(degrees);
	
			return prefix(max) + OnlineModel.storeSequence(Graphs.degrees(graph)) ;
		}
		
		throw new IllegalStateException();
	}
	
	public static double priorDegrees(List<Integer> degrees, Prior prior)
	{
		if(prior == Prior.NONE)
			return 0.0;
		
		if(prior == Prior.ML)
			return OnlineModel.storeSequenceML(degrees);
		
		if(prior == Prior.COMPLETE)
		{
			int max = Functions.max(degrees);
	
			return prefix(max) + OnlineModel.storeSequence(degrees);
		}
		
		throw new IllegalStateException();
	}

	public static double prior(List<D> degrees, Prior prior)
	{
		if(prior == Prior.NONE)
			return 0.0;
		
		if(prior == Prior.ML)
			return OnlineModel.storeSequenceML(in(degrees)) + 
			       OnlineModel.storeSequenceML(out(degrees));
		
		if(prior == Prior.COMPLETE)
		{
			int maxIn = Functions.max(in(degrees));
			int maxOut = Functions.max(out(degrees));
			
			return prefix(maxIn) + prefix(maxOut) + 
			       OnlineModel.storeSequence(in(degrees)) + 
			       OnlineModel.storeSequence(out(degrees));
		}
		
		throw new IllegalStateException();
	}

}
