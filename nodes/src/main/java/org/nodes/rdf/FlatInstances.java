package org.nodes.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;

public class FlatInstances implements Instances
{
	protected DTGraph<String, String> graph;
	protected int instanceSize, maxDepth;
	protected Scorer scorer;
	protected Comparator<Token> comp;
	
	public FlatInstances(DTGraph<String, String> graph, int instanceSize,
			int maxDepth, Scorer scorer)
	{
		super();
		this.graph = graph;
		this.instanceSize = instanceSize;
		this.maxDepth = maxDepth;
		
		this.scorer = scorer;
		comp = new ScorerComparator(scorer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DTNode<String, String>> instance(Node<String> instanceNode)
	{
		List<Token> nodes = neighborhood(instanceNode, maxDepth);
		Collections.sort(nodes, Collections.reverseOrder(comp));

		List<DTNode<String, String>> result = new ArrayList<DTNode<String,String>>(instanceSize);
		
		for(Token token : nodes.subList(0, instanceSize))
			result.add((DTNode<String, String>)token.node());
		
		return result;
	}
	
	public static List<Token> neighborhood(Node<String> center, int depth)
	{
		Set<Node<String>> nb = new LinkedHashSet<Node<String>>();
		nb.add(center);
		
		List<Token> tokens = new ArrayList<Token>();
		tokens.add(new Token(center, 0));
		
		nbInner(tokens, nb, depth);
		
		return tokens;
	}
	
	private static void nbInner(List<Token> tokens, Set<Node<String>> nodes, int depth)
	{
		if(depth == 0)
			return;
		
		List<Token> newTokens = new ArrayList<Token>();
		
		for(Token token : tokens)
			for(Node<String> neighbor : token.node().neighbors())
				if(! nodes.contains(neighbor))
					newTokens.add(new Token(neighbor, token.depth() + 1));
		
		tokens.addAll(newTokens);
		for(Token token : newTokens)
			nodes.add(token.node());
		
		nbInner(tokens, nodes, depth - 1);
	}
}
