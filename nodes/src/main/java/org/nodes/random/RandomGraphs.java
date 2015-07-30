package org.nodes.random;

import static java.lang.Math.log;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.nodes.Global;
import org.nodes.util.Generator;
import org.nodes.DTGraph;
import org.nodes.Graph;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;
import org.nodes.util.Functions;
import org.nodes.util.Pair;


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
		
		List<Integer> indices = Functions.sample(m, (n*n - n)/2);
		
		for(int index : indices)
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
	
	public static Pair<Integer, Integer> toPairUndirected(int index, boolean self)
	{
		double iDouble;
		int i, j;
		
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
		
		return new Pair<Integer, Integer>(i, j);
	}
}
