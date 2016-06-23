package org.nodes.util.bootstrap;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Pair;

/**
 * Constructs a percentile bootstrap confidence interval for the estimate of the 
 * mean 
 *  
 * TODO: Generalize the statistic to any function over the data. 
 *  
 * @author Peter
 *
 */
public class PercentileCI
{	
	protected List<Double> data;
	protected int bootstrapSamples;
	
	protected List<Double> bootstraps;
	
	protected double dataMean;
	
	public PercentileCI(List<Double> data, int bootstrapSamples)
	{
		this.data = data;
		this.bootstrapSamples = bootstrapSamples;
		
		dataMean = 0.0;
		for(double datum : data)
			dataMean += datum;
		dataMean /= data.size();
		
		bootstraps = new ArrayList<Double>(bootstrapSamples);
		
		List<Double> sample = new ArrayList<Double>(data.size());
		for(int b : series(bootstrapSamples))
		{
			sample.clear();
			for(int i : series(data.size()))
				sample.add(Functions.choose(data));
			
			double mean = 0.0;
			for(double val : sample)
				mean += val;
			mean /= sample.size();

			bootstraps.add(mean);
		}
		
		Collections.sort(bootstraps);
	}
	
	public double mean()
	{
		return dataMean;
	}
	
	/**
	 * Returns a value such that, by estimate, we have 1-alpha confidence that 
	 * the true value of the mean is above the value.  
	 * 
	 * @param alpha
	 * @return
	 */
	public double lowerBound(double alpha)
	{
		int iBelow = (int)(alpha * bootstraps.size());
		double rem = alpha * bootstraps.size() - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		// interpolated values
		return bootstraps.get(iBelow) * (1.0 - rem) + bootstraps.get(iBelow+1) * (rem);
	}
	
	/**
	 * Returns a value such that, by estimate, we have 1-alpha confidence that 
	 * the true value of the mean is below the value.  
	 * 
	 * @param alpha
	 * @return
	 */
	public double upperBound(double alpha)
	{
		int iBelow = (int)((1.0 - alpha) * bootstraps.size());
		double rem = ((1.0 - alpha) * bootstraps.size()) - iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		// interpolated values
		return bootstraps.get(iBelow) * (1.0 - rem) + bootstraps.get(iBelow+1) * (rem);
	}	
	
	/**
	 * Returns an upper and a lower bound such that, by estimate, we have 
	 * 1-alpha confidence that the true value is between the two bounds. 
	 * 
	 * @param alpha
	 * @return
	 */
	public Pair<Double, Double> twoSided(double alpha)
	{
		return new Pair<Double, Double>(
			lowerBound(alpha*0.5),
			upperBound(alpha*0.5));
	}
}
