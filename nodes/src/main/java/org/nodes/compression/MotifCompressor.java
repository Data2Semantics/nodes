package org.nodes.compression;

import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.prefix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.OnlineModel;
import org.nodes.util.Series;

public class MotifCompressor extends AbstractGraphCompressor<String>
{
	public static final String MOTIF_SYMBOL = "|M|";
	
	public MotifCompressor()
	{

	}

	
	@Override
	public double structureBits(Graph<String> graph, List<Integer> order)
	{
		return -1.0;
	}
	
	public static double size(
			DGraph<String> graph, 
			DGraph<String> sub, 
			List<List<Integer>> occurrences,
			Compressor<Graph<String>> comp)
	{
		List<Integer> wiring = new ArrayList<Integer>();
		DGraph<String> copy = subbedGraph(graph, sub, occurrences, wiring);
		
		double graphsBits = comp.compressedSize(sub);
		graphsBits += comp.compressedSize(copy);
		return graphsBits + wiringBits(wiring);
	}
	
	/**
	 * Calculates the compressed size for a motif with variable nodes
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @param comp
	 * @return
	 */
	public static double sizeSymbols(
			DGraph<String> graph,
			String symbol,
			DGraph<String> sub, 
			List<List<Integer>> occurrences,
			Compressor<Graph<String>> comp)
	{
		// * Store the graph, and the subgraph and the wiring
		double bits = size(graph, sub, occurrences, comp);
		
		// * Now to store the mapping from symbol to terminal for each occurrence 
		//   of a symbol
		// - Find the number of terminals per symbol
		List<Set<String>> terminalSets = new ArrayList<Set<String>>();
		List<Integer> indices = new ArrayList<Integer>();
		
		for(Node<String> node : sub.nodes())
			if(symbol.equals(node.label()))
			{
				int index = node.index();
				indices.add(index);
				
				Set<String> set = new HashSet<String>();
				
				for(List<Integer> occurrence : occurrences)
				{
					Node<String> occNode = graph.get(occurrence.get(index));
					set.add(occNode.label());
				}
				
				terminalSets.add(set);
			}
		
		// Per index of a symbol node in the motif we use a KT model to store
		// the sequence of terminals
		for(int i : Series.series(indices.size()))
		{
			int index = indices.get(i);
			int n = terminalSets.size();
			
			bits += prefix(n);
			
			// System.out.println(terminalSets.get(i));
			
			OnlineModel<String> model = new OnlineModel<String>();
			model.symbols(terminalSets.get(i));
			
			for(List<Integer> occurrence : occurrences)
			{
				String label = graph.get(occurrence.get(index)).label();
				bits -= log2(model.observe(label));
			}	
		}
		
		// * The string terminals aren't stored.
		
		return bits;
		
	}
	
	public static double wiringBits(List<Integer> wiring)
	{
		int max = Integer.MIN_VALUE;
		for(int i : wiring)
			max = Math.max(max, i);

		OnlineModel<Integer> om = new OnlineModel<Integer>();
		om.symbols(Series.series(max+1));
		
		double bits = 0.0;
		for(int wire : wiring)
			bits -= Functions.log2(om.observe(wire));
		
		return bits;
	}
	
	public static DGraph<String> subbedGraph(
			DGraph<String> inputGraph, 
			DGraph<String> sub, 
			List<List<Integer>> occurrences,
			List<Integer> wiring)
	{
		DGraph<String> copy = MapDTGraph.copy(inputGraph);
		
		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<DNode<String>>> occ = new ArrayList<List<DNode<String>>>(occurrences.size());
		for(List<Integer> occurrence : occurrences)
		{
			List<DNode<String>> nodes = new ArrayList<DNode<String>>(occurrence.size());
			for(int index : occurrence)
				nodes.add(copy.get(index));
			occ.add(nodes);
		}
	
		int totalOcc = occ.size(), i = 0;
		// * For each occurrence of the motif on the graph
		for(List<DNode<String>> nodes : occ)
		{
//			if(i % 100 == 0)
//				Global.log().info("Starting " + i + " of "+ totalOcc + " occurrences ("+String.format("%.2f", ((100.0 * i)/totalOcc))+"%). ");
			
			if(alive(nodes)) // -- make sure none of the nodes of the occurrence 
				             //    been removed. If two occurrences overlap, 
			                 //    only the first gets replaced. 
			{
				// * Wire a new symbol node into the graph to represent the occurrence
				DNode<String> newNode = copy.add(MOTIF_SYMBOL);

				int indexInSubgraph = 0;
				for(DNode<String> node : nodes)
				{
					for(DNode<String> neighbor : node.neighbors())
						if(! nodes.contains(neighbor))
						{
							for(DLink<String> link : node.linksOut(neighbor))
								newNode.connect(neighbor);
							for(DLink<String> link : node.linksIn(neighbor))
								neighbor.connect(newNode);
							
							wiring.add(indexInSubgraph);
						}
					
					indexInSubgraph ++;
				}
				
				for(DNode<String> node : nodes)
					node.remove();
			}
			i++;
		}
		
		return copy;
	}	
	
	/**
	 * Substitue multiple motifs
	 * 
	 * @param inputGraph
	 * @param subs
	 * @param occurrencesMap
	 * @param wiring
	 * @return
	 */
	public static DGraph<String> subbedGraphMulti(
			DGraph<String> inputGraph, 
			List<DGraph<String>> subs, 
			Map<DGraph<String>, List<List<Integer>>> occurrencesMap,
			List<Integer> wiring)
	{
		int numSymbols = 0;
		DGraph<String> copy = MapDTGraph.copy(inputGraph);
		
		Set<Node<String>> toRemove = new LinkedHashSet<Node<String>>();
		
		for(DGraph<String> sub : subs)
		{
			
			String symbol = MOTIF_SYMBOL + (numSymbols++);
			List<List<Integer>> occurrences = occurrencesMap.get(sub);
			
			// * Translate the occurrences from integers to nodes (in the copy)
			List<List<DNode<String>>> occ = new ArrayList<List<DNode<String>>>(occurrences.size());
			for(List<Integer> occurrence : occurrences)
			{
				List<DNode<String>> nodes = new ArrayList<DNode<String>>(occurrence.size());
				for(int index : occurrence)
					nodes.add(copy.get(index));
				occ.add(nodes);
			}
		
			int totalOcc = occ.size(), i = 0;
			// * For each occurrence of the motif on the graph
			for(List<DNode<String>> nodes : occ)
			{
//				if(i % 100 == 0)
//					Global.log().info("Multi: starting " + i + " of "+ totalOcc + " occurrences ("+String.format("%.2f", ((100.0 * i)/totalOcc))+"%). ");
				
				if(alive(nodes, toRemove)) 	// -- make sure none of the nodes of the occurrence 
											//    been removed. If two occurrences overlap, 
											//    only the first gets replaced. 
				{
					// * Wire a new symbol node into the graph to represent the occurrence
					DNode<String> newNode = copy.add(symbol);
	
					int indexInSubgraph = 0;
					for(DNode<String> node : nodes)
					{
						for(DNode<String> neighbor : node.neighbors())
							if(! nodes.contains(neighbor))
							{
								for(DLink<String> link : node.linksOut(neighbor))
									newNode.connect(neighbor);
								for(DLink<String> link : node.linksIn(neighbor))
									neighbor.connect(newNode);
								
								wiring.add(indexInSubgraph);
							}
						
						indexInSubgraph ++;
					}
				}
				i++;
			}
		}
		
		for(Node<String> node : toRemove)
			node.remove();
		
		return copy;
	}	

	private static boolean alive(List<? extends Node<String>> nodes)
	{
		for(Node<String> node : nodes)
			if(node.dead())
				return false;
		
		return true;
	}
	
	private static boolean alive(List<? extends Node<String>> nodes, Set<Node<String>> toRemove)
	{
		for(Node<String> node : nodes)
			if(toRemove.contains(node))
				return false;
		
		return true;
	}		
}
