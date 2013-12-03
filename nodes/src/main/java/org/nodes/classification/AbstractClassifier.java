package org.nodes.classification;

import java.util.*;

public abstract class AbstractClassifier<P> implements Classifier<P> {

	protected int numClasses;
	protected int dimensionality;
	
	public AbstractClassifier(int dimensionality, int numClasses)
	{
		this.numClasses = numClasses;
		this.dimensionality = dimensionality;
	}
	
	public List<Integer> classify(List<P> points)
	{
		List<Integer> classes = new ArrayList<Integer>(points.size());
		for(P point : points)
			classes.add(classify(point));
		
		return classes;
	}
	
	public int classify(P point) 
	{
		List<Double> probs = probabilities(point);
		
		double maxProb = Double.NEGATIVE_INFINITY;
		int    max = 0;
		
		for(int i = 0; i < probs.size(); i++)
			if(probs.get(i) > maxProb)
			{
				max = i; 
				maxProb = probs.get(i);
			}
		
		if(maxProb <= 0.0)
			return -1;
		return max; 
	}
	
	public List<Double> probabilities(P point) 
	{
		
		List<Double> probs = new ArrayList<Double>(size());
		
		for(int i = 0; i < size(); i++)
			probs.add(0.0);
		
		probs.set((int)classify(point), 1.0);
		
		return probs;
	}	

	public int dimension()
	{
		return dimensionality;
	}
	
	public int size() 
	{
		return numClasses;
	}
}
