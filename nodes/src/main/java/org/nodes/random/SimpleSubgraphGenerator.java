package org.nodes.random;

import static org.nodes.util.Functions.choose;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.util.AbstractGenerator;
import org.nodes.util.Generator;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.random.LinkGenerators.LinkGenerator;
import org.nodes.util.Functions;
import org.nodes.util.Permutations;
import org.nodes.util.Series;

/**
 * Samples subgraphs 
 * 
 * @author Peter
 *
 * @param <L>
 */
public class SimpleSubgraphGenerator extends AbstractGenerator<List<Integer>>
{
	private static final int REJECTION_TRIES = 10;

	private Generator<Integer> ints;
	
	private Graph<?> graph;
	
	public SimpleSubgraphGenerator(Graph<?> graph, Generator<Integer> ints)
	{
		this.graph = graph;
		this.ints = ints;
	}

	@Override
	public List<Integer> generate()
	{
		int depth = ints.generate();
		List<Integer> result = new ArrayList<Integer>(depth);
		
		boolean success;
		do {
			success = true;
			result.add(Global.random().nextInt(graph.size()));
			while(result.size() < depth)
				if(! addNeighbor(result))
				{
					success = false;
					break;
				}
		} while(!success);
		
		return result;
	}
	
	private boolean addNeighbor(List<Integer> indices)
	{
		Node<?> randomNeighbor = randomNeighbor(indices);
		
		int i = 0;
		while(randomNeighbor == null || indices.contains(randomNeighbor.index()))
		{
			randomNeighbor = randomNeighbor(indices);
			if(i++ > REJECTION_TRIES)
			{
				randomNeighbor = randomNeighborExhaustive(indices);
				if(randomNeighbor == null)
					return false;
				else
					break;
			}
		}
		
		indices.add(randomNeighbor.index());
		return true;
	}
	
	/**
	 * Searches explicitly for a neighbor that is not contained in the index list
	 * @param result
	 * @return
	 */
	private Node<?> randomNeighborExhaustive(List<Integer> indices)
	{
		List<Node<?>> candidates = new ArrayList<Node<?>>();
		for(int index : indices)
		{
			Node<?> node = graph.get(index);
			for(Node<?> neighbor : node.neighbors()) 
				if(! indices.contains(neighbor.index()))
					candidates.add(neighbor);
		}
		
		if(candidates.isEmpty())
			return null;
		
		return choose(candidates);
	}

	private Node<?> randomNeighbor(List<Integer> indices)
	{
		Node<?> randomNode = graph.get(choose(indices));
		Collection<? extends Node<?>> neighbors = randomNode.neighbors();
		if(neighbors.isEmpty())
			return null;
		
		return choose(neighbors);
	}
}
