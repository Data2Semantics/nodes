package org.nodes.compression;

import static org.nodes.compression.Functions.prefix;
import static org.nodes.util.Functions.log2;

import java.util.List;

import org.nodes.Global;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.draw.Draw;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Series;


/**
 * 
 * * Why does the neighborlist compressor suddenly work better?
 * * We should figure out storing labels and label subests properly
 * * We should include 
 *  
 * @author Peter
 *
 * @param <N>
 */
public class NeighborListCompressor<N> extends AbstractGraphCompressor<N>
{

	@Override
	public double structureBits(Graph<N> graph, List<Integer> order)
	{
		if(graph instanceof UGraph<?>)
			return size((UGraph<N>) graph, order, false);
		
		if(graph instanceof DGraph<?>)
			return size((DGraph<N>) graph, order, true);
		
		throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
	}
	
	public double size(Graph<N> graph, List<Integer> order, boolean directed)
	{
		FrequencyModel<Node<N>> nodes = new FrequencyModel<Node<N>>();
		FrequencyModel<Boolean> directions = new FrequencyModel<Boolean>();
		FrequencyModel<Boolean> delimiter = new FrequencyModel<Boolean>();
		
		List<Integer> inv = Draw.inverse(order);
		
		double bits = 0;
		double pBits = 0;
		
		bits += prefix(graph.size()); 
		for(Node<N> node : graph.nodes())
			nodes.add(node, 0.0);
		
		directions.add(true, 0);
		directions.add(false, 0);
		
		delimiter.add(true, 0);
		delimiter.add(false, 0);
		
		for(int index : Series.series(inv.size()))
		{
			Node<N> node = graph.nodes().get(inv.get(index));
			int size = 0;
			for(Node<N> neighbor : node.neighbors())
			{
				if(order.get(neighbor.index()) <= order.get(node.index()))
				{
					if(size == 0)
					{
						bits += -log2(p(false, delimiter));
						delimiter.add(false);
					}
						
					size++;
					
					// * Encode the reference to the neighboring node
					bits += - log2(p(neighbor, nodes));
					nodes.add(neighbor);
					
					if(directed) // * We encode the direction as an on-line binomial model
					{
						boolean direction = node.connected(neighbor);
						
						bits += - log2(p(direction, directions));
						directions.add(direction);
					}

				}
			}
			
			// * encode the size
			// bits += prefix(size);
			pBits += prefix(size); 
			
			// * Instead of the size
			bits += -log2(p(true, delimiter));
			delimiter.add(true);
		}
		
		Global.log().info("Symbol model entropy = "+nodes.entropy()+", delimiter model entropy = "+delimiter.entropy()+", directions model entropy = "+directions + " " +  directions.entropy()+". ");
		
		Global.log().info(pBits + " bits out of " + bits + " spent of encoding sizes (" + (pBits/bits)*100 + " percent). ");
		return bits;
	}
	
	private static <N> double p(N symbol, FrequencyModel<N> model)
	{
		double freq = model.frequency(symbol),
		       total = model.total(),
		       distinct = model.distinct();
			
		return (freq + 0.5) / (total + 0.5 * distinct);
	}

}
