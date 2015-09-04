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
	public EdgeListCompressor()
	{	
		super(true);
	}
	
	public EdgeListCompressor(boolean storeLabels)
	{	
		super(storeLabels);
	}

	@Override
	public double structureBits(Graph<N> graph, List<Integer> order)
	{
		// -- We ignore the order, since this method returns the same value for 
		//    all orderings
		
		if(graph instanceof UGraph<?>)
			return undirected((UGraph<N>) graph);
		
		if(graph instanceof DGraph<?>)
			return directed((DGraph<N>) graph);
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
	}
	
	public static <N> double undirected(Graph<N> graph)
	{
		return undirected(graph, true);
	}	
	public static <N> double undirected(Graph<N> graph, boolean withPrior)
	{

		OnlineModel<Integer> model = new OnlineModel<Integer>(Series.series(graph.size()));
		
		double bits = 0;
		
		if(withPrior)
		{
			bits += prefix(graph.size());
			bits += prefix(graph.numLinks());
		}
						
		for(Link<N> link : graph.links())
		{
			double p = model.observe(link.first().index()) * model.observe(link.second().index());
			if(! link.first().equals(link.second()))
				p *= 2.0;
			
			bits += -log2(p);
		}
		
		return bits - logFactorial(graph.numLinks(), 2.0);
	}	

	public static <N> double directed(DGraph<N> graph)
	{
		return directed(graph, true);
	}
	
	public static <N> double directed(DGraph<N> graph, boolean withPrior)
	{
		OnlineModel<Integer> source = new OnlineModel<Integer>(series(graph.size()));
		OnlineModel<Integer> target = new OnlineModel<Integer>(series(graph.size()));
		
		double bits = 0;
		
		if(withPrior)
		{
			bits += prefix(graph.size());
			bits += prefix(graph.numLinks());
		}

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
