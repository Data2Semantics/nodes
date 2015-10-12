package org.nodes.models;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.nodes.util.Functions.prefix;
import static org.nodes.motifs.MotifCompressor.MOTIF_SYMBOL;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Functions.log2Factorial;
import static org.nodes.util.Functions.logFactorial;
import static org.nodes.util.Functions.max;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.models.DegreeSequenceModel.Prior;
import org.nodes.motifs.MotifCompressor;
import org.nodes.models.DSequenceEstimator.D;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.OnlineModel;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.bootstrap.LogNormalCI;

/**
 * 
 * @author Peter
 *
 * @param <G>
 */
public class MotifModel
{
	public static <L> double size(Graph<L> graph, Graph<L> sub,
			List<List<Integer>> occurrences, StructureModel<Graph<?>> nullModel, boolean resetWiring)
	{
		FrequencyModel<String> bits = new FrequencyModel<String>();

		boolean directed = (graph instanceof DGraph<?>); 

		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Set<Integer> motifNodes = new HashSet<Integer>();
		Graph<L> subbed;
		if(directed)
			subbed = subbedGraph((DGraph<L>) graph, occurrences, wiring, motifNodes);
		else
			subbed = subbedGraph((UGraph<L>) graph, occurrences, wiring, motifNodes);
		
		if(nullModel instanceof RestrictedToSimple)
		{
			FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();

			if(directed)
				subbed = Graphs.toSimpleDGraph((DGraph<L>)subbed, removals);
			else
				subbed = Graphs.toSimpleUGraph((UGraph<L>)subbed, removals);
			
			List<Integer> additions = new ArrayList<Integer>();
			
			for(Link<L> link : subbed.links())
				if(motifNodes.contains(link.first().index()) || motifNodes.contains((link.second().index())))
				{
					int i = link.first().index(), j = link.second().index();
					
					Pair<Integer, Integer> pair = 
							directed ? Pair.p(i, j) : Pair.p(min(i,  j), max(i, j));
				
					additions.add((int)removals.frequency(pair));
				}
						
			bits.add("multiple-edges", Functions.prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
			bits.add("multiple-edges", OnlineModel.storeSequence(additions)); 
		}
		
		// * Store the labels
		bits.add("labels", log2Choose(occurrences.size(), subbed.size())); 
								
		bits.add("sub", nullModel.codelength(sub));

		bits.add("subbed", nullModel.codelength(subbed));
		
		// * Store the rewiring information
		bits.add("wiring", wiringBits(sub, wiring, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbed.size()));
		
//		bits.print(System.out);

		return bits.total();
	}
	
	public static <L> double sizeBeta(Graph<L> graph, Graph<L> sub,
			List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
	{		
		if(graph instanceof DGraph<?>)
			return sizeBeta((DGraph<L>)graph, (DGraph<L>) sub, occurrences, resetWiring, iterations, alpha);
		else 
			return sizeBeta((UGraph<L>)graph, (UGraph<L>) sub, occurrences, resetWiring, iterations, alpha);
	}
	
	public static <L> double sizeBeta(DGraph<L> graph, DGraph<L> sub,
			List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
	{		
		int numThreads = Runtime.getRuntime().availableProcessors();
		
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Set<Integer> motifNodes = new HashSet<Integer>();

		// * The "rest" of the code (ie. everything but the parts estimated by 
		//   importance sampling)
		FrequencyModel<String> rest = new FrequencyModel<String>();
		
		// * Compute the degree sequence of the subbed graph
		// ... and any node pairs with multiple links
		List<D> degrees = subbedDegrees(graph, occurrences, rest);
				
		// * The estimated cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 
		List<Double> samples = new ArrayList<Double>(iterations);
		DSequenceEstimator<String> motifModel = new DSequenceEstimator<String>(sub);
		DSequenceEstimator<String> subbedModel = new DSequenceEstimator<String>(degrees);
		motifModel.nonuniform(iterations, numThreads);
		subbedModel.nonuniform(iterations, numThreads);
		
		for(int i : series(iterations))
			samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		 
		LogNormalCI ci = new LogNormalCI(samples);
				
		// * parameters
		rest.add("sub", DegreeSequenceModel.prior((DGraph<?>)sub, Prior.COMPLETE));
		rest.add("subbed", DegreeSequenceModel.prior(degrees, Prior.COMPLETE));
		
		// * Store the labels
		rest.add("labels", log2Choose(occurrences.size(), degrees.size())); 
				
		// * Store the rewiring information
		rest.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		rest.add("insertions", log2Factorial(graph.size()) - log2Factorial(degrees.size()));
		
//		System.out.println("ci : " + ci.upperBound(alpha));
//		rest.print(System.out);
		
		return ci.upperBound(alpha) + rest.total();
	}	
	
	
	public static List<D> subbedDegrees(
			DGraph<?> graph, List<List<Integer>> occurrences, 
			FrequencyModel<String> rest)
	{
		// * records which node is in which occurrence (if any)
		Map<Integer, Integer> nodeInOccurrence = new HashMap<Integer, Integer>();
		
		for(int occurrenceIndex : series(occurrences.size()))
			for(int nodeIndex : occurrences.get(occurrenceIndex))
				nodeInOccurrence.put(nodeIndex, occurrenceIndex);
		
		FrequencyModel<Pair<Integer, Integer>> nodeToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToNode = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		
		// * Record the in and out degrees
		FrequencyModel<Integer> in = new FrequencyModel<Integer>();
		FrequencyModel<Integer> out = new FrequencyModel<Integer>();
		
		for(DLink<?> link : graph.links())
		{
			int fromInstance = nodeInOccurrence.get(link.from().index()) == null ? -1 : nodeInOccurrence.get(link.from().index()); 
			int toInstance =   nodeInOccurrence.get(link.to().index()) == null ? -1 : nodeInOccurrence.get(link.to().index()); 
		
			if(fromInstance == -1 && toInstance == -1)
			{
				out.add(link.from().index());
				in.add(link.to().index());
				continue;
			}
			
			if(fromInstance == -1)
			{
				Pair<Integer, Integer> n2i = Pair.p(link.from().index(), toInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
				{
					out.add(link.from().index());
					in.add(-(toInstance + 1));
				}
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(toInstance == -1)
			{
				Pair<Integer, Integer> i2n = Pair.p(fromInstance, link.to().index());
				if(instanceToNode.frequency(i2n) == 0.0)
				{
					in.add(link.to().index());
					out.add(-(fromInstance + 1));
				}
				instanceToNode.add(i2n);
				continue;
			}
			
			if(fromInstance != toInstance)
			{
				Pair<Integer, Integer> i2i = Pair.p(fromInstance, toInstance);
				if(instanceToInstance.frequency(i2i) == 0.0)
				{
					out.add(-(fromInstance + 1));
					in.add(-(toInstance + 1));
				}
				instanceToInstance.add(i2i);
			}
		}
		
		Set<Integer> nodes = new LinkedHashSet<Integer>();
		nodes.addAll(in.tokens());
		nodes.addAll(out.tokens());
		List<D> degrees = new ArrayList<D>(nodes.size());
		
		for(int node : nodes)
			degrees.add(new DSequenceEstimator.D((int)in.frequency(node), (int)out.frequency(node)));
				
		List<Integer> additions = new ArrayList<Integer>(graph.size());
		for(Pair<Integer, Integer> token : nodeToInstance.tokens())
			additions.add((int)nodeToInstance.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToNode.tokens())
			additions.add((int)instanceToNode.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToInstance.tokens())
			additions.add((int)instanceToInstance.frequency(token) - 1);
		
		rest.add("multi-edges", Functions.prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
		rest.add("multi-edges", OnlineModel.storeSequence(additions)); 
		
		return degrees;
	}
	
	public static <L> double sizeBeta(UGraph<L> graph, UGraph<L> sub,
			List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
	{		
		int numThreads = Runtime.getRuntime().availableProcessors();
		
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Set<Integer> motifNodes = new HashSet<Integer>();

		// * The "rest" of the code (ie. everything but the parts estimated by 
		//   importance sampling)
		FrequencyModel<String> rest = new FrequencyModel<String>();
		
		// * Compute the degree sequence of the subbed graph
		// ... and any node pairs with multiple links
		List<Integer> degrees = subbedDegrees(graph, occurrences, rest);
				
		// * The estimated cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 
		List<Double> samples = new ArrayList<Double>(iterations);
		USequenceEstimator<String> motifModel = new USequenceEstimator<String>(sub);
		USequenceEstimator<String> subbedModel = new USequenceEstimator<String>(degrees);
		motifModel.nonuniform(iterations, numThreads);
		subbedModel.nonuniform(iterations, numThreads);
		
		for(int i : series(iterations))
			samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		 
		LogNormalCI ci = new LogNormalCI(samples);
				
		// * parameters
		rest.add("sub", DegreeSequenceModel.prior(sub, Prior.COMPLETE));
		rest.add("subbed", DegreeSequenceModel.priorDegrees(degrees, Prior.COMPLETE));
		
		// * Store the labels
		rest.add("labels", log2Choose(occurrences.size(), degrees.size())); 
				
		// * Store the rewiring information
		rest.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		rest.add("insertions", log2Factorial(graph.size()) - log2Factorial(degrees.size()));
		
//		System.out.println("ci : " + ci.upperBound(alpha));
//		rest.print(System.out);
		
		return ci.upperBound(alpha) + rest.total();
	}		
	
	public static List<Integer> subbedDegrees(
			UGraph<?> graph, List<List<Integer>> occurrences, 
			FrequencyModel<String> rest)
	{
		// * records which node is in which occurrence (if any)
		Map<Integer, Integer> nodeInOccurrence = new HashMap<Integer, Integer>();
		
		for(int occurrenceIndex : series(occurrences.size()))
			for(int nodeIndex : occurrences.get(occurrenceIndex))
				nodeInOccurrence.put(nodeIndex, occurrenceIndex);
		
		FrequencyModel<Pair<Integer, Integer>> nodeToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		
		// * Record the in and out degrees
		FrequencyModel<Integer> degrees = new FrequencyModel<Integer>();

		
		for(ULink<?> link : graph.links())
		{
			int firstInstance = nodeInOccurrence.get(link.first().index()) == null ? -1 : nodeInOccurrence.get(link.first().index()); 
			int secondInstance =   nodeInOccurrence.get(link.second().index()) == null ? -1 : nodeInOccurrence.get(link.second().index()); 
		
			if(firstInstance == -1 && secondInstance == -1)
			{
				degrees.add(link.first().index());
				degrees.add(link.second().index());
				continue;
			}
			
			if(firstInstance == -1)
			{
				Pair<Integer, Integer> n2i = Pair.p(link.first().index(), secondInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
				{
					degrees.add(link.first().index());
					degrees.add(-(secondInstance + 1));
				}
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(secondInstance == -1)
			{
				Pair<Integer, Integer> n2i = Pair.p(link.second().index(), firstInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
				{
					degrees.add(link.second().index());
					degrees.add(-(firstInstance + 1));
				}
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(firstInstance != secondInstance)
			{
				Pair<Integer, Integer> i2i = Pair.p(min(firstInstance, secondInstance), max(firstInstance, secondInstance));
				if(instanceToInstance.frequency(i2i) == 0.0)
				{
					degrees.add(-(firstInstance + 1));
					degrees.add(-(secondInstance + 1));
				}
				instanceToInstance.add(i2i);
			}
		}
		
		List<Integer> result = new ArrayList<Integer>((int)degrees.distinct());
		
		for(int node : degrees.tokens())
			result.add((int)degrees.frequency(node));
				
		List<Integer> additions = new ArrayList<Integer>(graph.size());
		for(Pair<Integer, Integer> token : nodeToInstance.tokens())
			additions.add((int)nodeToInstance.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToInstance.tokens())
			additions.add((int)instanceToInstance.frequency(token) - 1);
		
		rest.add("multi-edges", Functions.prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
		rest.add("multi-edges", OnlineModel.storeSequence(additions)); 
		
		return result;
	}	
	
	public static double wiringBits(Graph<?> sub, List<List<Integer>> wiring,
			boolean reset)
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));

		double bits = 0.0;
		for (List<Integer> motifWires : wiring)
		{
			if (reset)
				om = new OnlineModel<Integer>(Series.series(sub.size()));

			for (int wire : motifWires)
				bits += - Functions.log2(om.observe(wire));
		}
		
		return bits;
	}

	public static double sizeER(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring)
	{
		if(graph instanceof DGraph)
			return sizeER((DGraph<?>) graph, (DGraph<?>) sub, occurrences, resetWiring);
		else
			return sizeER((UGraph<?>) graph, (UGraph<?>) sub, occurrences, resetWiring); 
	}
	
	private static ERSimpleModel erModel = new ERSimpleModel(true);
	public static double sizeER(DGraph<?> graph, DGraph<?> sub,
				List<List<Integer>> occurrences, boolean resetWiring)
	{		
		FrequencyModel<String> bits = new FrequencyModel<String>();
		
		bits.add("sub", erModel.codelength(sub));
		sizeSubbedER(graph, sub, occurrences, bits);
		
		// * Store the rewiring information
		bits.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size(); 
		
		bits.add("labels", log2Choose(occurrences.size(), subbedSize)); 

		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbedSize));
		
		return bits.total();
	}
	
	public static double sizeER(UGraph<?> graph, UGraph<?> sub,
				List<List<Integer>> occurrences, boolean resetWiring)
	{		
		FrequencyModel<String> bits = new FrequencyModel<String>();
		
		bits.add("sub", erModel.codelength(sub));
		sizeSubbedER(graph, sub, occurrences, bits);
		
		// * Store the rewiring information
		bits.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size(); 
		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbedSize));
		bits.add("labels", log2Choose(occurrences.size(), subbedSize)); 
		
		// bits.print(System.out);
		
		return bits.total();
	}
	
	private static void sizeSubbedER(DGraph<?> graph, DGraph<?> sub,
			List<List<Integer>> occurrences, FrequencyModel<String> bits)
	{
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size();
		int subbedLinks = 0;
		// - NB: subbedLinks is not simply 
		//      graph.numLinks() - (sub.numLinks() * occurrences.size())
		//   because the subbed graph is a simple graph: any multiple edges 
		//   created by removal of occurrences are removed and stored elsewhere. 
		
		// * records which node is in which occurrence (if any)
		Map<Integer, Integer> nodeInOccurrence = new HashMap<Integer, Integer>();
		
		for(int occurrenceIndex : series(occurrences.size()))
			for(int nodeIndex : occurrences.get(occurrenceIndex))
				nodeInOccurrence.put(nodeIndex, occurrenceIndex);
		
		FrequencyModel<Pair<Integer, Integer>> nodeToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToNode = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		
		for(DLink<?> link : graph.links())
		{
			int fromInstance = nodeInOccurrence.get(link.from().index()) == null ? -1 : nodeInOccurrence.get(link.from().index()); 
			int toInstance =   nodeInOccurrence.get(link.to().index()) == null ? -1 : nodeInOccurrence.get(link.to().index()); 
		
			if(fromInstance == -1 && toInstance == -1)
			{
				subbedLinks++;
				continue;
			}
			
			if(fromInstance == -1)
			{
				Pair<Integer, Integer> n2i = Pair.p(link.from().index(), toInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
					subbedLinks++;
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(toInstance == -1)
			{
				Pair<Integer, Integer> i2n = Pair.p(fromInstance, link.to().index());
				if(instanceToNode.frequency(i2n) == 0.0)
					subbedLinks++;
				instanceToNode.add(i2n);
				continue;
			}
			
			if(fromInstance != toInstance)
			{
				Pair<Integer, Integer> i2i = Pair.p(fromInstance, toInstance);
				if(instanceToInstance.frequency(i2i) == 0.0)
					subbedLinks++;
				instanceToInstance.add(i2i);
			}
		}
		
		// * size of the subbed graph under the binomial compressor
		double n = subbedSize;
		double t = n * n - n;
		
		bits.add("subbed", Functions.prefix((int)n) + Functions.log2(t) + log2Choose(subbedLinks, t));
		
		List<Integer> additions = new ArrayList<Integer>(graph.size());
		for(Pair<Integer, Integer> token : nodeToInstance.tokens())
			additions.add((int)nodeToInstance.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToNode.tokens())
			additions.add((int)instanceToNode.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToInstance.tokens())
			additions.add((int)instanceToInstance.frequency(token) - 1);
				
		bits.add("multiple-edges", Functions.prefix(additions.isEmpty() ? 0 : max(additions)));
		bits.add("multiple-edges", OnlineModel.storeSequence(additions)); 
	}
	
	private static void sizeSubbedER(UGraph<?> graph, UGraph<?> sub,
			List<List<Integer>> occurrences, FrequencyModel<String> bits)
	{
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size();
		int subbedLinks = 0;
		
		// * records which node is in which occurrence (if any)
		Map<Integer, Integer> nodeInOccurrence = new HashMap<Integer, Integer>();
		
		for(int occurrenceIndex : series(occurrences.size()))
			for(int nodeIndex : occurrences.get(occurrenceIndex))
				nodeInOccurrence.put(nodeIndex, occurrenceIndex);
		
		FrequencyModel<Pair<Integer, Integer>> nodeToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		FrequencyModel<Pair<Integer, Integer>> instanceToInstance = 
				new FrequencyModel<Pair<Integer,Integer>>();
		
		for(Link<?> link : graph.links())
		{
			int firstInstance = nodeInOccurrence.get(link.first().index()) == null ? -1 : nodeInOccurrence.get(link.first().index()); 
			int secondInstance =   nodeInOccurrence.get(link.second().index()) == null ? -1 : nodeInOccurrence.get(link.second().index()); 
		
			if(firstInstance == -1 && secondInstance == -1)
			{
				subbedLinks++;
				continue;
			}
			
			if(firstInstance == -1) // second is in an instance
			{
				Pair<Integer, Integer> n2i = Pair.p(link.first().index(), secondInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
					subbedLinks++;
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(secondInstance == -1) // first is in an instance
			{
				Pair<Integer, Integer> n2i = Pair.p(link.second().index(), firstInstance);
				if(nodeToInstance.frequency(n2i) == 0.0)
					subbedLinks++;
				nodeToInstance.add(n2i);
				continue;
			}
			
			if(firstInstance != secondInstance) // both are in an instance
			{
				Pair<Integer, Integer> i2i = Pair.p(Math.min(firstInstance, secondInstance), max(firstInstance, secondInstance));
				if(instanceToInstance.frequency(i2i) == 0.0)
					subbedLinks++;
				instanceToInstance.add(i2i);
			}
		}
		
		// * size of the subbed graph under the binomial compressor
		double n = subbedSize;
		double t = (n * n - n)/2;
		
		bits.add("subbed", Functions.prefix((int)n) + Functions.log2(t) + log2Choose(subbedLinks, t));
		
		List<Integer> additions = new ArrayList<Integer>(graph.size());
		for(Pair<Integer, Integer> token : nodeToInstance.tokens())
			additions.add((int)nodeToInstance.frequency(token) - 1);
		for(Pair<Integer, Integer> token : instanceToInstance.tokens())
			additions.add((int)instanceToInstance.frequency(token) - 1);
				
		bits.add("multiple-edges", Functions.prefix(additions.isEmpty() ? 0 : max(additions)));
		bits.add("multiple-edges", OnlineModel.storeSequence(additions)); 
	}	

	public static double sizeEL(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring)
	{
		if(graph instanceof DGraph)
			return sizeEL((DGraph<?>) graph, (DGraph<?>) sub, occurrences, resetWiring);
		else
			return sizeEL((UGraph<?>) graph, (UGraph<?>) sub, occurrences, resetWiring); 
	}
	
	private static EdgeListModel elModel = new EdgeListModel(true);
	public static double sizeEL(DGraph<?> graph, DGraph<?> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{		
		FrequencyModel<String> bits = new FrequencyModel<String>();
		
		bits.add("sub", elModel.codelength(sub));
		sizeSubbedEL(graph, sub, occurrences, bits);
		
		// * Store the rewiring information
		bits.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size(); 
		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbedSize));
		bits.add("labels", log2Choose(occurrences.size(), subbedSize)); 
		
		return bits.total();
	}
	
	public static double sizeEL(UGraph<?> graph, UGraph<?> sub,
			List<List<Integer>> occurrences, boolean resetWiring)
	{		
		FrequencyModel<String> bits = new FrequencyModel<String>();
		
		bits.add("sub", elModel.codelength(sub));
		sizeSubbedEL(graph, sub, occurrences, bits);
		
		// * Store the rewiring information
		bits.add("wiring", wiringBitsDirect(graph, sub, occurrences, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data
		int subbedSize = graph.size() - (sub.size() - 1) * occurrences.size(); 
		bits.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbedSize));
		bits.add("labels", log2Choose(occurrences.size(), subbedSize)); 
		
		return bits.total();
	}

	private static void sizeSubbedEL(Graph<?> graph, Graph<?> sub,
			List<List<Integer>> occurrences, FrequencyModel<String> bits)
	{
		boolean directed = graph instanceof DGraph<?>;
		
		// - This list holds the index of the occurrence the node belongs to
		List<Integer> inOccurrence = new ArrayList<Integer>(graph.size());
		for (int i : Series.series(graph.size()))
			inOccurrence.add(null);
	
		for (int occIndex : Series.series(occurrences.size()))
			for (Integer i : occurrences.get(occIndex))
				inOccurrence.set(i, occIndex);
	
		OnlineModel<Integer> source = new OnlineModel<Integer>(Collections.EMPTY_LIST);
		OnlineModel<Integer> target = new OnlineModel<Integer>(Collections.EMPTY_LIST);
	
		// - observe all symbols
		for (Node<?> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
			{
				source.add(node.index(), 0.0);
				target.add(node.index(), 0.0);
			}
	
		// - negative numbers represent symbol nodes
		for (int i : Series.series(1, occurrences.size() + 1))
		{
			source.add(-i, 0.0);
			target.add(-i, 0.0);
		}
	
		// * count the number of links in the subbed graph
		int subbedNumLinks = graph.numLinks() - sub.numLinks() * occurrences.size();
	
		// * Size of the subbed graph
		bits.add("subbed", Functions.prefix(graph.size() - (sub.size() - 1) * occurrences.size()));
		// * Num links in the subbed graph
		bits.add("subbed",  Functions.prefix(subbedNumLinks));
	
		for (Link<?> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());
	
			//* If link exists in subbed graph
			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
			{
				int first = link.first().index();
				int second = link.second().index();
	
				first = inOccurrence.get(first) == null ? first
						: -(inOccurrence.get(first) + 1);
				second = inOccurrence.get(second) == null ? second
						: -(inOccurrence.get(second) + 1);
				
				double p;
				if(directed)
				{
					p = source.observe(first) * target.observe(second);
				} else {
					p = source.observe(first) * source.observe(second);
					if(first != second)
						p *= 2.0;
				}
				bits.add("subbed",  -Functions.log2(p));
			}
		}
		
		if(subbedNumLinks < 0)
			System.out.println("!");
	
		bits.add("subbed", - logFactorial(subbedNumLinks, 2.0));
	}
//
//	public static double wiringBits(DGraph<String> graph, DGraph<String> sub, List<List<Integer>> occurrences,
//			boolean reset)
//	{
//		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));
//		
//		double wiringBits = 0.0;
//		
//		for (List<Integer> occurrence : occurrences)
//		{			
//			if(reset)
//				om = new OnlineModel<Integer>(Series.series(sub.size()));
//			
//			// * The index of the node within the occurrence
//			for (int indexInSubgraph : series(occurrence.size()))
//			{
//				DNode<String> node = graph.get(occurrence.get(indexInSubgraph));
//				
//				for(DLink<String> link : node.links())
//				{
//					DNode<String> neighbor = link.other(node);
//					
//					if(! occurrence.contains(neighbor.index()))
//						wiringBits += - log2(om.observe(indexInSubgraph));
//				}
//			}
//		}	
//		
//		return wiringBits;
//	}
//	
	public static <L> double wiringBitsDirect(Graph<L> graph, Graph<?> sub, List<List<Integer>> occurrences,
			boolean reset)
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));
		
		double wiringBits = 0.0;
		
		for (List<Integer> occurrence : occurrences)
		{			
			if(reset)
				om = new OnlineModel<Integer>(Series.series(sub.size()));
			
			// * The index of the node within the occurrence
			for (int indexInSubgraph : series(occurrence.size()))
			{
				Node<L> node = graph.get(occurrence.get(indexInSubgraph));
				
				for(Link<L> link : node.links())
				{
					Node<L> neighbor = link.other(node);
					
					if(! occurrence.contains(neighbor.index()))
						wiringBits += - log2(om.observe(indexInSubgraph));
				}
			}
		}	
		
		return wiringBits;
	}
	
	/**
	 * Create a copy of the input graph with all (known) occurrences of the 
	 * given subgraph replaced by a single node.
	 * 
	 * @param inputGraph
	 * @param sub
	 * @param occurrences non-overlapping
	 * @param wiring
	 * @return
	 */
	public static <L> DGraph<L> subbedGraph(
			DGraph<L> inputGraph,
			List<List<Integer>> occurrences,
			List<List<Integer>> wiring, Set<Integer> motifNodes)
	{
		// * Create a copy of the input.
		//   We will re-purpose node 0 of each occurrence as the new instance 
		//   node, so for those nodes, we set the label to null
		DGraph<L> copy = new MapDTGraph<L, String>();
		Set<Node<L>> mNodes = new LinkedHashSet<Node<L>>();
		
		Set<Integer> firstNodes = new HashSet<Integer>();
		for(List<Integer> occurrence : occurrences)
			firstNodes.add(occurrence.get(0));
		
		// -- copy the nodes
		for (DNode<L> node : inputGraph.nodes())
			if(firstNodes.contains(node.index()))
				mNodes.add(copy.add(null));
			else
				copy.add(node.label());
		
		// note: slightly leaky abstraction, we are counting on MapDTGraph's 
		// persistent node objects (ie. the Node objects in motif Nodes will
		// stay up to date even if we remove others from the graph later.)
		
		// -- copy the links
		for (DLink<L> link : inputGraph.links())
		{
			int i = link.from().index(), 
			    j = link.to().index();
			
			copy.get(i).connect(copy.get(j));
		}

		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<DNode<L>>> occ = 
				new ArrayList<List<DNode<L>>>(occurrences.size());
		
		for (List<Integer> occurrence : occurrences)
		{
			List<DNode<L>> nodes = 
					new ArrayList<DNode<L>>(occurrence.size());
			for (int index : occurrence)
				nodes.add(copy.get(index));
			
			occ.add(nodes);
		}
		
		for (List<DNode<L>> occurrence : occ)
		{
			// * Use the first node of the motif as the symbol node
			DNode<L> newNode = occurrence.get(0);

			// - This will hold the information how each edge into the motif node should be wired
			//   into the motif subgraph (to be encoded later)
			List<Integer> motifWiring = new ArrayList<Integer>(occ.size());
			wiring.add(motifWiring);
			
			for (int indexInSubgraph : series(occurrence.size()))
			{
				DNode<L> node = occurrence.get(indexInSubgraph);
				
				for(DLink<L> link : node.links())
				{
					// If the link is external
					DNode<L> neighbor = link.other(node);
					
					if(! occurrence.contains(neighbor))
					{
						if(!node.equals(newNode))
						{
							if(link.from().equals(node))
								newNode.connect(neighbor);
							else
								neighbor.connect(newNode);
						}
					
						motifWiring.add(indexInSubgraph);
					}
				}
			}

			for (int i : series(1, occurrence.size()))
				occurrence.get(i).remove();
		}
		
		for(Node<L> node : mNodes)
			motifNodes.add(node.index());

		return copy;
	}
	
	/**
	 * Create a copy of the input graph with all given occurrences of the 
	 * given subgraph replaced by a single node.
	 * 
	 * @param inputGraph
	 * @param sub
	 * @param occurrences
	 * @param wiring
	 * @return
	 */
	public static <L> UGraph<L> subbedGraph(
			UGraph<L> inputGraph,
			List<List<Integer>> occurrences,
			List<List<Integer>> wiring,
			Set<Integer> motifNodes)
	{	
		// * Create a copy of the input.
		//   We will re-purpose node 0 of each occurrence as the new instance 
		//   node, so for those nodes, we set the label to null
		UGraph<L> copy = new MapUTGraph<L, String>();
		Set<Node<L>> mNodes = new LinkedHashSet<Node<L>>();
		
		Set<Integer> firstNodes = new HashSet<Integer>();
		for(List<Integer> occurrence : occurrences)
			firstNodes.add(occurrence.get(0));
		
		// -- copy the nodes
		for (UNode<L> node : inputGraph.nodes())
			if(firstNodes.contains(node.index()))
				mNodes.add(copy.add(null));
			else
				copy.add(node.label());
		
		// -- copy the links
		for (ULink<L> link : inputGraph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.get(i).connect(copy.get(j));
		}
		
		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<UNode<L>>> occ = 
				new ArrayList<List<UNode<L>>>(occurrences.size());
		
		for (List<Integer> occurrence : occurrences)
		{
			List<UNode<L>> nodes = 
					new ArrayList<UNode<L>>(occurrence.size());
			for (int index : occurrence)
				nodes.add(copy.get(index));
			
			occ.add(nodes);
		}
		
		for (List<UNode<L>> occurrence : occ)
		{
				// * Wire a new symbol node into the graph to represent the occurrence
				UNode<L> newNode = occurrence.get(0);

				// - This will hold the information how each edge into the motif node should be wired
				//   into the motif subgraph (to be encoded later)
				List<Integer> motifWiring = new ArrayList<Integer>();
				wiring.add(motifWiring);
				
				for (int indexInSubgraph : series(occurrence.size()))
				{
					UNode<L> node = occurrence.get(indexInSubgraph);
					
					for(ULink<L> link : node.links())
					{
						UNode<L> neighbor = link.other(node);
						
						if(! occurrence.contains(neighbor))	// If the link is external
						{
							if(!node.equals(newNode))
								newNode.connect(neighbor);

							motifWiring.add(indexInSubgraph);
						}
					}
				}

				for (int i : series(1, occurrence.size()))
					occurrence.get(i).remove();
		}
		
		for(Node<L> node : mNodes)
			motifNodes.add(node.index());

		return copy;
	}
}
