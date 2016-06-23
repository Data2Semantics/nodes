package org.nodes.random;

import static nl.peterbloem.kit.Functions.choose;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.FastWalkable;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.clustering.ConnectionClusterer;
import org.nodes.random.LinkGenerators.LinkGenerator;

import nl.peterbloem.kit.AbstractGenerator;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Generator;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Permutations;
import nl.peterbloem.kit.Series;

/**
 * A fast and simple extractor of subgraphs.
 * 
 * For the best performance, make sure to use a FastWalkable graph.
 * @author Peter
 *
 * @param <L>
 */
public class SimpleSubgraphGenerator extends AbstractGenerator<List<Integer>>
{
	private static final int REJECTION_TRIES = 10;
	private static final int RESTARTS = 1000;

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
		int restarts = 0;
		
		int depth = ints.generate();
		List<Integer> result = new ArrayList<Integer>(depth);
		
		boolean success;
		do {
			success = true;
			
			result.clear();
			result.add(Global.random().nextInt(graph.size()));
			restarts ++;
			if(restarts > RESTARTS)
				checkLCC(depth);
			
			while(result.size() < depth)
				if(! addNeighbor(result))
				{
					success = false;
					break;
				}
		} while(!success);
		
		return result;
	}
	
	private void checkLCC(int depth)
	{
		int lccSize = ConnectionClusterer.largest(graph).size();
		
		if(lccSize < depth)
			throw new RuntimeException("The largest connected component of the graph has size ("+lccSize+"). This means that subgraphs of size "+depth+" cannot be generated.");
	}

	protected boolean addNeighbor(List<Integer> indices)
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
	protected Node<?> randomNeighborExhaustive(List<Integer> indices)
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

	/**
	 * Quickly selects a random neighbor of a random node in indices. 
	 * 
	 * The resulting neighbor may be contained in indices.
	 */
	protected Node<?> randomNeighbor(List<Integer> indices)
	{
		Node<?> randomNode = graph.get(choose(indices));
		
		Collection<? extends Node<?>> neighbors;
		
		if(graph instanceof FastWalkable<?, ?>)
			neighbors = ((FastWalkable) graph).neighborsFast(randomNode);
		else
			neighbors = randomNode.neighbors();
		
		
		if(neighbors.isEmpty())
			return null;
		
		return choose(neighbors);
	}
}
