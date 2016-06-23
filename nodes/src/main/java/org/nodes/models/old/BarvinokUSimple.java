package org.nodes.models.old;

import static java.lang.Math.exp;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Node;
import org.nodes.UGraph;

/**
 * These class computes bounds for the number of directed graphs (self loops 
 * allowed) with given in and out sequences.
 *  
 * @author Peter
 *
 */
public class BarvinokUSimple extends AbstractBarvinok
{

	private List<Integer> sequence;
	
	public BarvinokUSimple(
			List<Integer> sequence, 
			int memory)
	{
		super(sequence, sequence, memory);
		
		this.sequence = sequence;
		
		search();
	}
	
	public BarvinokUSimple(
			UGraph<?> graph,
			int memory)
	{		
		super(seq(graph), seq(graph), memory);

		search();
	}
	
	private static List<Integer> seq(UGraph<?> graph)
	{
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		
		for(Node<?> node : graph.nodes())
			sequence.add(node.degree());

		return sequence;
	}

	@Override
	public double value(RealVector x)
	{
		double value = 0.0;
		double[] xa = ((ArrayRealVector)x).getDataRef();// nasty trick.
		
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; j++)
				value += Math.log1p( exp(xa[i] + xa[t(j)]));
		
		for(int i = 0; i < n; i++)
			value -= x.getEntry(s(i)) * sequence.get(i);
		
		for(int j= 0; j < n; j++)
			value -= x.getEntry(t(j)) * sequence.get(j);
		
		return value;
	}

	@Override
	public RealVector gradient(RealVector x)
	{
		RealVector gradient = new ArrayRealVector(2 * n); 

		for(int i : series(n))
		{
			double sum = 0.0;
			for(int j : series(i+1, n))
			{
				double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
				sum += part / (1 + part);
			}
			
			gradient.setEntry(s(i), sum - inSequence.get(i));
		}
		
		for(int j : series(n))
		{
			double sum = 0.0;
			for(int i : series(0, j))
			{
				double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
				sum += part / (1 + part);
			}
			
			gradient.setEntry(t(j), sum - outSequence.get(j) );
		}
		
		return gradient;
	}
}
