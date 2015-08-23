package org.nodes.util.bootstrap;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.LogNum.fromDouble;
import static org.nodes.util.Series.series;
import static org.nodes.util.bootstrap.BCaCI.beta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.nodes.util.Functions;
import org.nodes.util.LogNum;

public class LogBCaCI extends LogPercentileCI
{
	private static final NormalDistribution N = new NormalDistribution();
	
	protected double a, b;

	public LogBCaCI(List<Double> data)
	{
		this(data, BS_SAMPLES);
	}
	
	public LogBCaCI(List<Double> data, int bootstrapSamples)
	{
		super(data, bootstrapSamples);
		
		// * estimate a by the jackknife method
		List<LogNum> means = new ArrayList<LogNum>(this.data.size());
		for(int i : series(this.data.size()))
		{
			List<LogNum> fold = Functions.minList(this.data, i);
			means.add(LogNum.mean(fold));
		}
		
		LogNum a = computeA(means);
		
		// * estimate b
		// * estimate b from the number of bootstrap means below the sample mean
		int m = 0;
		while(bootstraps.get(m).logMag() < dataMean.logMag())
			m++;
		
		b = N.inverseCumulativeProbability(m/(double)bootstraps.size());
	}
	
	protected static LogNum computeA(List<LogNum> jkEstimates)
	{
		LogNum mean = LogNum.mean(jkEstimates);
		
		List<LogNum> diffs = new ArrayList<LogNum>(jkEstimates.size());
		for(LogNum datum : jkEstimates)
			diffs.add(mean.minus(datum));
		
		List<LogNum> diffsTo2 = new ArrayList<LogNum>(jkEstimates.size());
		for(LogNum diff : diffs)
			diffsTo2.add(diff.pow(2));
				
		List<LogNum> diffsTo3 = new ArrayList<LogNum>(jkEstimates.size());
		for(LogNum diff : diffs)
			diffsTo3.add(diff.pow(3));
		
		LogNum num = LogNum.sum(diffsTo3);
		
		LogNum den = LogNum.sum(diffsTo2);
		den = den.root(2).pow(3).times(fromDouble(6.0, 2.0));
		
		if(den.logMag() == Double.NEGATIVE_INFINITY) // prevent div by zero
			return LogNum.fromDouble(0.0, den.base());
		
		return num.divide(den);
	}
	
	@Override
	public double lowerBound(double alpha)
	{ 
		int iBelow = (int) (beta(a, b, alpha) * bootstraps.size());  
		double rem = ((int) beta(a, b, alpha) * bootstraps.size()) - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow).logMag();
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
	}
	
	@Override
	public double upperBound(double alpha)
	{
		int iBelow = (int) (beta(a, b, 1.0 - alpha) * bootstraps.size());  
		double rem = ((int) (beta(a, b, 1.0 - alpha) * bootstraps.size())) - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow).logMag();
		
		return interpolate(bootstraps.get(iBelow), bootstraps.get(iBelow+1), (1.0 - rem)).logMag();
	}
	

}
