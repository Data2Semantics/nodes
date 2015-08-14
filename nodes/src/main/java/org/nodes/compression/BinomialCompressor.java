package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Choose;

import java.util.List;

import org.nodes.Global;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;

public class BinomialCompressor<N> extends AbstractGraphCompressor<N>
{
	@Override
	public double structureBits(Graph<N> graph, List<Integer> order)
	{
		if(graph instanceof UGraph<?>)
			return undirected((UGraph<N>) graph);
		
		if(graph instanceof DGraph<?>)
			return directed((DGraph<N>) graph);
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
	}
	
	public static <N> double undirected(Graph<N> graph)
	{
		int n = graph.size();
		int t = n * (n + 1) / 2;
		
		return prefix(n) + log2(t) + log2Choose(graph.numLinks(), t);
	}
	
	public static <N> double directed(DGraph<N> graph) 
	{
		double n = graph.size();
		double t = n * (double) n;
		
		Global.log().info("Choose bits: " +  log2Choose(graph.numLinks(), t));
		
		return prefix(graph.size()) + log2(t) + log2Choose(graph.numLinks(), t);
	}
}
