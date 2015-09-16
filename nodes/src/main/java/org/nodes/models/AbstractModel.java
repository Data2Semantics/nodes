package org.nodes.models;

import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.LightDGraph;
import org.nodes.Node;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.compression.Functions;
import org.nodes.util.GZIPCompressor;
import org.nodes.util.OnlineModel;
import org.nodes.util.Compressor;
import org.nodes.util.Series;

public abstract class AbstractModel<N> implements Compressor<Graph<N>>
{	
	public static boolean storeLabels = true;
	
	public AbstractModel()
	{}
	
	public AbstractModel(boolean storeLabels)
	{
		this.storeLabels = storeLabels;
	}

	
	@Override
	public double compressedSize(Object... objects)
	{
		if(objects.length == 1)
			return compressedSize((Graph<N>) objects[0]);
		
		Graph<N> graph = new LightDGraph<N>();
		
		for(Object graphObject : objects)
			Graphs.add(graph, (Graph<N>) graphObject);
		
		return compressedSize(graph);
	}
	
	/**
	 * Computes a self-delimiting encoding of this graph
	 * 
	 * @param graph
	 */
	public double compressedSize(Graph<N> graph)
	{
		return compressedSize(graph, series(graph.size()));
	}
	
	/**
	 * Computes a self-delimiting encoding of this graph
	 * 
	 * @param graph
	 */
	public double compressedSize(Graph<N> graph, List<Integer> order)
	{
		// * Structure
		double structureBits = structureBits(graph, order);
		
		// * Labels
		double labelBits = 0;
		double tagBits = 0;

		if(storeLabels)
		{
			// ** Label set
			List<N> labelSet = new ArrayList<N>(graph.labels());
			
			GZIPCompressor<List<Object>> compressor = new GZIPCompressor<List<Object>>();
			labelBits += compressor.compressedSize(labelSet);
			
			// ** Label sequence
			OnlineModel<N> labelModel = new OnlineModel<N>(labelSet); 
					
			for(Node<N> node : graph.nodes())
				labelBits += - Functions.log2(labelModel.observe(node.label()));
					
			// * Tags
			tagBits = 1;
			if(graph instanceof TGraph<?, ?>)
			{
				TGraph<?, ?> tgraph = (TGraph<?, ?>)graph;
				// ** Tag set
				List<?> tagSet = new ArrayList<Object>(tgraph.tags());
				tagBits += compressor.compressedSize(tagSet);
							
				// ** Tag sequence
				OnlineModel<Object> tagModel = new OnlineModel<Object>(  (Collection<Object>) tgraph.tags());
							
				for(TLink<?, ?> link : tgraph.links())
					tagBits += - Functions.log2(tagModel.observe(link.tag()));
			}
		}
		return structureBits + labelBits + tagBits;
	}

	
	/**
	 * Computes a self-delimiting encoding of this graph
	 * 
	 * @param graph
	 * 
	 */
	public abstract double structureBits(Graph<N> graph, List<Integer> order);

	@Override
	public double ratio(Object... object)
	{
		throw new UnsupportedOperationException();
	}

}
