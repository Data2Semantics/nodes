package org.nodes.compression;

import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.prefix;
import static org.nodes.compression.Functions.toc;
import static org.nodes.util.Functions.logFactorial;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.util.BitString;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.GZIPCompressor;
import org.nodes.util.OnlineModel;
import org.nodes.util.Pair;
import org.nodes.util.Series;

/**
 * Represents a selection of operations on a graph, a motif, and a list of 
 * occurrences for the motif 
 *
 * In contract to MotifVar.java, this version includes tags (edge labels) in the
 * motif and silhouette. Hence this method only works on DTGraphs.
 * 
 * @author Peter
 *
 */
public class MotifVarTags 
{
	public static final String MOTIF_SYMBOL = "|M|";
	public static final String VARIABLE_SYMBOL = "|V|";
	
	private DTGraph<String, String> graph;
	private DTGraph<String, String> motif;		
	private List<List<Integer>> occurrences; 

	// - This list holds the index of the occurrence the given node belongs to
	//
	private List<Integer> inOccurrence;
	private int replacedNodes = 0;
	private int numLabels;
	
	public MotifVarTags(
			DTGraph<String, String> graph, 
			DTGraph<String, String> motif,
			List<List<Integer>> occurrences)
	{
		super();
		this.graph = graph;
		this.motif = motif;
		this.occurrences = occurrences;

		inOccurrence = new ArrayList<Integer>(graph.size());
		for(int i : series(graph.size()))
			inOccurrence.add(null);

		for(int occIndex : Series.series(occurrences.size()))
		{
			for (Integer i : occurrences.get(occIndex))
			{
				inOccurrence.set(i, occIndex);
				replacedNodes++;
			}
		}
		
		numLabels = graph.labels().size();
	}

	public double size()
	{
		double bits = 0.0;
		
		bits += labelSets();
		bits += motif();
		
		bits += silhouetteStructure();
		bits += silhouetteLabels();
		
		bits += labelSubstitutions();
		bits += wiring();
				
		return bits;
	}
	
	/**
	 * The cost of storing the motif
	 * 
	 * @param graph
	 * @param sub
	 * @return
	 */
	public double motif()
	{
		double bits = 0.0;
		
		// * Store the structure
		bits += EdgeListCompressor.directed(motif); 
		
		// * Store the labels
		OnlineModel<String> labelModel = new OnlineModel<String>(graph.labels());
		labelModel.add(VARIABLE_SYMBOL, 0.0);
		
		for(DNode<String> node : motif.nodes())
			bits += - Functions.log2(labelModel.observe(node.label()));
		
		// * Store the tags
		OnlineModel<String> tagModel = new OnlineModel<String>(graph.tags());
		tagModel.add(VARIABLE_SYMBOL, 0.0);
		
		for(DTLink<String, String> link : motif.links())
			bits += - Functions.log2(tagModel.observe(link.tag()));
		
		return bits;
	}
	
	/**
	 * Bits required to store all labels once.
	 * 
	 * @param graph
	 * @return
	 */
	public double labelSets()
	{
		GZIPCompressor<List<Object>> compressor = new GZIPCompressor<List<Object>>();
		
		// * Labels
		double bits = 0.0;

		// ** Label set
		List<String> labelSet = new ArrayList<String>(graph.labels());
		bits += compressor.compressedSize(labelSet);
		
		// ** Tag Set
		List<String> tagSet = new ArrayList<String>(graph.tags());	
		bits += compressor.compressedSize(tagSet);
				
		return bits;
	}

	/**
	 * Computes the number of bits required to store the structure and label
	 * sequences of the silhouette graph, without actually creating the silhouette graph
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @return
	 */
	public double silhouetteStructure()
	{
		double bits = 0;

		// * Store the subbed graph

		OnlineModel<Integer> source = new OnlineModel<Integer>(Collections.EMPTY_LIST);
		OnlineModel<Integer> target = new OnlineModel<Integer>(Collections.EMPTY_LIST);

		// - observe all symbols

		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
			{
				source.addToken(node.index());
				target.addToken(node.index());
			}

		// - negative numbers represent symbol nodes
		//   (for us, to the decoder these are simply tokens)
		for (int i : series(1, occurrences.size() + 1))
		{
			source.addToken(-i);
			target.addToken(-i);
		}

		int subbedNumLinks = 0;
		for (Link<String> link : graph.links())
		{

			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());

			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
				subbedNumLinks++;
		}
		
		// Size of the subbed graph
		bits += prefix(graph.size() - replacedNodes + occurrences.size());
				
		// Num links in the subbed graph
		bits += prefix(subbedNumLinks);
		
		for (Link<String> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());

			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
			{
				int first = link.first().index();
				int second = link.second().index();

				first = inOccurrence.get(first) == null ? 
						first : -(inOccurrence.get(first) + 1);
				second = inOccurrence.get(second) == null ? 
						second : -(inOccurrence.get(second) + 1);

				double p = source.observe(first) * target.observe(second);
				
				bits += - Functions.log2(p);
			}
		}

		bits -= logFactorial(subbedNumLinks, 2.0);
		
		return bits;
	}

	public double silhouetteLabels()
	{
		// * Labels
		double bits = 0;
		
		OnlineModel<String> labelModel = new OnlineModel<String>(graph.labels());
		labelModel.addToken(MOTIF_SYMBOL);
		
		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
				bits += -log2(labelModel.observe(node.label()));
		
		for (int i : Series.series(occurrences.size()))
			bits += -log2(labelModel.observe(MOTIF_SYMBOL));
		
		// * Tags
		OnlineModel<String> tagModel = new OnlineModel<String>(graph.tags());

		for(DTLink<String, String> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());
			
			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
			{
				// -- the link is part of the silhouette.
				bits += - log2(tagModel.observe(link.tag()));
			}
		}
		
		return bits;
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
	public double labelSubstitutions()
	{
		double bits = 0.0;
		
		// - Find the number of terminals per symbol
		List<Set<String>> terminalSets = new ArrayList<Set<String>>();
		List<Integer> indices = new ArrayList<Integer>();

		for (Node<String> node : motif.nodes())
			if (VARIABLE_SYMBOL.equals(node.label()))
			{
				int index = node.index();
				indices.add(index);

				Set<String> set = new HashSet<String>();

				for (List<Integer> occurrence : occurrences)
				{
					Node<String> occNode = graph.get(occurrence.get(index));
					set.add(occNode.label());
				}

				terminalSets.add(set);
			}

		// Per index of a symbol node in the motif we use a KT model to store
		// the sequence of terminals
		for (int i : Series.series(indices.size()))
		{
			Set<String> terminals = terminalSets.get(i);

			// - Store the set of terminals
			bits += prefix(terminals.size());
			bits += log2(numLabels) * terminals.size();
			
			// - Store the sequence of terminals
			OnlineModel<String> model = new OnlineModel<String>(terminals);
			
			int index = indices.get(i);
			for (List<Integer> occurrence : occurrences)
			{
				String label = graph.get(occurrence.get(index)).label();
				bits += - log2(model.observe(label));
			}
		}

		return bits;
	}
	
	public double tagSubstitutions()
	{		
		// * Find the sequence of terminals for each symbol in the motif
		Map<Indicator, List<String>> map 
			= new LinkedHashMap<Indicator, List<String>>();
		
		// - collect all pairs of nodes that are linked
		Set<Pair<Integer, Integer>> pairedNodes = new LinkedHashSet<Pair<Integer,Integer>>();
		for(DTLink<String, String> link : motif.links())
			pairedNodes.add(new Pair<Integer, Integer>( link.first().index(), link.second().index()));
		
		for(Pair<Integer, Integer> pair : pairedNodes)
		{
			// - count of the number of symbol links
			int numVars = 0;
			// - tags that the motif has between thesetwo nodes
			List<String> motifTags = new ArrayList<String>();
			for(DTLink<String, String> link : motif.get(pair.first()).linksOut(motif.get(pair.second())) )
			{
				if(VARIABLE_SYMBOL.equals(link.tag()))
					numVars++;
				motifTags.add(link.tag());
			}
			
			if(numVars > 0)
				for(List<Integer> occurrence : occurrences)
				{
					DTNode<String, String> graphFrom = graph.get(occurrence.get(pair.first())),
					                       graphTo   = graph.get(occurrence.get(pair.second()));
					
					// - the tags that the occurrence has between the two nodes
					List<String> occTags = new ArrayList<String>();
					for(DTLink<String, String> link : graphFrom.linksOut(graphTo))
						occTags.add(link.tag());
					
					// - match and remove all non-var tags
					for(String tag : motifTags)
						if(! VARIABLE_SYMBOL.equals(tag))
							occTags.remove(tag);
					
					assert(occTags.size() == numVars);
					
					for(int i : series(occTags.size()))
					{
						Indicator ind = new Indicator(pair.first(), pair.second(), i);
						
						if(! map.containsKey(ind))
							map.put(ind, new ArrayList<String>());
						
						map.get(ind).add(occTags.get(i));
					}
				}
		}
		
		// 'map' now contains for each variable-link a list of the substitutions 
		// required for that variable. We store each sequence first with a set
		// of the symbols that occur and then a KT process representing
		// the actual sequence.
		
		double bits = 0.0;

		for(List<String> sequence : map.values())
		{
			// Store the set of symbols
			Set<String> set = new LinkedHashSet<String>(sequence);
			bits += prefix(set.size());
			bits += set.size() * log2(graph.tags().size());
			
			// Store the sequence
			// We know the length of sequence already
			OnlineModel<String> model = new OnlineModel<String>(set);
			for(String symbol : sequence)
				bits += - log2(model.observe(symbol));
		}
		
		return bits;
	}
	
	/**
	 * Represents a pair of nodes and an index of a link between the two. 
	 * @author Peter
	 *
	 */
	private class Indicator
	{
		private int from, to;
		private int index;
		
		public Indicator(int from, int to, int index)
		{
			this.from = from;
			this.to = to;
			this.index = index;
		}



		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + from;
			result = prime * result + index;
			result = prime * result + to;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Indicator other = (Indicator) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (from != other.from)
				return false;
			if (index != other.index)
				return false;
			if (to != other.to)
				return false;
			return true;
		}



		private MotifVarTags getOuterType()
		{
			return MotifVarTags.this;
		}
		
		public String toString()
		{
			return "I("+from+"->"+to+":"+index+")"; 
		}
	}

	/**
	 * The wiring section of the code records how each edge linking into a motif 
	 * node should be wired into the motif (ie. which of the motif's nodes it 
	 * connects too). We presume a canonical ordering over all such edges and 
	 * store the resulting sequence of integers. 
	 * 
	 * @param wiring
	 * @return
	 */
	public double wiring()
	{
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(motif.size()));

		double bits = 0.0;
		
		for(List<Integer> occurrence : occurrences)
		{
			Set<Integer> occurrenceNodes = new HashSet<Integer>(occurrence);
			for(int indexInOccurrence : Series.series(occurrence.size()))
			{
				int nodeIndex = occurrence.get(indexInOccurrence);
				DNode<String> node = graph.get(nodeIndex);
				
				for(DNode<String> neighbor : node.neighbors())
				{
					int neighborIndex = neighbor.index();
					if(! occurrenceNodes.contains(neighborIndex))
					{
						// * record wiring information
						bits += -log2(om.observe(indexInOccurrence));
					}
				}
			
			}
		}

		return bits;
	}

	/**
	 * Creates the actual graph with ocurrences replaced by symbol nodes. Note
	 * that this operation is not necessary to compute the code length.
	 *  
	 * @param wiring An empty list to which this method will add the wiring info
	 * @return
	 */
	public DTGraph<String, String> subbedGraph(List<Integer> wiring)
	{
		DTGraph<String, String> copy = MapDTGraph.copy(graph);

		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<DNode<String>>> occ = new ArrayList<List<DNode<String>>>(
				occurrences.size());
		for (List<Integer> occurrence : occurrences)
		{
			List<DNode<String>> nodes = new ArrayList<DNode<String>>(
					occurrence.size());
			for (int index : occurrence)
				nodes.add(copy.get(index));
			occ.add(nodes);
		}

		int totalOcc = occ.size(), i = 0;
		// * For each occurrence of the motif on the graph
		int removed = 0;
		for (List<DNode<String>> nodes : occ)
		{
			if (alive(nodes)) // -- make sure none of the nodes of the
								// occurrence
								// been removed. If two occurrences overlap,
								// only the first gets replaced.
			{
				// * Wire a new symbol node into the graph to represent the
				// occurrence
				DNode<String> newNode = copy.add(MOTIF_SYMBOL);

				int indexInSubgraph = 0;
				for (DNode<String> node : nodes)
				{
					for (DNode<String> neighbor : node.neighbors())
						if (!nodes.contains(neighbor))
						{
							for (DLink<String> link : node.linksOut(neighbor))
							{
								newNode.connect(neighbor);
								wiring.add(indexInSubgraph);
							}

							for (DLink<String> link : node.linksIn(neighbor))
							{
								neighbor.connect(newNode);
								wiring.add(indexInSubgraph);
							}
						} else
						{
							removed++;
						}

					indexInSubgraph++;
				}

				for (DNode<String> node : nodes)
					node.remove();
			}
			i++;
		}

		return copy;
	}

	private static boolean alive(List<? extends Node<String>> nodes)
	{
		for (Node<String> node : nodes)
			if (node.dead())
				return false;

		return true;
	}

	private static boolean alive(List<? extends Node<String>> nodes,
			Set<Node<String>> toRemove)
	{
		for (Node<String> node : nodes)
			if (toRemove.contains(node))
				return false;

		return true;
	}
}
