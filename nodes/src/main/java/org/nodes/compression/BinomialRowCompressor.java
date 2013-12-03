package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.logChoose;

import java.util.List;

import org.nodes.Global;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.draw.Draw;

import org.nodes.util.Series;

public class BinomialRowCompressor<N> extends AbstractGraphCompressor<N>
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
		int n = graph.size();		
		int k = graph.numLinks();

		List<Integer> inv = Draw.inverse(order);
		
		long bits = 0;
		
		bits+= prefix(n);
		
		int entries = (n * (n + 1)) / 2;
		bits += log2(entries); // Encode total number of ones (k)
		
		int row = 0;
		
		for(int index : Series.series(inv.size()))
		{
			Node<N> node = graph.nodes().get(inv.get(index));
			int backDegree = 0;
			for(Node<N> neighbor : node.neighbors()) // BUG?
				if(order.get(neighbor.index()) <= order.get(node.index()))
					backDegree++;
				
			bits += log2(row + 1) + logChoose(backDegree, row + 1, 2.0);
			row ++;
		}
		
		return bits;
	}
	
	public double directed(DGraph<N> graph, List<Integer> order)
	{
		int n = graph.size();
		int k = graph.numLinks();
				
		double sizeBits = prefix(n);
		sizeBits += 2 * log2(n);

		double kBits = logChoose(k, k + n - 1);
		
		double rowBits = 0.0;
		for(DNode<N> node : graph.nodes())
			rowBits += logChoose(node.linksIn().size(), n, 2.0); // TODO: stars and bars
		
		Global.log().info("sizeBits: "+sizeBits+", kBits: "+kBits+", rowBits "+rowBits+". ");
		
		return sizeBits + kBits + rowBits;
	}
	
	
}
