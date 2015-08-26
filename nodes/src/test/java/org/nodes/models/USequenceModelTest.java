package org.nodes.models;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.models.USequenceModel.findMaxFailDegree;
import static org.nodes.models.USequenceModel.isGraphical;
import static org.nodes.models.USequenceModel.numZeroes;
import static org.nodes.util.Functions.exp2;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.LogNumTest.l;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.distribution.TDistribution;
import org.junit.Test;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;
import org.nodes.util.LogNum;
import org.nodes.util.LogNumTest;
import org.nodes.util.Pair;
import org.nodes.util.Series;

public class USequenceModelTest
{

	@Test
	public void testIsGraphical()
	{
		testIsGraphical(Graphs.jbc());
		
		for(int i : series(50))
			testIsGraphical(RandomGraphs.random(100, 500));
		
		assertFalse(isGraphical(Arrays.asList(5, 3, 3, 3, 1, 1, 1))); // odd sum
		
		assertFalse(isGraphical(Arrays.asList(1, 2, 3, 4, 5, 6, 7))); // even sum, each degree occurs only once
		
		assertTrue(isGraphical(Arrays.asList(3, 2, 2, 2, 1)));
		assertTrue(isGraphical(Arrays.asList(3, 1, 2, 2, 0)));
		assertTrue(isGraphical(Arrays.asList(2, 0, 2, 2, 0)));
		assertTrue(isGraphical(Arrays.asList(1, 0, 2, 1, 0)));
		assertTrue(isGraphical(Arrays.asList(0, 0, 1, 1, 0)));
		assertTrue(isGraphical(Arrays.asList(0, 0, 0, 0, 0)));
		
		assertFalse(isGraphical(Arrays.asList(4, 2, 2, 2, 0)));
		assertFalse(isGraphical(Arrays.asList(4, 4, 2, 2, 1)));
		assertFalse(isGraphical(Arrays.asList(1, 1, 1, 1, 1)));
		
		assertFalse(isGraphical(Arrays.asList(3, 3, 1, 1)));
		assertFalse(isGraphical(Arrays.asList(3, 3, 1, 1, 0, 0)));

		assertFalse(isGraphical(Arrays.asList(5, 5, 1, 1, 1, 1, 1, 1)));
		assertFalse(isGraphical(Arrays.asList(5, 5, 1, 1, 1, 1, 1, 1, 0, 0)));

		assertFalse(isGraphical(Arrays.asList(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
		
	}
	
	@Test
	public void testNumZeroes()
	{
		assertEquals(1, numZeroes(asList(4, 2, 2, 2, 0)));
		assertEquals(2, numZeroes(asList(4, 2, 2, 2, 0, 0)));
		assertEquals(5, numZeroes(asList(0, 0, 0, 0, 0)));
	}
	
	
	@Test
	public void testIsGraphicalSpeed()
	{
		Graph<?> graph = org.nodes.random.RandomGraphs.random(1000, 5000);
		
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		for(Node<?> node : graph.nodes())
			sequence.add(node.degree());
	
		for(int i : series(5000))
			isGraphical(sequence);
	}
	
	public <L> void testIsGraphical(Graph<L> graph)
	{
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		for(Node<L> node : graph.nodes())
			sequence.add(node.degree());
		
		assertTrue(isGraphical(sequence));
	}

	@Test
	public void testGenerate()
	{
		Graph<String> graph = Graphs.jbc();
		
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		for(Node<String> node : graph.nodes())
			sequence.add(node.degree());
		
		
		USequenceModel<String> model = new USequenceModel<String>(graph, 500);
		System.out.println(model.nonuniform().graph());
		
	}
	
	@Test
	public void testGenerateX()
	{
		USequenceModel<String> model = new USequenceModel<String>(asList(1, 2, 2, 1));
		
		for(int i : series(1000))
			model.nonuniform();
		
		assertEquals(1.0, model.logNumGraphs(), 0.0);
	}
	
	@Test
	public void testGenerateSpeed()
	{
		Graph<String> graph = RandomGraphs.random(5, 7);
				
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		for(Node<String> node : graph.nodes())
			sequence.add(node.degree());
		
		System.out.println(sequence);
		
		USequenceModel<String> model = new USequenceModel<String>(graph, 500);
		for(int i : series(500))
			System.out.println(model.nonuniform().graph());
		
	}
	
	@Test
	public void testFoodWeb()
	{
		List<Integer> sequence = asList(7, 8, 5, 1, 1, 2, 8, 10, 4, 2, 4, 5, 3, 
			6, 7, 3, 2, 7, 6, 1, 2, 9, 6, 1, 3, 4, 6, 3, 3, 3, 2, 4, 4);
		
		System.out.println(USequenceModel.sum(sequence));

		tic();
		USequenceModel<String> model = new USequenceModel<String>(sequence, 6000);
		System.out.println("Sampling completed in " + toc() + " seconds.");
		
		System.out.println("logprob    :" + model.logProb());
		System.out.println("num graphs :" + model.numGraphs());
		
		assertEquals(1.51E57, model.numGraphs(), 0.06E57);
	}
	
	@Test
	public void testBig()
	{
		Graph<String> graph = RandomGraphs.random(10000, 100000);
		System.out.println("sampled");
		
		tic();
		USequenceModel<String> model = new USequenceModel<String>(graph);
		for(int i : series(5))
		{
			model.nonuniform();
			System.out.println(" " + toc() + " seconds.");
			tic();
		}
		
			
		System.out.println(model.logSamples());
		
		for(double y : model.logSamples())
			System.out.println(Functions.exp2(y));

		System.out.println("Sampling completed in " + toc() + " seconds.");

//		System.out.println("log std dev " + model.logStdDev());
//		System.out.println("std dev " + exp2(model.logStdDev()));
//		
//		System.out.println("log std error " + model.logStdError());
//		System.out.println("std error " + exp2(model.logStdError()));
//		
//		System.out.println("std error " + exp2(model.logStdDev()) / sqrt(10) );
		
		System.out.println("log number of graphs " + model.logNumGraphs());
		
		double alpha = 0.05;
		
		Pair<Double, Double> ci;
		ci = model.confidence(alpha, USequenceModel.CIType.TWO_SIDED);
		System.out.println("standard:   [" + ci.first() + ", " + ci.second() + "]");
		
		ci = model.confidence(alpha, USequenceModel.CIMethod.LOG_NORMAL, USequenceModel.CIType.TWO_SIDED);
		System.out.println("lognorm:    [" + ci.first() + ", " + ci.second() + "]");

		ci = model.confidence(alpha, USequenceModel.CIMethod.PERCENTILE, USequenceModel.CIType.TWO_SIDED);
		System.out.println("percentile: [" + ci.first() + ", " + ci.second() + "]");
		
		ci = model.confidence(alpha, USequenceModel.CIMethod.BCA, USequenceModel.CIType.TWO_SIDED);
		System.out.println("bca:        [" + ci.first() + ", " + ci.second() + "]");
		
		
		System.out.println("effective sample size " + model.effectiveSampleSize());

	}
	
	
	@Test
	public void test3Regular()
	{
		test3Regular(6,  70,       2.4,      500);
		test3Regular(8,  19355,    730,      500);
		test3Regular(10, 1.118E7,  0.042E7,  500);
		test3Regular(12, 1.156E10, 0.044E10, 500);
		test3Regular(14, 1.914E13, 0.072E13, 500);
                               
		test3Regular(16, 5.122E16, 0.186E16, 500);
		test3Regular(18, 1.893E20, 0.068E20, 500);
		test3Regular(20, 9.674E23, 0.34E23,  500);
		test3Regular(22, 6.842E27, 0.24E27,  500);
		test3Regular(24, 6.411E31, 0.22E31,  500);
	}
	
	public void test3Regular(int size, double expected, double delta, int samples)
	{
		List<Integer> sequence = new ArrayList<Integer>(size);
		for(int i : series(size))
			sequence.add(3);

		tic();
		USequenceModel<String> model = new USequenceModel<String>(sequence, samples);
		System.out.println("Sampling completed in " + toc() + " seconds.");

		
		System.out.println("number of 3-regular graphs of size " + size + ":" + model.numGraphs() + " " + Math.pow(2.0, model.logStdError()));
		System.out.println("effective sample size " + model.effectiveSampleSize());
		
		// assertEquals(expected, model.numGraphs(), delta);
	}
	
	@Test
	public void irregular()
	{
		List<Integer> sequence = Arrays.asList(6, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
		
		tic();
		USequenceModel<String> model = new USequenceModel<String>(sequence, 500);
		System.out.println("Sampling completed in " + toc() + " seconds.");

		System.out.println("number:" + model.numGraphs());
		assertEquals(7392, model.numGraphs(), 1000);
	}
	
	@Test
	public void testGenerate2()
	{
		USequenceModel<String> model = new USequenceModel<String>(Arrays.asList(3,2,2,2,1), 500);
		System.out.println(model.nonuniform().graph());
	}
	
	@Test 
	public void testFindXks()
	{	
		assertEquals(asList(11, 7, 4), USequenceModel.findxks(asList(5,5,4,3, 2, 2, 2, 1, 1, 1, 1)));
		
		assertEquals(asList(4, 2), USequenceModel.findxks(asList(3, 3, 1, 1)));
		assertEquals(asList(8, 2), USequenceModel.findxks(asList(5, 5, 1, 1, 1, 1, 1, 1)));
	}

	@Test 
	public void testFindFailDegree()
	{
		List<USequenceModel.Index> seq = new ArrayList<USequenceModel.Index>();
		seq.add(new USequenceModel.Index(2, false));
		seq.add(new USequenceModel.Index(2, false));
		seq.add(new USequenceModel.Index(2, false));
		seq.add(new USequenceModel.Index(1, false));
		seq.add(new USequenceModel.Index(1, false));
		
		assertEquals(0, findMaxFailDegree(seq));
	}
	
	@Test 
	public void testFindAcceptableSet()
	{
		List<Integer> seq; 
		Graph<String> graph = new MapUTGraph<String, String>();
		
		Node<String> a = graph.add("a");
		Node<String> b = graph.add("b");
		Node<String> c = graph.add("c");
		Node<String> d = graph.add("d");
		Node<String> e = graph.add("e");
		
		Node<String> hub = graph.get(0);

		List<Integer> expected;
		
		seq = asList(4, 3, 3, 2, 2);
		
		expected = asList(1, 2, 3, 4);
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		a.connect(c);
		seq = asList(3, 3, 2, 2, 2);
		
		expected = asList(1, 3, 4);
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		a.connect(d);
		seq = asList(2, 3, 2, 1, 2);
		
		expected = asList(1, 4);
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		a.connect(e);
		seq = asList(1, 3, 2, 1, 1);
		
		expected = asList(1);
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		a.connect(b);
		
		seq = asList(0, 2, 2, 1, 1);
		
		hub = b;
		
		expected = asList(2, 3, 4);
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		b.connect(e);
		seq = asList(0, 1, 2, 1, 0);
		
		expected = asList(2); // NOTE: d is no longer acceptable
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));
		
		b.connect(c);
		seq = asList(0, 0, 1, 1, 0);
		
		hub = c;
		
		expected = asList(3); 
		assertEquals(expected, USequenceModel.findAcceptableSet(seq, graph, hub));

	}
	
	@Test
	public void quick()
	{
		for(int i : series(1, 1000))
		{
			TDistribution dist = new TDistribution(i);
		
			System.out.println(dist.inverseCumulativeProbability(0.025));
		}
	}
	
	@Test
	public void computeATest()
	{
		double a, b, c;
		
		a = 1.0; b =10.0; c = 88.0;
		LogNum exp = LogNum.fromDouble(computeATest(a, b, c), 2.0);
		LogNum actual = USequenceModel.computeA(Arrays.asList(l(a), l(b), l(c)));
		actual.toBase(2.0);
		
		assertEquals(exp.logMag(), actual.logMag(), 0.0000001);
		assertEquals(exp.positive(), actual.positive());
		
		a = 1.0; b =1.0; c = 1.0;
		exp = LogNum.fromDouble(computeATest(a, b, c), 2.0);
		actual = USequenceModel.computeA(Arrays.asList(l(a), l(b), l(c)));
		actual.toBase(2.0);
		
		assertEquals(exp.logMag(), actual.logMag(), 0.0000001);
		assertEquals(exp.positive(), actual.positive());
		
		a = -1.0; b = 0.0; c = 1.0;
		exp = LogNum.fromDouble(computeATest(a, b, c), 2.0);
		actual = USequenceModel.computeA(Arrays.asList(l(a), l(b), l(c)));
		actual.toBase(2.0);
				
		assertEquals(exp.logMag(), actual.logMag(), 0.0000001);
		assertEquals(exp.positive(), actual.positive());
	}
	
	public double computeATest(double a, double b, double c)
	{
		double mean = (a + b + c)/3.0;
		double da = mean - a, db = mean - b, dc = mean - c;
		
		double num = da*da*da + db*db*db + dc*dc*dc;
		double den = da*da + db*db + dc*dc;
		
		den = Math.sqrt(den);
		den = den * den * den;
		if(den == 0.0)
			return 0.0;
		
		return num / (6.0 * den);
	}
}

