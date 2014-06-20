package org.nodes.compression;

import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.prefix;
import static org.nodes.compression.Functions.toc;
import static org.nodes.util.Functions.logFactorial;

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
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.util.BitString;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
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

	public static double size(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, Compressor<Graph<String>> comp)
	{
		List<Integer> wiring = new ArrayList<Integer>();
		DGraph<String> copy = subbedGraph(graph, sub, occurrences, wiring);

		double graphsBits = comp.compressedSize(sub); // TODO: we are slightly
														// cheating on the label
														// bits here.
		graphsBits += comp.compressedSize(copy);
		return graphsBits + wiringBits(wiring);
	}

	/**
	 * Computes the size quickly, with explicitly creating a subbed graph.
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @return
	 */
	public static double sizeFast(DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences)
	{
		double bits = 0;

		// * Store the motif
		Compressor<Graph<String>> comp = new EdgeListCompressor<String>();
		bits += comp.compressedSize(sub);

		// * Store the subbed graph

		// - This list holds the index of the occurrence the node belongs to
		List<Integer> inOccurrence = new ArrayList<Integer>(graph.size());
		for (int i : Series.series(graph.size()))
			inOccurrence.add(null);

		int replacedNodes = 0;

		for (int occIndex : Series.series(occurrences.size()))
		{
			for (Integer i : occurrences.get(occIndex))
			{
				inOccurrence.set(i, occIndex);
				replacedNodes++;
			}
		}

		OnlineModel<Integer> source = new OnlineModel<Integer>();
		OnlineModel<Integer> target = new OnlineModel<Integer>();

		// - observe all symbols

		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
			{
				source.add(node.index(), 0.0);
				target.add(node.index(), 0.0);
			}

		// - negative numbers represent symbol nodes
		for (int i : Series.series(1, occurrences.size() + 1))
		{
			source.add(-i, 0.0);
			target.add(-i, 0.0);
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

		OnlineModel<Object> tagModel = null;
		double tagBits = 0.0;
		if (graph instanceof TGraph<?, ?>)
		{
			tagModel = new OnlineModel<Object>();
			tagModel.add(((TGraph<?, ?>) graph).tags());
		}

		for (Link<String> link : graph.links())
		{
			Integer firstOcc = inOccurrence.get(link.first().index());
			Integer secondOcc = inOccurrence.get(link.second().index());

			if ((firstOcc == null && secondOcc == null)
					|| firstOcc != secondOcc)
			{
				int first = link.first().index();
				int second = link.second().index();

				first = inOccurrence.get(first) == null ? first
						: -(inOccurrence.get(first) + 1);
				second = inOccurrence.get(second) == null ? second
						: -(inOccurrence.get(second) + 1);

				double p = source.observe(first) * target.observe(second);
				bits += -Functions.log2(p);

				if (graph instanceof TGraph<?, ?>)
					tagBits += -log2(tagModel.observe(((TLink<?, ?>) link)
							.tag()));
			}
		}

		bits -= logFactorial(subbedNumLinks, 2.0);

		// - Subbed graph, rest of the bits (as done by AbstractGraphCompressor)

		// -- Labels
		double labelBits = 0;
		OnlineModel<String> labelModel = new OnlineModel<String>();

		labelModel.add(MOTIF_SYMBOL, 0.0);

		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
				labelBits += -log2(labelModel.observe(node.label()));

		for (int i : Series.series(occurrences.size()))
			labelBits += -log2(labelModel.observe(MOTIF_SYMBOL));

		bits += labelBits + tagBits;

		// * Record the wiring information

		OnlineModel<Integer> om = new OnlineModel<Integer>();
		om.symbols(Series.series(sub.size()));

		double wiringBits = 0;

		for (List<Integer> occurrence : occurrences)
		{
			Set<Integer> occSet = new HashSet<Integer>(occurrence);

			// * The index of the node within the occurrence
			int indexWithin = 0;
			for (int index : occurrence)
			{
				for (DNode<String> neighbor : graph.get(index).neighbors())
					if (!occSet.contains(neighbor.index()))
					{
						for (DLink<String> link : graph.get(index).linksOut(
								neighbor))
							wiringBits += -log2(om.observe(indexWithin));
						for (DLink<String> link : graph.get(index).linksIn(
								neighbor))
							wiringBits += -log2(om.observe(indexWithin));
					}

				indexWithin++;
			}
		}

		bits += wiringBits;

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
	public static double sizeSymbols(DGraph<String> graph, String symbol,
			DGraph<String> sub, List<List<Integer>> occurrences)
	{
		int numLabels = graph.labels().size();
		
		// * Store the graph, and the subgraph and the wiring
		double bits = sizeFast(graph, sub, occurrences);

		// * Now to store the mapping from symbol to terminal for each
		//   occurrence of a symbol
		// - Find the number of terminals per symbol
		List<Set<String>> terminalSets = new ArrayList<Set<String>>();
		List<Integer> indices = new ArrayList<Integer>();

		for (Node<String> node : sub.nodes())
			if (symbol.equals(node.label()))
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
			int index = indices.get(i);
			int n = terminalSets.size();

			// - store pointers to the relevant labels
			bits += prefix(terminalSets.size());
			bits += -log2(numLabels) *  terminalSets.size();
				
			OnlineModel<String> model = new OnlineModel<String>();
			model.symbols(terminalSets.get(i));

			for (List<Integer> occurrence : occurrences)
			{
				String label = graph.get(occurrence.get(index)).label();
				bits -= Functions.log2(model.observe(label));
			}
		}

		// * The string terminals aren't stored.

		return bits;

	}

	public static double wiringBits(List<Integer> wiring)
	{
		int max = Integer.MIN_VALUE;
		for (int i : wiring)
			max = Math.max(max, i);

		OnlineModel<Integer> om = new OnlineModel<Integer>();
		om.symbols(Series.series(max + 1));

		double bits = 0.0;
		for (int wire : wiring)
			bits += -log2(om.observe(wire));

		return bits;
	}

	public static DGraph<String> subbedGraph(DGraph<String> inputGraph,
			DGraph<String> sub, List<List<Integer>> occurrences,
			List<Integer> wiring)
	{
		DGraph<String> copy = MapDTGraph.copy(inputGraph);

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
	 * Substitute multiple motifs
	 * 
	 * @param inputGraph
	 * @param subs
	 * @param occurrencesMap
	 * @param wiring
	 * @return
	 */
	public static DGraph<String> subbedGraphMulti(DGraph<String> inputGraph,
			List<DGraph<String>> subs,
			Map<DGraph<String>, List<List<Integer>>> occurrencesMap,
			List<Integer> wiring)
	{
		int numSymbols = 0;
		DGraph<String> copy = MapDTGraph.copy(inputGraph);

		Set<Node<String>> toRemove = new LinkedHashSet<Node<String>>();

		for (DGraph<String> sub : subs)
		{

			String symbol = MOTIF_SYMBOL + (numSymbols++);
			List<List<Integer>> occurrences = occurrencesMap.get(sub);

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
			for (List<DNode<String>> nodes : occ)
			{
				// if(i % 100 == 0)
				// Global.log().info("Multi: starting " + i + " of "+ totalOcc +
				// " occurrences ("+String.format("%.2f", ((100.0 *
				// i)/totalOcc))+"%). ");

				if (alive(nodes, toRemove)) // -- make sure none of the nodes of
											// the occurrence
											// been removed. If two occurrences
											// overlap,
											// only the first gets replaced.
				{
					// * Wire a new symbol node into the graph to represent the
					// occurrence
					DNode<String> newNode = copy.add(symbol);

					int indexInSubgraph = 0;
					for (DNode<String> node : nodes)
					{
						for (DNode<String> neighbor : node.neighbors())
							if (!nodes.contains(neighbor))
							{
								for (DLink<String> link : node
										.linksOut(neighbor))
									newNode.connect(neighbor);
								for (DLink<String> link : node
										.linksIn(neighbor))
									neighbor.connect(newNode);

								wiring.add(indexInSubgraph);
							}

						indexInSubgraph++;
					}
				}
				i++;
			}
		}

		for (Node<String> node : toRemove)
			node.remove();

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
