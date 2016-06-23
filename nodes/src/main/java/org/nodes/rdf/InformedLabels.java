package org.nodes.rdf;

import static java.util.Collections.reverseOrder;
import static nl.peterbloem.kit.Functions.log2;
import static nl.peterbloem.kit.Series.series;

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
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Node;
import org.nodes.classification.Classified;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.MaxObserver;
import nl.peterbloem.kit.Series;


/**
 * Version of HubAvoidance which takes instance labels into account
 * @author Peter
 *
 */
public class InformedLabels implements Scorer
{
	private List<? extends Node<String>> instances;
	
	// * Marginal counts
	// 		The outer lists collects a frequencymodel for each depth d
	//		The frequency model counts for each node in how d-neighborhoods of 
	//		instances it occurs.
	private List<FrequencyModel<String>> counts;
	
	// * Counts conditional on class
	//	 the first list collects by class, the rest the same as counts
	private List<List<FrequencyModel<String>>> classCounts;
	
	private FrequencyModel<Integer> classes;
	
	private int numClasses, maxDepth;
	
	/**
	 * 
	 * @param graph The graph from which to extract instances
	 * @param instances The instance nodes
	 * @param maxDepth The maximum depth to which neighbourhoods will be analysed
	 * @param instanceSize The number of nodes to extract for each instance
	 */
	public InformedLabels(
			DTGraph<String, String> graph,
			Classified<? extends Node<String>> instances, 
			int maxDepth)
	{
		this.instances = instances;
		this.maxDepth = maxDepth;
		
		int numInstances = instances.size();
		numClasses = instances.numClasses();
		classes = new FrequencyModel<Integer>(instances.classes());
		
		// ** Intitialize the counts object
		counts = new ArrayList<FrequencyModel<String>>(maxDepth + 1);
		// * set the 0 index to null, so that the indices match up with the 
		//   depths they represent
		counts.add(null);
		
		for(int d : series(1, maxDepth + 1))
			counts.add(new FrequencyModel<String>());
		
		
		// ** Initialize the classCounts object
		classCounts = new ArrayList<List<FrequencyModel<String>>>();
		for(int i : series(numClasses))
		{
			ArrayList<FrequencyModel<String>> classCount = new ArrayList<FrequencyModel<String>>(maxDepth + 1);  
					
			// * set the 0 index to null, so that the indices match up with the 
			//   depths they represent
			classCount.add(null);
			
			for(int d : series(1, maxDepth + 1))
				classCount.add(new FrequencyModel<String>());
	
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
					
		for(Node<String> node : core)
		{
			for(String label : labels((DTNode<String, String>) node))
			{
				counts.get(depth).add(label); // marginal over all classes
				classCounts.get(cls).get(depth).add(label); // conditional by class
			}
		}
		
		count(depth + 1, core, cls);
	}
		
	private class InformedComp implements Comparator<String>
	{
		private int depth;
		
		public InformedComp(int depth)
		{
			this.depth = depth;
		}

		@Override
		public int compare(String first, String second)
		{
			return Double.compare(classEntropy(first, depth), classEntropy(second, depth));
		}
	}
	
	private class UninformedComp implements Comparator<String>
	{
		private int depth;
		
		public UninformedComp(int depth)
		{
			this.depth = depth;
		}

		@Override
		public int compare(String first, String second)
		{
			return Double.compare(bintropy(first, depth), bintropy(second, depth));
		}
	}

	public Comparator<String> informedComparator(int depth)
	{
		return new InformedComp(depth);
	}
	
	public Comparator<String> uninformedComparator(int depth)
	{
		return new UninformedComp(depth);
	}
	
	private double bintropy(String node, int depth)
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
	
	/**
	 * Non-Viable nodes should be filtered out of any list of potential nodes to 
	 * consider for processing
	 *  
	 * @param node
	 * @return
	 */
	public boolean viableHub(Node<String> node, int depth, int numInstances)
	{
		if(counts.get(depth).frequency(node.label()) < numInstances)
			return false;
		
		return true;
	}
	
	/**
	 * The probability that a random instance is in the depth-neighbourhood of 
	 * the given node
	 *  
	 * @param node
	 * @param depth
	 * @return
	 */
	public double p(String label, int depth)
	{
		if(depth == 0)
			return 0.0;
		
		return counts.get(depth).frequency(label) / (double) instances.size();
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
	public double p(String label, int cls, int depth)
	{
		List<FrequencyModel<String>> cns = classCounts.get(cls);
		
		if(depth == 0)
			return 0.0;
		
		return cns.get(depth).frequency(label) / classes.frequency(cls);
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
	public double pClass(int cls, String label, int depth)
	{
		double div = p(label, depth);
		
		if(div == 0.0)
			return 0.0;
		
		return p(label, cls, depth) * p(cls) / div;
	}

	public double classEntropy(String label, int depth)
	{
		double entropy = 0.0;
		
		for(int cls : classes.tokens())
		{
			double p = pClass(cls, label, depth);
			
			entropy += p == 0.0 ? 0.0 : p * log2(p);
		}
			
		return - entropy;
	}
	
	@Override
	public double score(Node<String> node, int depth)
	{
		return - classEntropy(node.label(), depth);
	}
	
	
	/**
	 * All the labels this node could take.
	 * 
	 * @param node
	 * @return
	 */
	public List<String> labels(DTNode<String, String> node)
	{
		List<String> labels = new ArrayList<String>(node.degree());
		labels.add(node.label());
		
		for(DTLink<String, String> link : node.linksOut())
		{
			if(link.to().equals(link.from()))
				continue;
			
			String label = "out: ";
			label += link.tag() + " ";
			label += link.other(node) + " ";
			
			labels.add(label);
		}

		for(DTLink<String, String> link : node.linksIn())
		{
			if(link.to().equals(link.from()))
				continue;

			String label = " in: ";
			label += link.tag() + " ";
			label += link.other(node);
			
			labels.add(label);
		}

		return labels;
	}
	
	public String chooseLabelInformed(DTNode<String, String> node, int depth)
	{
		return choose(labels(node), Collections.reverseOrder(informedComparator(depth)));
	}
	
	public String chooseLabelUninformed(DTNode<String, String> node, int depth)
	{		
		return choose(labels(node), uninformedComparator(depth));
	}
	
	public String choose(List<String> labels, Comparator<String> comp)
	{
		MaxObserver<String> mo = new MaxObserver<String>(1, comp);
		
		mo.observe(labels);
		
		return mo.elements().get(0);
	}

}
