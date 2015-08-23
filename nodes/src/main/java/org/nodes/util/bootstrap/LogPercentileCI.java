package org.nodes.util.bootstrap;

import static org.nodes.util.LogNum.fromDouble;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nodes.util.Functions;
import org.nodes.util.LogNum;
import org.nodes.util.Pair;

/**
 * Constructs a percentile bootstrap confidence interval for the estimate of the 
 * mean, with the data provided as binary logarithms of the true values.
 * 
 * The values returns are logarithms of the bounds of the interval on the true 
 * data.
 *  
 * TODO: Generalize the statistic to any function over the data. 
 *  
 * @author Peter
 *
 */
public class LogPercentileCI
{	
	public static final int BS_SAMPLES = 10000;
	
	protected List<LogNum> data;
	protected int bootstrapSamples;
	
	protected List<LogNum> bootstraps;
	
	protected LogNum dataMean;
	
	public LogPercentileCI(List<Double> data, int bootstrapSamples)
	{
		this.data = new ArrayList<LogNum>(data.size());
		for(double datum : data)
			this.data.add(new LogNum(datum, true, 2.0));
		
		this.bootstrapSamples = bootstrapSamples;
		
		dataMean = LogNum.mean(this.data);
		
		bootstraps = new ArrayList<LogNum>(bootstrapSamples);
		
		List<LogNum> sample = new ArrayList<LogNum>(data.size());
		for(int b : series(bootstrapSamples))
		{
			sample.clear();
			for(int i : series(this.data.size()))
				sample.add(Functions.choose(this.data));

			bootstraps.add(LogNum.mean(sample));
		}
		
		Collections.sort(bootstraps);
	}
	
	public double logMean()
	{
		return dataMean.logMag();
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
			return bootstraps.get(iBelow).logMag();
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
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
			return bootstraps.get(iBelow).logMag();
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
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
	
	public static LogNum interpolate(LogNum left, LogNum right, double leftWeight)
	{
		// interpolated values
		LogNum lw = left.times(fromDouble(leftWeight, 2.0));
		LogNum rw = right.times(fromDouble(1.0-leftWeight, 2.0));
		
		return lw.plus(rw);
	}
}
