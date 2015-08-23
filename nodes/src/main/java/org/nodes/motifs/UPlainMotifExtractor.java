package org.nodes.motifs;

import static org.nodes.motifs.MotifCompressor.exDegree;
import static org.nodes.util.Series.series;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.compression.EdgeListCompressor;
import org.nodes.compression.Functions;
import org.nodes.compression.NeighborListCompressor;
import org.nodes.compression.Functions.NaturalComparator;
import org.nodes.models.USequenceModel;
import org.nodes.models.USequenceModel.CIMethod;
import org.nodes.models.USequenceModel.CIType;
import org.nodes.random.SubgraphGenerator;
import org.nodes.util.AbstractGenerator;
import org.nodes.util.BitString;
import org.nodes.util.Compressor;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Generator;
import org.nodes.util.Generators;
import org.nodes.util.Order;
import org.nodes.util.Series;

import com.itextpdf.text.log.SysoLogger;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Extracts motifs from a UGraph<String> by sampling.
 * 
 * This extractor does not perform masking. It simply returns the subgraphs 
 * that best compress the structure. If labels are to be ignored, the graph 
 * should be blanked beforehand 
 * 
 * @author Peter
 *
 */
public class UPlainMotifExtractor<L extends Comparable<L>>
{
	private static final boolean SPECIFY_SUBS = true;
	private static final int MIN_OCCURRENCES = 10;
	private static final int MAX_MOTIFS = 10;
	private static final boolean CORRECT_FREQUENCIES = true;

	private UGraph<L> data;
	private int samples;

	private Functions.NaturalComparator<L> comparator;

	private List<UGraph<L>> tokens;

	private Generator<Integer> intGen;
	
	private MotifVarTags mvTop = null;
	
	private FrequencyModel<UGraph<L>> fm;
	private Map<UGraph<L>, List<List<Integer>>> occurrences;
	private int minFreq;
	
	
	public UPlainMotifExtractor(
			UGraph<L> data,
			int numSamples,
			int minSize,
			int maxSize)
	{
		this(data, numSamples, minSize, maxSize, 0);
	}
	
	
	public UPlainMotifExtractor(
			UGraph<L> data,
			int numSamples,
			int minSize,
			int maxSize,
			int minFreq)
	{	
		this.data = data;
		this.samples = numSamples;
		this.minFreq = minFreq;
		
		comparator = new Functions.NaturalComparator<L>();
		intGen = Generators.uniform(minSize, maxSize + 1);
		
		run();
	}
	
	public UPlainMotifExtractor(
			UGraph<L> data,
			int numSamples,
			int size)
	{
		this.data = data;
		this.samples = numSamples;
		
		comparator = new Functions.NaturalComparator<L>();
		intGen = Generators.uniform(size, size+1);
		
		run();
	}

	private void run()
	{
		Global.log().info("Sampling motifs");		
		fm = new FrequencyModel<UGraph<L>>();

		// * The (overlapping) instances
		occurrences = new LinkedHashMap<UGraph<L>, List<List<Integer>>>();

		SubgraphGenerator<L> gen = 
			new SubgraphGenerator<L>(data, intGen, Collections.EMPTY_LIST);

		Global.log().info("Start sampling.");
		for (int i : Series.series(samples))
		{
			if (i % 10000 == 0)
				System.out.println("Samples finished: " + i);

			SubgraphGenerator<L>.Result result = gen.generate();
			
			UGraph<L> sub = Subgraph.uSubgraphIndices(data, result.indices());

			// * Reorder nodes to canonical ordering
			Order canonical = Nauty.order(sub, comparator);
			sub = Graphs.reorder(sub, canonical);
			
			List<Integer> occurrence = canonical.apply(result.indices()); 
			
			fm.add(sub); // no need for a correction as in normal motif sampling
			
			// * record the occurrence
			if (!occurrences.containsKey(sub))
				occurrences.put(sub, new ArrayList<List<Integer>>());

			occurrences.get(sub).add(occurrence);
		}
		
		Global.log().info("Removing overlapping occurrences.");
		// * Remove overlapping occurrences 
		//   (keep the ones with the lowest exdegrees)
		FrequencyModel<UGraph<L>> newFm = 
				new FrequencyModel<UGraph<L>>();
		Map<UGraph<L>, List<List<Integer>>> newOccurrences = 
				new LinkedHashMap<UGraph<L>, List<List<Integer>>>();
		
		for(UGraph<L> sub : fm.tokens())
			if(fm.frequency(sub) >= minFreq)
			{
				// * A map from nodes to occurrences containing them
				Map<UNode<L>, List<Occurrence>> map = 
					new LinkedHashMap<UNode<L>, List<Occurrence>>();
				
				// * A list of all occurrences, sorted by exDegree
				LinkedList<Occurrence> list = new LinkedList<Occurrence>();
				
				// - fill the map and list
				for(List<Integer> occurrence : occurrences.get(sub))
				{
					Occurrence occ = new Occurrence(occurrence);
					list.add(occ);
					
					for(int index : occurrence)
					{
						UNode<L> node = data.get(index);
						
						if(! map.containsKey(node))
							map.put(node, new ArrayList<Occurrence>());	 
						map.get(node).add(occ);
					}
				}
				
				Collections.sort(list);
				
				while(!list.isEmpty())
				{
					// * Find the first living occurrence, remove any dead ones
					Occurrence head = list.poll();
					
					if(head.alive()) // * register it as a viable occurrence
					{
						newFm.add(sub);
						if(! newOccurrences.containsKey(sub))
							newOccurrences.put(sub, new ArrayList<List<Integer>>());
						newOccurrences.get(sub).add(head.indices());
						
						// - now kill any occurrence that shares a node with this one.
						for(int nodeIndex : head.indices())
							for(Occurrence occ : map.get(data.get(nodeIndex)))
								occ.kill();
					} 
				}
			}
		
		fm = newFm;
		occurrences = newOccurrences;
		
		Global.log().info("Finished sampling motifs and removing overlaps.");

		tokens = fm.sorted();
	}
	
	public List<UGraph<L>> subgraphs()
	{
		return fm.sorted();
	}
	
	public List<List<Integer>> occurrences(UGraph<L> subgraph)
	{
		return occurrences.get(subgraph);
	}

	private class Occurrence implements Comparable<Occurrence> {
		
		private boolean alive = true;
		private List<Integer> indices;
		private int id;
		
		private int exDegree;
	
		public Occurrence(List<Integer> indices)
		{
			this.indices = indices;
			
			exDegree = MotifCompressor.exDegree(data, indices);
		}
		
		public boolean alive()
		{
			return alive;
		}
		
		public void kill()
		{
			alive = false;
		}
		
		public List<Integer> indices()
		{
			return indices;
		}
		
		public int exDegree()
		{
			return exDegree;
		}

		@Override
		public int compareTo(Occurrence other)
		{
			return Integer.compare(exDegree, other.exDegree);
		}
		

		
	}

	public double frequency(UGraph<L> sub)
	{
		return fm.frequency(sub);
	}
	
}
