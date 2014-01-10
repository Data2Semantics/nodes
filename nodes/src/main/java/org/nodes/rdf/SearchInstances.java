package org.nodes.rdf;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;
import org.nodes.util.MaxObserver;

/** 
 * A beamwidth search for the best nodes to represent instances.
 * 
 * @author Peter
 *
 */
public class SearchInstances implements Instances
{
	protected DTGraph<String, String> graph;
	protected int beamWidth, instanceSize, maxDepth;
	
	protected Scorer scorer;
	
	public SearchInstances(DTGraph<String, String> graph,
			int beamWidth, int instanceSize, int maxDepth, Scorer scorer)
	{
		this.graph = graph;
		this.beamWidth = beamWidth;
		this.instanceSize = instanceSize;
		this.maxDepth = maxDepth;
		this.scorer = scorer;
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
			
			List<Integer> depths = new ArrayList<Integer>();
			
			List<DTNode<String, String>> nodes = 
					new ArrayList<DTNode<String,String>>(best.selection().size());
			
			for(Token token : best.selection())
			{
				nodes.add((DTNode<String, String>)token.node());
				depths.add(token.depth());
			}
			
			System.out.println(depths);
				
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
				score += scorer.score(token.node(), token.depth());
			
			return score;
		}
		
		public Iterator<State> iterator()
		{
			if(selection.size() >= instanceSize)
			{
				List<State> empty = new ArrayList<State>();
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
					for(Node<String> node : currentToken.node().neighbors())
						if(! nodes.contains(node))
							nextTokenBuffer.add(new Token(node, currentToken.depth() + 1));
				}
			}
			
		}

		@Override
		public int compareTo(State o)
		{
			double thisSize = this.nodes.size();
			double thatSize = o.nodes.size();
			
			double thisScore = this.score();
			double thatScore = o.score();
			
			if(thisSize == thatSize)
				return Double.compare(this.score(), o.score());
			
			return Double.compare(thisSize, thatSize);
		}
		
		public Set<Token> selection()
		{
			return selection;
		}
		
		public Set<Node<String>> nodes()
		{
			return nodes;
		}
		
		public String toString()
		{
			return nodes.toString() + " " + score();
		}
	}
}
