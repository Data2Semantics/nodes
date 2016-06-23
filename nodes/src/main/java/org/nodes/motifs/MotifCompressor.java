package org.nodes.motifs;

import static nl.peterbloem.kit.Functions.logFactorial;
import static nl.peterbloem.kit.Functions.prefix;
import static nl.peterbloem.kit.Series.series;
import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.toc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.nodes.FastWalkable;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.compression.AbstractGraphCompressor;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.util.Compressor;
import org.nodes.util.GZIPCompressor;

import nl.peterbloem.kit.BitString;
import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.OnlineModel;
import nl.peterbloem.kit.Series;

/**
 * 
 * @author Peter
 *
 */
public class MotifCompressor extends AbstractGraphCompressor<String>
{
	public static final String MOTIF_SYMBOL = "|M|";

	
	private int samples;
	private int minSize, maxSize;

	public MotifCompressor()
	{

	}

	/**
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @param storeLabels Whether to explicitly store the node labels. If false, a 
	 * distinguishing code will still be stored for each node, but the actual string
	 * will not be recoverable. If the nodes should also not be distinguishable by 
	 * label, the graph should be blanked first.
	 * @param comp
	 * @return
	 */
	public static double size(
			DGraph<String> graph, DGraph<String> sub,
			List<List<Integer>> occurrences, boolean storeLabels,
			Compressor<Graph<String>> comp)
	{
		List<List<Integer>> wiring = new ArrayList<List<Integer>>();
		
		DGraph<String> copy = subbedGraph(graph, sub, occurrences, wiring);

		double graphsBits = 0.0;
		
		if(storeLabels)
		{
			double labels = labelSets(graph); 
			System.out.println("***     labels: " + labels);
			graphsBits += labels;
		}
		
		double motif = motifSize(graph, sub);
		graphsBits += motif;
		
		double silhouette = silhouetteSize(graph, copy);
		graphsBits += silhouette;
		
		double wiringBits = wiringBits(wiring, sub);
		graphsBits += wiringBits;
		
		System.out.println("***      motif: " + motif);
		System.out.println("*** silhouette: " + silhouette);
		System.out.println("***     wiring: " + wiringBits);

		return labelSets(graph) + graphsBits;
	}
	
	/**
	 * The cost of storing the motif
	 * 
	 * @param graph
	 * @param sub
	 * @return
	 */
	public static double motifSize(
			DGraph<String> graph, DGraph<String> sub)
	{
		double bits = 0.0;
		
		// * Store the structure
		bits += EdgeListCompressor.directed(sub); 
		
		// * Store the labels
		OnlineModel<String> model = new OnlineModel<String>(graph.labels());
		
		for(DNode<String> node : sub.nodes())
			bits += - Functions.log2(model.observe(node.label())) ;
		
		return bits;
	}
	
	/**
	 * TODO: pass only labels, not the graph
	 * 
	 * @param graph
	 * @param subbed
	 * @return
	 */
	public static double silhouetteSize(
			DGraph<String> graph, DGraph<String> subbed)
	{
		double bits = 0.0;
		
		// * Store the structure
		bits += EdgeListCompressor.directed(subbed); 
		
		// * Store the labels
		OnlineModel<String> model = new OnlineModel<String>(graph.labels());
		model.add(MOTIF_SYMBOL, 0.0);
		
		for(DNode<String> node : subbed.nodes())
			bits += - Functions.log2(model.observe(node.label())) ;
		
		return bits;
	}
	
	/**
	 * Bits required to store all labels once.
	 * 
	 * @param graph
	 * @return
	 */
	public static double labelSets(
			DGraph<String> graph)
	{
		// * Labels
		double labelBits = 0;

		// ** Label set
		List<String> labelSet = new ArrayList<String>(graph.labels());
		
		GZIPCompressor<List<Object>> compressor = new GZIPCompressor<List<Object>>();
		labelBits += compressor.compressedSize(labelSet);
		
		// * Tags
		double tagBits = 0;
	
		if(graph instanceof TGraph<?, ?>)
		{
			TGraph<?, ?> tgraph = (TGraph<?, ?>)graph;
			// ** Tag set
			List<?> tagSet = new ArrayList<Object>(tgraph.tags());
			tagBits += compressor.compressedSize(tagSet);
		}
				
		return labelBits + tagBits;
	}

	/**
	 * Computes the size quickly, without explicitly creating a subbed graph.
	 * 
	 * @param graph
	 * @param sub
	 * @param occurrences
	 * @return
	 */
	public static double sizeFast(
			DGraph<String> graph, 
			DGraph<String> sub,
			List<List<Integer>> occurrences)
	{
		double bits = 0;

		bits += labelSets(graph);
		
		// * Store the motif
		bits += motifSize(graph, sub);

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

		OnlineModel<Integer> source = new OnlineModel<Integer>(Collections.EMPTY_LIST);
		OnlineModel<Integer> target = new OnlineModel<Integer>(Collections.EMPTY_LIST);

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
		bits += Functions.prefix(graph.size() - replacedNodes + occurrences.size());
		// Num links in the subbed graph
		bits += Functions.prefix(subbedNumLinks);

		OnlineModel<Object> tagModel = null;
		double tagBits = 0.0;
		if (graph instanceof TGraph<?, ?>)
			tagModel = new OnlineModel<Object>( (Collection<Object>) ((TGraph<?, ?>) graph).tags() );

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
		// -- Structure is now stored
		// - Subbed graph, bits for the label sequences (as done by AbstractGraphCompressor)

		// -- Labels
		double labelBits = 0;
		
		OnlineModel<String> labelModel = new OnlineModel<String>(graph.labels());
		labelModel.addToken(MOTIF_SYMBOL);

		for (Node<String> node : graph.nodes())
			if (inOccurrence.get(node.index()) == null)
				labelBits += -log2(labelModel.observe(node.label()));

		for (int i : Series.series(occurrences.size()))
			labelBits += -log2(labelModel.observe(MOTIF_SYMBOL));

		bits += labelBits + tagBits;

		// * Record the wiring information

		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));
		
		double wiringBits = 0.0;

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
						for (DLink<String> link : graph.get(index).linksOut(neighbor))
							wiringBits += -log2(om.observe(indexWithin));
						for (DLink<String> link : graph.get(index).linksIn(neighbor))
							wiringBits += -log2(om.observe(indexWithin));
					}

				indexWithin++;
			}
		}

		bits += wiringBits;

		return bits;
	}

	public static double wiringBits(List<List<Integer>> wiring, Graph<String> sub)
	{
		// TODO: Add reset
		OnlineModel<Integer> om = new OnlineModel<Integer>(Series.series(sub.size()));

		double bits = 0.0;
		for(List<Integer> motif : wiring)
			for (int wire : motif)
				bits += -log2(om.observe(wire));

		return bits;
	}

	/**
	 * Create a copy of the input graph with all (known) occurrences of the 
	 * given subgraph replaced by a single node.
	 * 
	 * @param inputGraph
	 * @param sub
	 * @param occurrences
	 * @param wiring
	 * @return
	 */
	public static DGraph<String> subbedGraph(
			DGraph<String> inputGraph,
			DGraph<String> sub, 
			List<List<Integer>> occurrences,
			List<List<Integer>> wiring)
	{
		// * Create a copy of the input.
		//   We will re-purpose node 0 of each occurrence as the new instance 
		//   node, so for those nodes, we set the label to "|M|"
		DGraph<String> copy = new MapDTGraph<String, String>();
		
		Set<Integer> firstNodes = new HashSet<Integer>();
		for(List<Integer> occurrence : occurrences)
			firstNodes.add(occurrence.get(0));
		
		// -- copy the nodes
		for (DNode<String> node : inputGraph.nodes())
			if(firstNodes.contains(node.index()))
				copy.add(MOTIF_SYMBOL);
			else
				copy.add(node.label());
		
		// -- copy the links
		for (DLink<String> link : inputGraph.links())
		{
			int i = link.from().index(), 
			    j = link.to().index();
			
			copy.get(i).connect(copy.get(j));
		}

		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<DNode<String>>> occ = 
				new ArrayList<List<DNode<String>>>(occurrences.size());
		
		for (List<Integer> occurrence : occurrences)
		{
			List<DNode<String>> nodes = 
					new ArrayList<DNode<String>>(occurrence.size());
			for (int index : occurrence)
				nodes.add(copy.get(index));
			
			occ.add(nodes);
		}
		
		for (List<DNode<String>> occurrence : occ)
		{
			if (alive(occurrence)) // -- make sure none of the nodes of the
								   // occurrence have been removed. If two 
								   // occurrences overlap,
								   // only the first gets replaced.
			{
				// * Use the first node of the motif as the symbol node
				DNode<String> newNode = occurrence.get(0);

				// - This will hold the information how each edge into the motif node should be wired
				//   into the motif subgraph (to be encoded later)
				List<Integer> motifWiring = new ArrayList<Integer>(occ.size());
				wiring.add(motifWiring);
				
				for (int indexInSubgraph : series(occurrence.size()))
				{
					DNode<String> node = occurrence.get(indexInSubgraph);
					
					for(DLink<String> link : node.links())
					{
						// If the link is external
						DNode<String> neighbor = link.other(node);
						
						if(! occurrence.contains(neighbor))
						{
							if(!node.equals(newNode))
							{
								if(link.from().equals(node))
									newNode.connect(neighbor);
								else
									neighbor.connect(newNode);
							}
						
							motifWiring.add(indexInSubgraph);
						}
					}
				}

				for (int i : series(1, occurrence.size()))
					occurrence.get(i).remove();
			}
		}

		return copy;
	}
	
	/**
	 * Create a copy of the input graph with all given occurrences of the 
	 * given subgraph replaced by a single node.
	 * 
	 * @param inputGraph
	 * @param sub
	 * @param occurrences
	 * @param wiring
	 * @return
	 */
	public static UGraph<String> subbedGraph(
			UGraph<String> inputGraph,
			UGraph<String> sub, 
			List<List<Integer>> occurrences,
			List<List<Integer>> wiring)
	{
		// * Create a copy of the input.
		//   We will re-purpose node 0 of each occurrence as the new instance 
		//   node, so for those nodes, we set the label to "|M|"
		UGraph<String> copy = new MapUTGraph<String, String>();
		
		Set<Integer> firstNodes = new HashSet<Integer>();
		for(List<Integer> occurrence : occurrences)
			firstNodes.add(occurrence.get(0));
		
		// -- copy the nodes
		for (UNode<String> node : inputGraph.nodes())
			if(firstNodes.contains(node.index()))
				copy.add(MOTIF_SYMBOL);
			else
				copy.add(node.label());
		
		// -- copy the links
		for (ULink<String> link : inputGraph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.get(i).connect(copy.get(j));
		}
		
		// * Translate the occurrences from integers to nodes (in the copy)
		List<List<UNode<String>>> occ = 
				new ArrayList<List<UNode<String>>>(occurrences.size());
		
		for (List<Integer> occurrence : occurrences)
		{
			List<UNode<String>> nodes = 
					new ArrayList<UNode<String>>(occurrence.size());
			for (int index : occurrence)
				nodes.add(copy.get(index));
			
			occ.add(nodes);
		}
		
		for (List<UNode<String>> occurrence : occ)
		{
			if (alive(occurrence)) // -- make sure none of the nodes of the
								   // occurrence have been removed. If two 
								   // occurrences overlap,
								   // only the first gets replaced.
			{
				// * Wire a new symbol node into the graph to represent the occurrence
				UNode<String> newNode = occurrence.get(0);

				// - This will hold the information how each edge into the motif node should be wired
				//   into the motif subgraph (to be encoded later)
				List<Integer> motifWiring = new ArrayList<Integer>();
				wiring.add(motifWiring);
				
				for (int indexInSubgraph : series(occurrence.size()))
				{
					UNode<String> node = occurrence.get(indexInSubgraph);
					
					for(ULink<String> link : node.links())
					{
						UNode<String> neighbor = link.other(node);
						
						if(! occurrence.contains(neighbor))	// If the link is external
						{
							if(!node.equals(newNode))
								newNode.connect(neighbor);

							motifWiring.add(indexInSubgraph);
						}
					}
				}

				for (int i : series(1, occurrence.size()))
					occurrence.get(i).remove();
			}
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

	@Override
	public double structureBits(Graph<String> graph, List<Integer> order)
	{
		return -1.0;
	}

	/**
	 * NB: This is currently only correct for graphs without multiple edges !! 
	 * @param graph
	 * @param occurrence
	 * @return
	 */
	public static <L> int exDegree(Graph<L> graph, List<Integer> occurrence)
	{
		Set<Integer> occSet = new HashSet<Integer>(occurrence);
		
		if(graph instanceof FastWalkable<?, ?>)
		{
			FastWalkable<L, Node<L>> g = (FastWalkable<L, Node<L>>)graph;
			
			int sum = 0;
			List<Integer> neighbs = new ArrayList<Integer>();
			
			for (int i : Series.series(occurrence.size()))
			{
				int nodeIndex = occurrence.get(i);
				Node<L> node = graph.get(nodeIndex);
		
				for(Node<L> neighb : g.neighborsFast(node))
					if (!occSet.contains(neighb.index()))
						sum++;
			}
			
			return sum;
		}
		
		int sum = 0;
	
		for (int i : Series.series(occurrence.size()))
		{
			int nodeIndex = occurrence.get(i);
			Node<L> node = graph.get(nodeIndex);
	
			for (Node<L> neighbor : node.neighbors())
				if (!occSet.contains(neighbor.index()))
					sum++;
		}
	
		return sum;
	}
	
	public static<L> Comparator<List<Integer>> exDegreeComparator(Graph<L> data)
	{
		return new ExDegreeComparator<L>(data);
	}
	
	private static class ExDegreeComparator<L> implements Comparator<List<Integer>>
	{
		private Graph<L> data;

		public ExDegreeComparator(Graph<L> data)
		{
			this.data = data;
		}

		@Override
		public int compare(List<Integer> a, List<Integer> b)
		{
			return Integer.compare(exDegree(data, a),  exDegree(data, b)); 
		}
	}
}
