package org.nodes.rdf;

import static java.util.Collections.reverseOrder;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;
import org.nodes.util.FrequencyModel;
import org.nodes.util.MaxObserver;
import org.nodes.util.Series;

public class HubAvoidance implements Scorer
{
	private List<? extends Node<String>> instances;
	
	private List<FrequencyModel<Node<String>>> counts;
	
	private int maxDepth;
	
	/**
	 * 
	 * @param graph The graph from which to extract instances
	 * @param instances The instance nodes
	 * @param maxDepth The maximum depth to which neighbourhoods will be analysed
	 * @param instanceSize The number of nodes to extract for each instance
	 */
	public HubAvoidance(
			DTGraph<String, String> graph,
			List<? extends Node<String>> instances, 
			int maxDepth)
	{
		this.instances = instances;
		this.maxDepth = maxDepth;
		
		counts = new ArrayList<FrequencyModel<Node<String>>>(maxDepth + 1);
		
		// * set the 0 index to null, so that the indices match up with the 
		//   depths they represent
		counts.add(null);
		
		for(int d : series(1, maxDepth + 1))
			counts.add(new FrequencyModel<Node<String>>());
		
		for(Node<String> instance : instances)
		{
			Set<Node<String>> core = new LinkedHashSet<Node<String>>();
			core.add(instance);
			
			count(1, core);
		}	
	}

	private void count(int depth, Set<Node<String>> core)
	{		
		if(depth > maxDepth)
			return;
		
		Set<Node<String>> shell = new HashSet<Node<String>>();
		
		for(Node<String> node : core)
			for(Node<String> neighbor : node.neighbors())
				if(! core.contains(neighbor))
					shell.add(neighbor);
		
		core.addAll(shell);
				
		for(Node<String> node : core)
			counts.get(depth).add(node);
		
		count(depth + 1, core);
	}
	
	private class EntropyComp implements Comparator<Node<String>>
	{
		private int depth;
		
		public EntropyComp(int depth)
		{
			this.depth = depth;
		}

		@Override
		public int compare(Node<String> first, Node<String> second)
		{
			return Double.compare(bintropy(first, depth), bintropy(second, depth));
		}
	}
	
	private class ProbabilityComp implements Comparator<Node<String>>
	{
		private int depth;
		
		public ProbabilityComp(int depth)
		{
			this.depth = depth;
		}

		@Override
		public int compare(Node<String> first, Node<String> second)
		{
			return Double.compare(p(first, depth), p(second, depth));
		}
	}

	public Comparator<Node<String>> entropyComparator(int depth)
	{
		return new EntropyComp(depth);
	}
	
	public Comparator<Node<String>> probabilityComparator(int depth)
	{
		return new ProbabilityComp(depth);
	}
	
	public double p(Node<String> node, int depth)
	{
		if(depth == 0)
			return 0.0;
		return counts.get(depth).frequency(node) / (double) instances.size();
	}
	
	private double bintropy(Node<String> node, int depth)
	{
		double p = p(node, depth);
		
		return bintropy(p);
	}
	
	private static double bintropy(double p)
	{
		if(p == 0.0 || p == 1.0)
			return 0.0;
		
		double q = 1.0 - p;
		return - (p * log2(p) + q * log2(q));
	}

	@Override
	public double score(Node<String> node, int depth)
	{
		return bintropy(node, depth);
	}
}
