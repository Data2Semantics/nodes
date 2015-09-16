package org.nodes.models;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2Choose;
import static org.nodes.util.Functions.log2Factorial;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.Series.series;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gephi.io.generator.plugin.RandomGraph;
import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.UGraph;
import org.nodes.data.Data;
import org.nodes.data.Examples;
import org.nodes.models.DegreeSequenceModel.Prior;
import org.nodes.motifs.DPlainMotifExtractor;
import org.nodes.motifs.UPlainMotifExtractor;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.OnlineModel;
import org.nodes.util.Pair;
import org.nodes.util.bootstrap.LogNormalCI;

public class MotifModelTest
{
	public static final int N = 20;

	@Test
	public void testBetaD()
	{
		int iterations = 10;
		DGraph<String> data = Examples.physicians();
		
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(DGraph<String> sub : ex.subgraphs().subList(0, N))
		{
			DGraph<String> subbed = MotifModel.subbedGraph(
					data, ex.occurrences(sub), new ArrayList<List<Integer>>(),
					new HashSet<Integer>());
			
			subbed = Graphs.toSimpleDGraph(subbed);
			
			List<DSequenceEstimator.D> slowDegrees = 
					DSequenceEstimator.sequence(subbed);
			Collections.sort(slowDegrees);
			 
			List<DSequenceEstimator.D> fastDegrees = 
					MotifModel.subbedDegrees(data, ex.occurrences(sub), new FrequencyModel<String>()); 
			Collections.sort(fastDegrees);
			
			assertEquals(slowDegrees, fastDegrees);
			
			double sizeSlow = MotifModelTest.sizeBetaCopying(data, sub, ex.occurrences(sub), true, iterations, 0.05);
			double sizeFast = MotifModel.sizeBeta(data, sub, ex.occurrences(sub), true, iterations, 0.05);
			
			assertEquals(sizeSlow, sizeFast, 50.0);
			
			System.out.print('.');
		}
	}
	
	@Test
	public void testBetaU()
	{
		int iterations = 250;
		UGraph<String> data = RandomGraphs.random(30, 300);
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(UGraph<String> sub : ex.subgraphs().subList(0, N))
		{
			UGraph<String> subbed = MotifModel.subbedGraph(
					data, ex.occurrences(sub), new ArrayList<List<Integer>>(),
					new HashSet<Integer>());
			
			subbed = Graphs.toSimpleUGraph(subbed);
			
			List<Integer> slowDegrees = Graphs.degrees(subbed);
			Collections.sort(slowDegrees);
			 
			List<Integer> fastDegrees = 
					MotifModel.subbedDegrees(data, ex.occurrences(sub), new FrequencyModel<String>()); 
			Collections.sort(fastDegrees);
			
			assertEquals(slowDegrees, fastDegrees);
			
			double sizeSlow = MotifModelTest.sizeBetaCopying(data, sub, ex.occurrences(sub), true, iterations, 0.05);
			double sizeFast = MotifModel.sizeBeta(data, sub, ex.occurrences(sub), true, iterations, 0.05);
			assertEquals(sizeSlow, sizeFast, 3.0);
			
			System.out.print('.');
		}
	}
	
	@Test
	public void testERD()
	{
		DGraph<String> data = Examples.physicians();
		
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(DGraph<String> sub : ex.subgraphs().subList(0, N))
		{
			double sizeSlow = MotifModel.size(data, sub, ex.occurrences(sub), new ERSimpleModel(true), true);
			double sizeFast = MotifModel.sizeER(data, sub, ex.occurrences(sub), true);
			
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
	}
	
	@Test
	public void testERU()
	{
		UGraph<String> data = Examples.jazz();
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(UGraph<String> sub : ex.subgraphs().subList(0,  N))
		{
			System.out.println(sub);
			double sizeSlow = MotifModel.size(data, sub, ex.occurrences(sub), new ERSimpleModel(true), true);
			double sizeFast = MotifModel.sizeER(data, sub, ex.occurrences(sub), true);

			
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
	}
	
	@Test
	public void testELD()
	{
		DGraph<String> data = Examples.physicians();
		
		DPlainMotifExtractor<String> ex = new DPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(DGraph<String> sub : ex.subgraphs().subList(0, N))
		{
			double sizeSlow = MotifModel.size(data, sub, ex.occurrences(sub), new EdgeListModel(true), true);
			double sizeFast = MotifModel.sizeEL(data, sub, ex.occurrences(sub),  true);
			
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
	}
	
	@Test
	public void testELU()
	{
		UGraph<String> data = Examples.jazz();
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 1000, 2, 7, 1);
		
		for(UGraph<String> sub : ex.subgraphs().subList(0, N))
		{
			System.out.println(sub);
			double sizeSlow = MotifModel.size(data, sub, ex.occurrences(sub), new EdgeListModel(true), true);
			double sizeFast = MotifModel.sizeEL(data, sub, ex.occurrences(sub), true);

			
			assertEquals(sizeSlow, sizeFast, 0.000001);
		}
	}

	public static <L> double sizeBetaCopying(Graph<L> graph, Graph<L> sub,
			List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
	{		
		if(graph instanceof DGraph<?>)
			return sizeBetaCopying((DGraph<L>)graph, (DGraph<L>)sub, occurrences, resetWiring, iterations, alpha);
		else
			return sizeBetaCopying((UGraph<L>)graph, (UGraph<L>)sub, occurrences, resetWiring, iterations, alpha);
	}

	public static <L> double sizeBetaCopying(DGraph<L> graph, DGraph<L> sub,
				List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
		{		
			int numThreads = Runtime.getRuntime().availableProcessors();
			
			List<List<Integer>> wiring = new ArrayList<List<Integer>>();
			Set<Integer> motifNodes = new HashSet<Integer>();
			DGraph<L> subbed = MotifModel.subbedGraph(graph, occurrences, wiring, motifNodes);
					
			// * the beta model can only store simple graphs, so we translate subbed
			//   to a simple graph and store the multiple edges separately 
			FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();
			subbed = Graphs.toSimpleDGraph((DGraph<L>)subbed, removals);
					
			// * The estimated cost of storing the structure of the motif and the 
			//   structure of the subbed graph. 
			List<Double> samples = new ArrayList<Double>(iterations);
			DSequenceEstimator<String> motifModel = new DSequenceEstimator<String>(sub);
			DSequenceEstimator<String> subbedModel = new DSequenceEstimator<String>(subbed);
			motifModel.nonuniform(iterations, numThreads);
			subbedModel.nonuniform(iterations, numThreads);
			
			for(int i : series(iterations))
				samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
	
			LogNormalCI ci = new LogNormalCI(samples);
			
			// * The rest of the graph (for which we can compute the code length 
			//   directly) 
			FrequencyModel<String> rest = new FrequencyModel<String>();
			
			// * parameters
			rest.add("sub", DegreeSequenceModel.prior((DGraph<?>)sub, Prior.COMPLETE));
			
			// * size of the subbed graph
			// * degree sequence of subbed
			rest.add("subbed", DegreeSequenceModel.prior((DGraph<?>)subbed, Prior.COMPLETE));
			
			// * Store the labels
			rest.add("labels", log2Choose(occurrences.size(), subbed.size())); 
			
			// * Any node pairs with multiple links
			List<Integer> additions = new ArrayList<Integer>();
			for(Link<L> link : subbed.links())
				if(motifNodes.contains(link.first().index()) || motifNodes.contains((link.second().index())))
				{
					int i = link.first().index(), j = link.second().index();
					
					Pair<Integer, Integer> pair =  Pair.p(i, j);
				
					additions.add((int)removals.frequency(pair));
				}
			
			rest.add("multi-edges", prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
			rest.add("multi-edges", OnlineModel.storeSequence(additions)); 
					
			// * Store the rewiring information
			rest.add("wiring", MotifModel.wiringBits(sub, wiring, resetWiring));
			
			// * Store the insertion order, to preserve the precise ordering of the
			//   nodes in the data 
			rest.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbed.size()));
			
	//		System.out.println("ci : " + ci.upperBound(alpha));
	//		rest.print(System.out);
			
			return ci.upperBound(alpha) + rest.total();
		}

	public static <L> double sizeBetaCopying(UGraph<L> graph, UGraph<L> sub,
			List<List<Integer>> occurrences, boolean resetWiring, int iterations, double alpha)
	{		
		int numThreads = Runtime.getRuntime().availableProcessors();
		
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		Set<Integer> motifNodes = new HashSet<Integer>();
		UGraph<L> subbed = MotifModel.subbedGraph(graph, occurrences, wiring, motifNodes);
				
		// * the beta model can only store simple graphs, so we translate subbed
		//   to a simple graph and store the multiple edges separately 
		FrequencyModel<Pair<Integer, Integer>> removals = new FrequencyModel<Pair<Integer,Integer>>();
		subbed = Graphs.toSimpleUGraph(subbed, removals);
		
		// * The estimated cost of storing the structure of the motif and the 
		//   structure of the subbed graph. 
		List<Double> samples = new ArrayList<Double>(iterations);
		USequenceEstimator<String> motifModel = new USequenceEstimator<String>((UGraph<String>)sub);
		USequenceEstimator<String> subbedModel = new USequenceEstimator<String>((UGraph<String>)subbed);
		motifModel.nonuniform(iterations, numThreads);
		subbedModel.nonuniform(iterations, numThreads);
		
		for(int i : series(iterations))
			samples.add(motifModel.logSamples().get(i) + subbedModel.logSamples().get(i));
		
		LogNormalCI ci = new LogNormalCI(samples);
		
		// * The rest of the graph (for which we can compute the code length 
		//   directly) 
		FrequencyModel<String> rest = new FrequencyModel<String>();
		
		// * parameters
		rest.add("sub", DegreeSequenceModel.prior(sub, Prior.COMPLETE));
		
		// * size of the subbed graph
		// * degree sequence of subbed
		rest.add("subbed", DegreeSequenceModel.prior(subbed, Prior.COMPLETE));
		
		// * Store the labels
		rest.add("labels", log2Choose(occurrences.size(), subbed.size())); 
		
		// * Any node pairs with multiple links
		List<Integer> additions = new ArrayList<Integer>();
		for(Link<L> link : subbed.links())
			if(motifNodes.contains(link.first().index()) || motifNodes.contains((link.second().index())))
			{
				int i = link.first().index(), j = link.second().index();
				
				Pair<Integer, Integer> pair = 
						(graph instanceof DGraph<?>) ? Pair.p(i, j) : Pair.p(min(i,  j), max(i, j));
			
				additions.add((int)removals.frequency(pair));
			}
		
		rest.add("multi-edges", prefix(additions.isEmpty() ? 0 : Functions.max(additions)));
		rest.add("multi-edges", OnlineModel.storeSequence(additions)); 
				
		// * Store the rewiring information
		rest.add("wiring", MotifModel.wiringBits(sub, wiring, resetWiring));
		
		// * Store the insertion order, to preserve the precise ordering of the
		//   nodes in the data 
		rest.add("insertions", log2Factorial(graph.size()) - log2Factorial(subbed.size()));
		
		// System.out.println("ci: " + ci.upperBound(alpha));
		// rest.print(System.out);
		
		return ci.upperBound(alpha) + rest.total();
	}

}
