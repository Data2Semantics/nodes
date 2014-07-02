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
import org.nodes.util.Series;

/**
 * Represents a selection of operations on a graph, a motif, and a list of 
 * occurrences for the motif 
 * 
 * 
 * NOTE on tags: All other graph compressors store tags if the graph has them. 
 * In our case, we store the whole graph as though it doesn't have tags and then
 * store the tags in a straightforward manner (using a KT estimator). This means 
 * that (for now), the motif does not contain tags. 
 * 
 * @author Peter
 *
 */
public class MotifVar 
{
	public static final String MOTIF_SYMBOL = "|M|";
	public static final String VARIABLE_SYMBOL = "|V|";
	
	private DGraph<String> graph;
	private DGraph<String> motif;		
	private List<List<Integer>> occurrences; 

	// - This list holds the index of the occurrence the given node belongs to
	//
	private List<Integer> inOccurrence;
	private int replacedNodes = 0;
	private int numLabels;
	
	public MotifVar(DGraph<String> graph, DGraph<String> motif,
			List<List<Integer>> occurrences)
	{
		super();
		this.graph = graph;
		this.motif = motif;
		this.occurrences = occurrences;

		inOccurrence = new ArrayList<Integer>(graph.size());
		for(int i : Series.series(graph.size()))
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
		
		bits += substitutions();
		bits += wiring();
		
		bits += tags();
		
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
		OnlineModel<String> model = new OnlineModel<String>(graph.labels());
		model.add(VARIABLE_SYMBOL, 0.0);
		
		for(DNode<String> node : motif.nodes())
			bits += - Functions.log2(model.observe(node.label()));
		
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
		// * Labels
		double labelBits = 0;

		// ** Label set
		List<String> labelSet = new ArrayList<String>(graph.labels());
		
		GZIPCompressor<List<Object>> compressor = new GZIPCompressor<List<Object>>();
		labelBits += compressor.compressedSize(labelSet);
				
		return labelBits;
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
		// -- Labels
		double bits = 0;
		
		OnlineModel<String> labelModel = new OnlineModel<String>(graph.labels());
		labelModel.addToken(MOTIF_SYMBOL);

		System.out.println(labelModel.tokens());
		
		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
				bits += -log2(labelModel.observe(node.label()));
		
		for (int i : Series.series(occurrences.size()))
			bits += -log2(labelModel.observe(MOTIF_SYMBOL));
		
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
	public double substitutions()
	{
		double bits = 0.0;
	
		// * Now to store the mapping from symbol to terminal for each
		//   occurrence of a symbol
		
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
	public DGraph<String> subbedGraph(List<Integer> wiring)
	{
		DGraph<String> copy = MapDTGraph.copy(graph);

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
	
	/**
	 * The cost of storing the tags. These are not taken as part of the structure
	 * and simply stored as a sequence once the rest of the graph has been encoded.
	 * @return
	 */
	public double tags()
	{
		// * Tags
		double tagBits = 1; // one bit to signal whether or not there are tags following
	
		if(graph instanceof TGraph<?, ?>)
		{
			TGraph<?, ?> tgraph = (TGraph<?, ?>)graph;
			// ** Tag set
			List<?> tagSet = new ArrayList<Object>(tgraph.tags());
			tagBits += new GZIPCompressor<List<Object>>().compressedSize(tagSet);
						
			// ** Tag sequence
			OnlineModel<Object> tagModel = new OnlineModel<Object>(  (Collection<Object>) tgraph.tags());
						
			for(TLink<?, ?> link : tgraph.links())
				tagBits += - Functions.log2(tagModel.observe(link.tag()));
		}
		
		return tagBits;
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
