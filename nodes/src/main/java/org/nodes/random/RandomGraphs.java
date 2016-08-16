package org.nodes.random;

import static java.lang.Math.log;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.nodes.DTGraph;
import org.nodes.Graph;
import org.nodes.LightUGraph;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.UGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Generator;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;


/**
 * 
 * TODO: This requires an UndirectedGraph implementation.
 * @author Peter
 *
 */
public class RandomGraphs
{
	public static final int BA_INITIAL = 3; 
	
	public static UTGraph<String, String> preferentialAttachment(int nodes, int toAttach)
	{
		BAGenerator bag = new BAGenerator(BA_INITIAL, toAttach);
		bag.iterate((nodes - BA_INITIAL));
		
		return bag.graph();
	}
	
	public static DTGraph<String, String> preferentialAttachmentDirected(int nodes, int toAttach)
	{
		DBAGenerator bag = new DBAGenerator(BA_INITIAL, toAttach);
		bag.iterate((nodes - BA_INITIAL));
		
		return bag.graph();
	}
	
	public static UTGraph<String, String> random(int n, double prob)
	{
		MapUTGraph<String, String> graph = new MapUTGraph<String, String>();
		List<UTNode<String, String>> nodes = new ArrayList<UTNode<String, String>>(n);

		for(int i : series(n))
			nodes.add(graph.add("x"));
		
		for(int i : series(n))
			for(int j : series(i+1, n))
				if(Global.random().nextDouble() < prob)
					nodes.get(i).connect(nodes.get(j));
		
		return graph;
	}

	/**
	 * Makes a uniform random selection from the set of all graphs with exactly
	 * n nodes and m links. Nodes will not connect to themselves
	 * 
	 * @param n
	 * @param m
	 * @return
	 */
	public static UTGraph<String, String> random(int n, int m)
	{		
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
		for(int i : series(n))
			graph.add("x");
		
		long ln = (long)n;
		long total = (ln*ln - ln) / (long)2;
		System.out.println(".");
		List<Long> indices = Functions.sample(m, total);
		System.out.println("x");
		
		for(long index : indices)
		{
			Pair<Integer, Integer> ij = toPairUndirected(index, false);
			graph.nodes().get(ij.first()).connect(graph.nodes().get(ij.second()));
		}
		
		return graph;
	}
	
	public static UGraph<String> randomFast(int n, int m)
	{		
		UGraph<String> graph = new LightUGraph<String>();
		for(int i : series(n))
			graph.add("x");
		
		long ln = (long)n;
		long total = (ln*ln - ln) / (long)2;
		List<Long> indices = Functions.sample(m, total);
		
		for(long index : indices)
		{
			Pair<Integer, Integer> ij = toPairUndirected(index, false);
			graph.nodes().get(ij.first()).connect(graph.nodes().get(ij.second()));
		}
		
		return graph;
	}
	
	/**
	 * No self-loops
	 * @param n
	 * @param prob
	 * @return
	 */
	public static DTGraph<String, String> randomDirected(int n, double prob)
	{
		MapDTGraph<String, String> graph = new MapDTGraph<String, String>();

		for(int i : series(n))
			graph.add("x");
		
		for(int i : series(n))
			for(int j : series(n))
				if(i != j && Global.random().nextDouble() < prob)
					graph.get(i).connect(graph.get(j));
		
		return graph;
	}
	
	public static UTGraph<String, String> fractal(
			int depth, int offspring, int interLinks, double hubProb)
	{
		FractalGenerator gen = new FractalGenerator(offspring, interLinks, hubProb);
		
		for(int i : series(depth))
			gen.iterate();
		
		return gen.graph();
	}
	
	/**
	 * NB: may overflow if the resulting indices are too big. 
	 * 
	 * @param index
	 * @param self
	 * @return
	 */
	public static Pair<Integer, Integer> toPairUndirected(long index, boolean self)
	{
		double iDouble;
		long i, j;
		
		if(self)
		{
			iDouble = - 0.5 + 0.5 * Math.sqrt(1.0 + 8.0 * index);
			i = (int) Math.floor(iDouble);
			j = index - ((i * (i + 1)) / 2);	
		} else {
			iDouble = 0.5 + 0.5 * Math.sqrt(1.0 + 8.0 * index);
			i = (int) Math.floor(iDouble);
			j = index - ((i * (i - 1)) / 2);	
		}
		
		return new Pair<Integer, Integer>((int)i, (int)j);
	}
}
