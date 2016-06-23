package org.nodes.models.old;

import static java.lang.Math.exp;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.nodes.DGraph;
import org.nodes.DNode;

/**
 * This class computes bounds for the number of directed graphs (self loops 
 * allowed) with given in and out sequences.
 *  
 * @author Peter
 *
 */
public class BarvinokDSimple extends AbstractBarvinok
{

	public BarvinokDSimple(
			List<Integer> inSequence, 
			List<Integer> outSequence,
			int memory)
	{
		super(inSequence, outSequence, memory);
		search();
	}
	
	public BarvinokDSimple(
			DGraph<?> graph,
			int memory)
	{		
		super(seq(graph, true), seq(graph, false), memory);

		search();
	}
	
	private static List<Integer> seq(DGraph<?> graph, boolean in)
	{
		List<Integer> sequence = new ArrayList<Integer>(graph.size());
		
		for(DNode<?> node : graph.nodes())
			sequence.add(in ? node.inDegree() : node.outDegree());

		return sequence;
	}

	@Override
	public double value(RealVector x)
	{
		double value = 0.0;
		double[] xa = ((ArrayRealVector)x).getDataRef();// nasty trick.
		int nn = 2 * n;
		for(int i = 0; i < n; i++)
			for(int j = n; j < nn; j++)
				if(i != j)
					value += Math.log1p( exp(xa[i] + xa[j]));
		
		for(int i = 0; i < n; i++)
			value -= x.getEntry(s(i)) * inSequence.get(i);
		
		for(int j= 0; j < n; j++)
			value -= x.getEntry(t(j)) * outSequence.get(j);
		
		return value;
	}

	@Override
	public RealVector gradient(RealVector x)
	{
		RealVector gradient = new ArrayRealVector(2 * n); 

		for(int i : series(n))
		{
			double sum = 0.0;
			for(int j : series(n))
				if(i != j) {
					double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
					sum += part / (1 + part);
				}
			
			gradient.setEntry(s(i), sum - inSequence.get(i));
		}
		
		for(int j : series(n))
		{
			double sum = 0.0;
			for(int i : series(n))
				if(i != j) {
					double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
					sum += part / (1 + part);
				}
			
			gradient.setEntry(t(j), sum - outSequence.get(j) );
		}
		
		return gradient;
	}
}
