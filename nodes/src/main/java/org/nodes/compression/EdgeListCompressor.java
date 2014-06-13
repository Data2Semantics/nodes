package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.logFactorial;
import static org.nodes.util.Series.series;

import java.util.List;

import org.nodes.Global;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Compressor;
import org.nodes.util.OnlineModel;
import org.nodes.util.Series;

public class EdgeListCompressor<N> extends AbstractGraphCompressor<N>
{

	@Override
	public double structureBits(Graph<N> graph, List<Integer> order)
	{
		if(graph instanceof UGraph<?>)
			return undirected((UGraph<N>) graph, order);
		
		if(graph instanceof DGraph<?>)
			return directed((DGraph<N>) graph, order);
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
	}
	
	public double undirected(Graph<N> graph, List<Integer> order)
	{
		System.out.println("aaa " + graph.size());
		OnlineModel<Integer> model = new OnlineModel<Integer>();
		
		double bits = 0;
		
		bits += prefix(graph.size());
		bits += prefix(graph.numLinks());
		
		model.symbols(Series.series(graph.size()));
				
		for(Link<N> link : graph.links())
		{
			double p = model.observe(link.first().index()) * model.observe(link.second().index());
			if(! link.first().equals(link.second()))
				p *= 2.0;
			
			bits += -log2(p);
		}
		
		return bits - logFactorial(graph.numLinks(), 2.0);
	}	
	
	public double directed(Graph<N> graph, List<Integer> order)
	{
		
		OnlineModel<Integer> source = new OnlineModel<Integer>();
		OnlineModel<Integer> target = new OnlineModel<Integer>();

		
		double bits = 0;
		
		bits += prefix(graph.size());
		bits += prefix(graph.numLinks());
				
		source.symbols(series(graph.size()));
		target.symbols(series(graph.size()));
		
				
		for(Link<N> link : graph.links())
		{
			double p = 
				source.observe(link.first().index()) * 
				target.observe(link.second().index());
						
			bits += -log2(p);
		}
		
		bits -= logFactorial(graph.numLinks(), 2.0);
		
		return bits;
	}	
}
