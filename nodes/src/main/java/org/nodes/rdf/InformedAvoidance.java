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
import org.nodes.classification.Classified;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.MaxObserver;
import org.nodes.util.Series;


/**
 * Version of HubAvoidance which takes instance labels into account
 * @author Peter
 *
 */
public class InformedAvoidance implements Scorer
{
	private List<Node<String>> instances;
	
	// * Marginal counts
	// 		The outer lists collects a frequencymodel for each depth d
	//		The frequency model counts for each node in how d-neighborhoods of 
	//		instances it occurs.
	private List<FrequencyModel<Node<String>>> counts;
	
	// * Counts conditional on class
	//	 the first list collects by class, the rest the same as counts
	private List<List<FrequencyModel<Node<String>>>> classCounts;
	
	private FrequencyModel<Integer> classes;
	
	private int numClasses, maxDepth;
	
	/**
	 * 
	 * @param graph The graph from which to extract instances
	 * @param instances The instance nodes
	 * @param maxDepth The maximum depth to which neighbourhoods will be analysed
	 * @param instanceSize The number of nodes to extract for each instance
	 */
	public InformedAvoidance(
			DTGraph<String, String> graph,
			Classified<Node<String>> instances, 
			int maxDepth)
	{
		this.instances = instances;
		this.maxDepth = maxDepth;
		
		int numInstances = instances.size();
		numClasses = instances.numClasses();
		classes = new FrequencyModel<Integer>(instances.classes());
		
		// ** Intitialize the counts object
		counts = new ArrayList<FrequencyModel<Node<String>>>(maxDepth + 1);
		// * set the 0 index to null, so that the indices match up with the 
		//   depths they represent
		counts.add(null);
		
		for(int d : series(1, maxDepth + 1))
			counts.add(new FrequencyModel<Node<String>>());
		
		
		// ** Initialize the classCounts object
		classCounts = new ArrayList<List<FrequencyModel<Node<String>>>>();
		for(int i : series(numClasses))
		{
			ArrayList<FrequencyModel<Node<String>>> classCount = new ArrayList<FrequencyModel<Node<String>>>(maxDepth + 1);  
					
			// * set the 0 index to null, so that the indices match up with the 
			//   depths they represent
			classCount.add(null);
			
			for(int d : series(1, maxDepth + 1))
				classCount.add(new FrequencyModel<Node<String>>());
	
			classCounts.add(classCount);
		}
		
		for(int i : series(instances.size()))
		{
			Node<String> instance = instances.get(i);
			int cls = instances.cls(i);
			
			Set<Node<String>> core = new LinkedHashSet<Node<String>>();
			core.add(instance);
			
			count(1, core, cls);
		}
	}
		
	private void count(int depth, Set<Node<String>> core, int cls)
	{		
		if(depth > maxDepth)
			return;
		
		Set<Node<String>> shell = new HashSet<Node<String>>();
		
		for(Node<String> node : core)
			for(Node<String> neighbor : node.neighbors())
				if(! core.contains(neighbor))
					shell.add(neighbor);
		
		core.addAll(shell);
				
		List<FrequencyModel<Node<String>>> cns;
		
		for(Node<String> node : core)
		{
			counts.get(depth).add(node); // marginal over all classes
			classCounts.get(cls).get(depth).add(node); // conditional by class
		}
		
		count(depth + 1, core, cls);
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
			return Double.compare(classEntropy(first, depth), classEntropy(second, depth));
		}
	}

	public Comparator<Node<String>> entropyComparator(int depth)
	{
		return new EntropyComp(depth);
	}
	
	/**
	 * The probability that a random instance is in the depth-neighbourhood of 
	 * the given node
	 *  
	 * @param node
	 * @param depth
	 * @return
	 */
	public double p(Node<String> node, int depth)
	{
		if(depth == 0)
			return 0.0;
		
		return counts.get(depth).frequency(node) / (double) instances.size();
	}
	
	/**
	 * The probability that a random instance of the given class is in the 
	 * depth-neighborhood of the given node.
	 * 
	 * @param node
	 * @param depth
	 * @param cls
	 * @return
	 */
	public double p(Node<String> node, int cls, int depth)
	{
		List<FrequencyModel<Node<String>>> cns = classCounts.get(cls);
		
		if(depth == 0)
			return 0.0;
		
		return cns.get(depth).frequency(node) / classes.frequency(cls);
	}
	
	/**
	 * The prior probability of a given class
	 * @param cls
	 * @return
	 */
	public double p(int cls)
	{
		return classes.probability(cls);
	}
	
	/**
	 * The probability of a class for a given node
	 * 
	 * It is the entropy over this distribution that we want minimized
	 * @return
	 */
	public double pClass(int cls, Node<String> node, int depth)
	{
		double div = p(node, depth);
		
		if(div == 0.0)
			return 0.0;
		
		return p(node, cls, depth) * p(cls) / div;
	}

	public double classEntropy(Node<String>node, int depth)
	{
		double entropy = 0.0;
		
		for(int cls : classes.tokens())
		{
			double p = pClass(cls, node, depth);
			
			entropy += p == 0.0 ? 0.0 : p * log2(p);
		}
			
		return - entropy;
	}
	
	@Override
	public double score(Node<String> node, int depth)
	{
		return - classEntropy(node, depth);
	}
}
