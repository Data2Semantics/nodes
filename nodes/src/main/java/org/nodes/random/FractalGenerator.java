package org.nodes.random;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTLink;
import org.nodes.UTNode;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class FractalGenerator
{
	public static final String LABEL = "x";
	
	// * Size of offspring per node
	private int offspring;
	// * number of links between offspring
	private int linksBetweenOffspring;
	// * Probability that hubs stay connected
	private double hubConnectionProb;
		
	private UTGraph<String, String> graph;
	
	/**
	 * Creates a fractal network generator
	 * 
	 * @param offspring
	 * @param linksBetweenOffspring
	 * @param hubConnectionProb The probability that hubs stay connected. If 
	 *    this value is 1, the network is pure small-world (mode 1) if it is zero,
	 *    the network is pure fractal (mode 2). 
	 */
	public FractalGenerator(int offspring, int linksBetweenOffspring,
			double hubConnectionProb)
	{
		this.offspring = offspring;
		this.linksBetweenOffspring = linksBetweenOffspring;
		this.hubConnectionProb = hubConnectionProb;
		
		graph = new MapUTGraph<String, String>();
		graph.add(LABEL).connect(graph.add(LABEL));
	}

	public void iterate()
	{
		
		// * Copy the links to avoid concurrent modification
		List<UTLink<String, String>> links = new ArrayList<UTLink<String,String>>((int)graph.numLinks());
		for(UTLink<String, String> link : graph.links())
			links.add(link);
		
		for(UTLink<String, String> link : links)
		{
			// * Add 'offspring' neighbours to the nodes on each side of this link ...
			List<UTNode<String, String>> nodesA = new ArrayList<UTNode<String,String>>(offspring);
			List<UTNode<String, String>> nodesB = new ArrayList<UTNode<String,String>>(offspring);
			
			for(int i : series(offspring))
			{
				nodesA.add(graph.add(LABEL));
				nodesB.add(graph.add(LABEL));
				
				nodesA.get(i).connect(link.first());
				nodesB.get(i).connect(link.second());
			}
			
			// ... connect 'linksBetweenOffspring' of the new nodes.
			int last = 0;
			for(int i : series(linksBetweenOffspring))
			{
				nodesA.get(i).connect(nodesB.get(i));
				last = i;
			}
			
			// ... possibly exchange the original link for another link between the new neighbours
			if(Global.random().nextDouble() < hubConnectionProb)
			{
				link.remove();
				
				nodesA.get(last + 1).connect(nodesB.get(last + 1));				
			}
		}
	}

	public UTGraph<String, String> graph()
	{
		return graph; 	
	}
	
	public static int size(int offspring, int offspringLinks, int depth)
	{
		int n = 2;
		int l = 1;
		
		int n0 = n, l0 = l;
		for(int i : series(depth))
		{
			n = n0 + 2 * l0 * offspring;
			l = l0 + l0 * 2 * offspring + l0 * offspringLinks;
			
			n0 = n; l0 = l;
		}
		
		return n;
				
//		if(depth == 0)
//			return 2;
//		
//		int ld = numLinks(offspring, offspringLinks, depth - 1);
//		int nd = size(offspring, offspringLinks, depth - 1);
//			
//		return nd + 2 * ld * offspring;
	}
	
	public static int numLinks(int offspring, int offspringLinks, int depth)
	{
		int n = 2;
		int l = 1;
		
		int n0 = n, l0 = l;
		for(int i : series(depth))
		{
			n = n0 + 2 * l0 * offspring;
			l = l0 + l0 * 2 * offspring + l0 * offspringLinks;
			
			n0 = n; l0 = l;
		}
		
		return l;
	}
//	
//	public static Result search(int nodes, int links)
//	{
//		
//	}
//	
//	public class State implements Comparable<State>
//	{
//		private int offspring, offspringLinks, depth;
//		
//		private int targetNodes, targetLinks;
//		private int myNodes, myLinks;
//
//		public State(int offspring, int offspringLinks, int depth, int targetNodes, int targetLinks)
//		{
//			this.offspring = offspring;
//			this.offspringLinks = offspringLinks;
//			this.depth = depth;
//			
//			myNodes = size(offspring, offspringLinks, depth);
//			myLinks = numLinks(offspring, offspringLinks, depth);
//		}
//
//		public int offspring()
//		{
//			return offspring;
//		}
//
//		public int offspringLinks()
//		{
//			return offspringLinks;
//		}
//
//		public int depth()
//		{
//			return depth;
//		}
//		
//		public int score()
//		{
//			int n = targetNodes - myLinks;
//			int l = targetLinks - myLinks;
//			
//			return n * n + l * l;
//		}
//		
//		public List<State> children()
//		{	
//			if(myNodes > targetNodes && myLinks > targetLinks)
//				return Collections.emptyList();
//			
//			return Arrays.asList(
//					new State(offspring + 1, offspringLinks, depth, targetNodes, targetLinks),
//					new State(offspring, offspringLinks + 1, depth, targetNodes, targetLinks),
//					new State(offspring, offspringLinks, depth + 1, targetNodes, targetLinks)
//				);
//		}
//
//		@Override
//		public int compareTo(State other)
//		{
//			if(this.score() != other.score())
//				// * Return the one with the lowest 
//				return Double.compare(this.score(), other.score());
//		}
//	}
//	
//	public class Result
//	{
//		private int offspring, offspringLinks, depth;
//
//		public Result(int offspring, int offspringLinks, int depth)
//		{
//			this.offspring = offspring;
//			this.offspringLinks = offspringLinks;
//			this.depth = depth;
//		}
//
//		public int offspring()
//		{
//			return offspring;
//		}
//
//		public int offspringLinks()
//		{
//			return offspringLinks;
//		}
//
//		public int depth()
//		{
//			return depth;
//		}
//	}
}

/* Python Code

def fractal_model(generation,m,x,e):
	"""
	Returns the fractal model introduced by 
	Song, Havlin, Makse in Nature Physics 2, 275.
	generation = number of generations
	m = number of offspring per node
	x = number of connections between offsprings
	e = probability that hubs stay connected
	1-e = probability that x offsprings connect.
	If e=1 we are in MODE 1 (pure small-world).
	If e=0 we are in MODE 2 (pure fractal).
	"""
	G=Graph()
	G.add_edge(0,1) #This is the seed for the network (generation 0)
	node_index = 2
	for n in range(1,generation+1):
		all_links = G.edges()
		while all_links:
			link = all_links.pop()
			
			new_nodes_a = range(node_index,node_index + m)
			node_index += m
			new_nodes_b = range(node_index,node_index + m)
			node_index += m
			
			G.add_edges_from([(link[0],node) for node in new_nodes_a])
			G.add_edges_from([(link[1],node) for node in new_nodes_b])
			
			repulsive_links = zip(new_nodes_a,new_nodes_b)
			G.add_edges_from([repulsive_links.pop() for i in range(x-1)])
			
			if random.random() > e:
				G.remove_edge(link[0],link[1])
				rl = repulsive_links.pop()
				G.add_edge(rl[0],rl[1])
	return G
*/
