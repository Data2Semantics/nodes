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

public class HubAvoidance implements Instances
{
	private DTGraph<String, String> graph;
	private List<Node<String>> instances;

	private int maxDepth, instanceSize, beamWidth;
	
	private List<FrequencyModel<Node<String>>> counts;
	
	
	/**
	 * 
	 * @param graph The graph from which to extract instances
	 * @param instances The instance nodes
	 * @param maxDepth The maximum depth to which neighbourhoods will be analysed
	 * @param instanceSize The number of nodes to extract for each instance
	 */
	public HubAvoidance(
			DTGraph<String, String> graph,
			List<Node<String>> instances, 
			int maxDepth, 
			int instanceSize,
			int beamWidth)
	{
		super();
		this.graph = graph;
		this.instances = instances;
		this.maxDepth = maxDepth;
		this.instanceSize = instanceSize;
		this.beamWidth = beamWidth;
		
		int numInstances = instances.size();
		
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
	public List<DTNode<String, String>> instance(Node<String> instanceNode)
	{
		if(graph != instanceNode.graph())
			throw new IllegalArgumentException("This Instance extractor was created with a diffeent graph than the given node belongs to.");
			
		return new Search((DTNode<String, String>)instanceNode).result();
	}
	
	/**
	 * We wrap the search process in an object to ensure thread safety
	 * @author Peter
	 *
	 */
	private class Search
	{
		private State root;
		private MaxObserver<State> mo = new MaxObserver<State>(1);
		
		private LinkedList<State> buffer = new LinkedList<State>();
		
		public Search(DTNode<String, String> node)
		{
			root = new State(new LinkedHashSet<Token>(), new Token(node, 0));
			buffer.add(root);
			
			while(! buffer.isEmpty())
			{
				State current = buffer.pop();
				mo.observe(current);
				
				for(State child : current)
					buffer.add(child);	
				
				Collections.sort(buffer, reverseOrder()); // sort with largest first
				
				// * Trim buffer back to beam width
				while(buffer.size() > beamWidth)
					buffer.pollLast();
				}
		}
		
		public List<DTNode<String, String>> result()
		{
			State best = mo.elements().get(0);
			
			List<DTNode<String, String>> nodes = 
					new ArrayList<DTNode<String,String>>(best.selection().size());
			
			for(Token token : best.selection())
			{
				System.out.println("     " + p(token.node(), token.depth()) + " " + token.node());
				nodes.add(token.node());
			}
				
			return nodes;
		}
	}
	
	private class State implements Iterable<State>, Comparable<State>
	{
		Set<Token> selection;
		Set<Node<String>> nodes = new LinkedHashSet<Node<String>>();
		
		public State(Set<Token> parentSelection, Token addition)
		{
			selection = new LinkedHashSet<Token>(parentSelection);
			selection.add(addition);
			
			for(Token token : selection)
				nodes.add(token.node());
		}
		
		
		public double score()
		{	
			double score = 0.0;
			
			for(Token token : selection)
				score += token.score();
			
			return score;
		}
		
		public Iterator<State> iterator()
		{
			if(selection.size() >= instanceSize)
			{
				List<HubAvoidance.State> empty = new ArrayList<HubAvoidance.State>();
				return empty.iterator();
			}
			
			return new SIterator();
		}
		
		/**
		 * Iterators over all child states of this states
		 * 
		 * @author Peter
		 */
		private class SIterator implements Iterator<State> 
		{
			private static final int BUFFER_SIZE = 5;
			
			private Deque<State> stateBuffer = new LinkedList<State>();
			
			private Deque<Token> currentTokenBuffer = new LinkedList<Token>();
			private Deque<Token> nextTokenBuffer = new LinkedList<Token>();
			
			public SIterator()
			{
				// * Buffer all tokens that can be used to generate child states
				for(Token token : selection)
					if(token.depth() < maxDepth)
						currentTokenBuffer.add(token);
			}
			
			@Override
			public boolean hasNext()
			{
				buffer();
				return ! stateBuffer.isEmpty();
			}

			@Override
			public State next()
			{
				buffer();
				return stateBuffer.pop();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
			
			@SuppressWarnings("unchecked")
			private void buffer()
			{
				while(stateBuffer.size() < BUFFER_SIZE)
				{
					nodeBuffer();
					if(nextTokenBuffer.isEmpty())
						return;
					
					stateBuffer.add(new State(selection, nextTokenBuffer.pop()));
				}
			}

			private void nodeBuffer()
			{
				while(nextTokenBuffer.size() < BUFFER_SIZE)
				{
					if(currentTokenBuffer.isEmpty())
						return;
					
					Token currentToken = currentTokenBuffer.pop();
					for(DTNode<String, String> node : currentToken.node().neighbors())
						if(! nodes.contains(node))
							nextTokenBuffer.add(new Token(node, currentToken.depth() + 1));
				}
			}
			
		}

		@Override
		public int compareTo(State o)
		{
			return Double.compare(this.score(), o.score());
		}
		
		public Set<Token> selection()
		{
			return selection;
		}
		
		public Set<Node<String>> nodes()
		{
			return nodes;
		}
		
	}
	
	/**
	 * Wrapper class for node and depth
	 * @author Peter
	 *
	 */
	private class Token
	{
		private DTNode<String, String> node;
		private int depth;
		
		public Token(DTNode<String, String> node, int depth)
		{
			this.node = node;
			this.depth = depth;
		}

		public DTNode<String, String> node()
		{
			return node;
		}

		public int depth()
		{
			return depth;
		}
		
		public double score()
		{
			return bintropy(node, depth);
		}
	}
	
}
