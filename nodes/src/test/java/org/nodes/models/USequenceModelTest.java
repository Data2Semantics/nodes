package org.nodes.models;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.models.USequenceModel.isGraphical;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.Node;
import org.nodes.random.RandomGraphs;
import org.nodes.util.Functions;
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

	}
	
	@Test
	public void testIsGraphicalSpeed()
	{
		Graph<?> graph = org.nodes.random.RandomGraphs.random(10000, 50000);
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		for(Node<?> node : graph.nodes())
			sequence.add(node.degree());
	
		for(int i : series(100))
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
		Graph<String> graph = RandomGraphs.preferentialAttachment(66, 2);
		System.out.println("sampled");
		
		tic();
		USequenceModel<String> model = new USequenceModel<String>(graph, 6000);
		System.out.println("Sampling completed in " + toc() + " seconds.");

		System.out.println("number of graphs of size " + model.numGraphs() + " +/- " + Math.pow(2.0, model.logStdError()));
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
	
}

