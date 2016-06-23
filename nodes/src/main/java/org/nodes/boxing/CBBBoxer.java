package org.nodes.boxing;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.Graph;
import org.nodes.Node;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class CBBBoxer<L> implements BoxingAlgorithm<L>
{
	private Graph<L> graph;
	
	// private Distance<V> usp;
	
	public CBBBoxer(Graph<L> graph)
	{
		this.graph = graph;
		
		// usp = new DijkstraDistance<V, E>(graph);
	}

	@Override
	public Boxing<L> box(int l)
	{
		List<Set<Node<L>>> result = new ArrayList<Set<Node<L>>>();
				
		Set<Node<L>> uncovered = new LinkedHashSet<Node<L>>();
		uncovered.addAll(graph.nodes());
				
		while(! uncovered.isEmpty())
		{
// 			Global.log().info("uncovered size: " +  uncovered.size());
			List<Node<L>> candidates = new ArrayList<Node<L>>(uncovered);
			Set<Node<L>> box = new HashSet<Node<L>>();
			while(! candidates.isEmpty())
			{
				int draw = Global.random().nextInt(candidates.size());
				Node<L> center = candidates.remove(draw);
				
				box.add(center);
				uncovered.remove(center);
				
				// Remove the candidates that are too far away
				Set<Node<L>> neighbourhood = neighbourhood(center, l);
				candidates.retainAll(neighbourhood);
				
//				Iterator<V> it = candidates.iterator();
//				while(it.hasNext())
//				{
//					V other = it.next();
//					if(distance(center, other) >= l)
//						it.remove();
//				}
			}
			
			result.add(box);
		}
		
		return new Boxing<L>(result, graph);
	}
	
	/**
	 * Return all nodes with distance less than d to center.
	 * @param center
	 * @param d
	 * @return
	 */
	public Set<Node<L>> neighbourhood(Node<L> center, int d)
	{
		Set<Node<L>> neighbourhood = new LinkedHashSet<Node<L>>();
		Set<Node<L>> shell0 = new LinkedHashSet<Node<L>>(),
		       shell1 = new LinkedHashSet<Node<L>>();
				
		neighbourhood.add(center);
		shell0.add(center);
		
		int c = 1;
		while(c < d)
		{
			for(Node<L> node : shell0)
				shell1.addAll(node.neighbors());
			 
			neighbourhood.addAll(shell1);
			
			shell0 = shell1;
			shell1 = new LinkedHashSet<Node<L>>();
			c++;
		}
		
		return neighbourhood;
	}
}
