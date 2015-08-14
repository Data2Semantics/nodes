package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;

import java.util.List;

import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.FrequencyModel;

/**
 * TODO: Check for self-loops, use one bit to encode
 * @author Peter
 *
 * @param <N>
 */
public class UniformCompressor<N> extends AbstractGraphCompressor<N>
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
	
	public double undirected(Graph<N> graph)
	{
		int n = graph.size();
		return prefix(n) + n * (n + 1) / 2; 
	}
	
	public double directed(DGraph<N> graph)
	{
		int n = graph.size();
		return prefix(n) + n * n;
	}
}
