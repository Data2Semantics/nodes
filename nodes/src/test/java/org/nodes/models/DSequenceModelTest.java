package org.nodes.models;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.models.DSequenceEstimator.isGraphical;
import static org.nodes.models.DSequenceEstimator.sequence;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.Series.series;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Global;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.UGraph;
import org.nodes.data.Data;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.bootstrap.LogBCaCI;
import org.nodes.util.bootstrap.LogNormalCI;

public class DSequenceModelTest
{

//	@Test
//	public void test1()
//	{
//		DGraph<?> graph = RandomGraphs.randomDirected(100, 0.1);
//		System.out.println("sampled");
//		
//		DSequenceModelOld<String> model = new DSequenceModelOld<String>(
//				graph, false);
//		
//		System.out.println("Barvinok: the size of the compressed graph is between " 
//				+ model.bitsLowerBound() + " and " + model.bitsUpperBound() + " bits");
//		
//		DSequenceModel<String> sampling = new DSequenceModel<String>(graph, 1000);
//		LogBCaCI bca = new LogBCaCI(sampling.logSamples(), 10000);
//		
//		Pair<Double, Double> ci = bca.twoSided(0.05);
//		System.out.println("Sampling: the size of the compressed graph is between " 
//				+  ci.first() + " and " + ci.second() + " bits");
//
//	}

	@Test
	public void testG1()
	{
		List<Integer> in = Arrays.asList(5, 3, 2, 2, 1, 1, 1);
		List<Integer> g1 = Arrays.asList(3, 2, 1, 0, 0, 1, 0);
		
		assertEquals(g1, DSequenceEstimator.g1(in));
		
		in = Arrays.asList(1, 1, 1, 1, 1, 1, 1);
		g1 = Arrays.asList(6, 1, 0, 0, 0, 0, 0);
		
		assertEquals(g1, DSequenceEstimator.g1(in));
		
		in = Arrays.asList(2, 1, 0);
		g1 = Arrays.asList(0, 0, 0);
		
		assertEquals(g1, DSequenceEstimator.g1(in));
	}
	
	@Test
	public void testS()
	{
		List<Integer> in = Arrays.asList(5, 3, 2, 2, 1, 1, 1);
		List<Integer> s  = Arrays.asList(0, 0, -1, 1, 0, 0, 0);
		
		assertEquals(s, DSequenceEstimator.s(in));
		
		in = asList(5, 2, 2, 2, 2, 2, 2);
		s  = asList(0, -1, 1, 0, 0, 0, 0);
			
		assertEquals(s, DSequenceEstimator.s(in));

		in = asList(5, 5, 5, 5, 5, 5, 5);
		s  = asList(0, 0, 0, 0, -4, 4, 0);
			
		assertEquals(s, DSequenceEstimator.s(in));
	}
	
	@Test
	public void testIsGraphical()
	{
		assertTrue(isGraphical(asList(0, 1, 2), asList(2, 1, 0)));
		
		assertTrue(isGraphical(asList(2, 2, 2), asList(2, 2, 2)));
		
		assertTrue(isGraphical(asList(1, 1, 1), asList(1, 1, 1)));
		
		assertFalse(isGraphical(asList(3, 2, 2), asList(3, 2, 2)));

		assertFalse(isGraphical(asList(8, 4, 4, 4, 0), asList(8, 4, 4, 4, 0)));

		assertFalse(isGraphical(asList(4, 2, 2, 2, 0), asList(4, 2, 2, 2, 0)));

		assertTrue(isGraphical(Arrays.asList(1, 1, 1, 1, 1), Arrays.asList(1, 1, 1, 1, 1)));		
		
		assertTrue(isGraphical(Arrays.asList(5, 0, 1, 0, 1, 0), Arrays.asList(0, 2, 1, 2, 1, 1)));		

	}
	
	
	@Test
	public void testGenerate()
	{
		Global.random().setSeed(1);
		DSequenceEstimator<String> model;
		
		model = new DSequenceEstimator<String>(asList(1, 1, 1, 1), asList(1, 1, 1, 1));

		for(int i : series(100000))
			model.nonuniform();
		
		assertEquals(Functions.log2(9.0), model.logNumGraphsNaive(), 0.001);
		
		model = new DSequenceEstimator<String>(asList(1,  1, 1), asList(1, 1, 1));

		for(int i : series(100))
			model.nonuniform();
		
		assertEquals(1.0, model.logNumGraphsNaive(), 0.0);
		
		model = new DSequenceEstimator<String>(asList(2,  1, 0), asList(0, 1, 2));
		
		for(int i : series(100))
				model.nonuniform();
		
		assertEquals(0.0, model.logNumGraphsNaive(), 0.0);
	}
	
	
	@Test
	public void testGenerateBig()
	{
		DGraph<?> graph = RandomGraphs.randomDirected(7115, 0.00205);
		System.out.println("generated");
		
		DSequenceEstimator<String> model = new DSequenceEstimator<String>(graph);
		Functions.tic();
		model.nonuniform();
		System.out.println("Finished sample. Time taken: " + Functions.toc() + " seconds.");
	}
	
	@Test
	public void random()
	{	
		for(int seed : series(100))
		{
			if(seed % 10 == 0)
				System.out.print('.');
			
			Global.random().setSeed(seed);
			
			int n = Global.random().nextInt(25) + 5;
			double p = Global.random().nextDouble() * 0.9 + 0.1;
			
			DGraph<?> graph = RandomGraphs.randomDirected(n, p);
			
			DSequenceEstimator<String> model = new DSequenceEstimator<String>(graph);
			for(int i : series(100))
			{
				DGraph<String> gen = model.nonuniform().graph();
			
				assertEquals(sequence(graph), sequence(gen));
			}
		}
	}
	
	
	@Test
	public void testG()
	{
		assertEquals(0, DSequenceEstimator.g(1, 0, asList(2, 2, 2)));
	}
	
	@Test
	public void testTime()
	{	
		int n = 150;
		List<Pair<Double, Double>> values = new ArrayList<Pair<Double,Double>>();
		
		for(int i : series(n))
		{
			int size = Global.random().nextInt(400) + 100;
			int numLinks = Global.random().nextInt(2000) + 500;
			
			UGraph<String> graph;
			try {
				graph = RandomGraphs.random(size, numLinks);
			} catch(Exception e)
			{
				continue;
			}
			
			tic();
			USequenceEstimator<String> model = new USequenceEstimator<String>(graph);
			model.nonuniform();
			double time = toc();
			
			System.out.println(((double)size*numLinks) + "\t" +  time);
			
		}
		
	}
	
	@Test
	public void testTime2() throws IOException
	{
		DGraph<String> graph = Data.edgeListDirectedUnlabeled(new File("/Users/Peter/Documents/datasets/graphs/facebook/facebook.txt"), true);
		// System.out.println("expected : " + (graph.size() * (double)graph.numLinks() / (double)100000) + " seconds");

		Functions.tic();
		USequenceEstimator<String> model = new USequenceEstimator<String>(graph, 10);
		LogNormalCI ci = new LogNormalCI(model.logSamples(), 20000);

		System.out.println(ci.twoSided(0.05));
	}
}	
