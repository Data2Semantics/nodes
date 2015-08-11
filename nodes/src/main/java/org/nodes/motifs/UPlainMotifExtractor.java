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
 * This extractor does not consider the node labels: ie. it does not perform 
 * masking. It simply returns the subgraphs that best compress the structure.
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
	
	
	public UPlainMotifExtractor(
			UGraph<L> data,
			int numSamples,
			int minSize,
			int maxSize)
	{
		this.data = data;
		this.samples = numSamples;
		
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
		Map<UGraph<L>, Set<Occurrence>> newOccurrences = 
				new LinkedHashMap<UGraph<L>, Set<Occurrence>>();
		
		for(UGraph<L> sub : fm.tokens())
		{
			// * A map from nodes to occurrences containing them
			Map<UNode<L>, List<Occurrence>> map = 
				new LinkedHashMap<UNode<L>, List<Occurrence>>();
			
			// - fill the map
			for(List<Integer> occurrence : occurrences.get(sub))
				for(int index : occurrence)
				{
					UNode<L> node = data.get(index);
					if(! map.containsKey(node))
						map.put(node, new ArrayList<Occurrence>());
						
					map.get(node).add(new Occurrence(occurrence));
				}
			
			// - sort the occurrences by exdegree
			Comparator<List<Integer>> comp = MotifCompressor.exDegreeComparator(data);
			for(List<Occurrence> list : map.values())
				Collections.sort(list, new ExDegreeComparator<L>(data));
			
			// * Now iterate over all occurrences for each node. Keep the first 
			//   one that hasn't been killed yet, and kill the rest
			for(UNode<L> node : map.keySet())
			{
				// - Find the first living Occurrence
				Occurrence living = null;
				Iterator<Occurrence> it = map.get(node).iterator();
				while(it.hasNext())
				{
					living = it.next();
					if(living.alive())
						break;
				}
				
				if(living != null)
				{
					if(! newOccurrences.containsKey(sub))
						newOccurrences.put(sub, new LinkedHashSet<Occurrence>());
					
					newOccurrences.get(sub).add(living);
					newFm.add(sub);
				}
					
				// - kill the rest
				while(it.hasNext())
					it.next().kill();
			}

		}
		
		fm = newFm;
		
		// - get rid of the Occurrence objects, return to lists of Integers
		occurrences.clear();
		for(UGraph<L> sub : newOccurrences.keySet())
		{
			List<List<Integer>> value = new ArrayList<List<Integer>>(newOccurrences.get(sub).size());
			
			for(Occurrence occ :  newOccurrences.get(sub))
				value.add(occ.indices());
			
			occurrences.put(sub, value);
		}
		
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

	private static class Occurrence {
		private static int numObjects = 0;
		private static List<Occurrence> objects = new ArrayList<Occurrence>();
		
		private boolean alive = true;
		private List<Integer> indices;
		private int id;
	
		public Occurrence(List<Integer> indices)
		{
			this.indices = indices; 
			id = numObjects++;
			
			objects.add(this);
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
		
		public static Occurrence get(int id)
		{
			return objects.get(id);
		}
		
	}
	
	private static class ExDegreeComparator<L> implements Comparator<Occurrence>
	{
		private Graph<L> data;

		public ExDegreeComparator(Graph<L> data)
		{
			this.data = data;
		}

		@Override
		public int compare(Occurrence a, Occurrence b)
		{
			return Integer.compare(exDegree(data, a.indices()),  exDegree(data, b.indices())); 
		}
	}
	
}
