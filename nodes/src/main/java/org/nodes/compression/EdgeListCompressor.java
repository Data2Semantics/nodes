package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.logFactorial;

import java.util.List;

import org.nodes.Global;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Compressor;

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
		FrequencyModel<Node<N>> model = new FrequencyModel<Node<N>>();
		
		double bits = 0;
		
		bits += prefix(graph.size());
		bits += prefix(graph.numLinks());
		
		for(Node<N> node : graph.nodes())
			model.add(node, 0.0);
				
		for(Link<N> link : graph.links())
		{
			double p = p(link.first(), model) * p(link.second(), model);
			if(! link.first().equals(link.second()))
				p *= 2.0;
			
			model.add(link.first());
			model.add(link.second());
			
			bits += -log2(p);
		}
		
		int l = graph.numLinks();
		// Global.log().info("source model entropy = "+model.entropy()+", logFact per line = "+logFactorial(l, 2.0)/(double)l+", log l = " + (Math.log(l) - 1)/Math.log(2.0));
		
		return bits - logFactorial(graph.numLinks(), 2.0);
	}	
	
	public double directed(Graph<N> graph, List<Integer> order)
	{
		
		FrequencyModel<Node<N>> source = new FrequencyModel<Node<N>>();
		FrequencyModel<Node<N>> target = new FrequencyModel<Node<N>>();

		
		double bits = 0;
		
		bits += prefix(graph.size());
		bits += prefix(graph.numLinks());
		
		for(Node<N> node : graph.nodes())
		{
			source.add(node, 0.0);
			target.add(node, 0.0);
		}
				
		for(Link<N> link : graph.links())
		{
			double p = p(link.first(), source) * p(link.second(), target);
			
			source.add(link.first());
			target.add(link.second());
			
			bits += -log2(p);
		}
		
		bits -= logFactorial(graph.numLinks(), 2.0);
		
		int l = graph.numLinks();
		// Global.log().info("source model entropy = "+source.entropy()+", target model entropy = "+target.entropy()+", logFact per line = "+logFactorial(l, 2.0)/(double)l+", log l = " + (Math.log(l) - 1)/Math.log(2.0));
		
		return bits;
	}	
	
	private static <N> double p(N symbol, FrequencyModel<N> model)
	{
		double freq = model.frequency(symbol),
		       total = model.total(),
		       distinct = model.distinct();
			
		// TODO parametrize smoothing
		return (freq + 0.5) / (total + 0.5 * distinct);
	}
}
