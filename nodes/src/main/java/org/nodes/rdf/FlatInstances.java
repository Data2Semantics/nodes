package org.nodes.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;

import nl.peterbloem.kit.MaxObserver;

public class FlatInstances implements Instances
{
	protected DTGraph<String, String> graph;
	protected int instanceSize, maxDepth;
	protected Scorer scorer;
	protected Comparator<Token> comp;
	protected boolean directed;
	
	public FlatInstances(DTGraph<String, String> graph, int instanceSize,
			int maxDepth, Scorer scorer)
	{
		this(graph, instanceSize, maxDepth, scorer, true);
	}
	
	public FlatInstances(DTGraph<String, String> graph, int instanceSize,
			int maxDepth, Scorer scorer, boolean directed)
	{
		this.directed = directed;
		this.graph = graph;
		this.instanceSize = instanceSize;
		this.maxDepth = maxDepth;
		
		this.scorer = scorer;
		comp = new ScorerComparator(scorer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DTNode<String, String>> instance(DNode<String> instanceNode)
	{
		List<Token> nodes = neighborhood(instanceNode, maxDepth, directed);
		
		if(instanceSize == -1)
		{
			List<DTNode<String, String>> result = new ArrayList<DTNode<String,String>>(nodes.size());

			for(Token token : nodes)
				result.add((DTNode<String, String>)token.node());
			
			return result;
		}
		
		MaxObserver<W> observer = new MaxObserver<FlatInstances.W>(instanceSize);
		for(Token token : nodes)
			observer.observe(new W(token));
		
		List<DTNode<String, String>> result = new ArrayList<DTNode<String,String>>(instanceSize);
		for(W w : observer.elements())
			result.add(w.node());
			
		return result;
	}
	
	public static List<Token> neighborhood(DNode<String> center, int depth, boolean directed)
	{
		Set<DNode<String>> nb = new LinkedHashSet<DNode<String>>();
		nb.add(center);
		
		List<Token> tokens = new ArrayList<Token>();
		tokens.add(new Token(center, 0));
		
		nbInner(tokens, nb, depth, directed);
		
		return tokens;
	}
	
	private static void nbInner(List<Token> tokens, Set<DNode<String>> nodes, int depth, boolean directed)
	{
		if(depth == 0)
			return;
		
		List<Token> newTokens = new ArrayList<Token>();
		
		for(Token token : tokens)
			for(DNode<String> neighbor : directed ? token.node().neighbors() : token.node().neighbors())
				if(! nodes.contains(neighbor))
					newTokens.add(new Token(neighbor, token.depth() + 1));
		
		tokens.addAll(newTokens);
		for(Token token : newTokens)
			nodes.add(token.node());
		
		nbInner(tokens, nodes, depth - 1, directed);
	}

	private class W implements Comparable<W>
	{
		private Token token;
		private double score;
		
		public W(Token token)
		{
			this.token = token;
			this.score = scorer.score(token.node(), token.depth());
			
		}
		
		@Override
		public int compareTo(W o)
		{
			return Double.compare(this.score, o.score);
		}	
		
		public DTNode<String, String> node()
		{
			return (DTNode<String, String>) token.node();
		}
	}
}
