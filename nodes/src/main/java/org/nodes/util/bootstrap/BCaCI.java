package org.nodes.util.bootstrap;

import static nl.peterbloem.kit.Functions.log2;
import static nl.peterbloem.kit.Functions.log2Sum;
import static nl.peterbloem.kit.Functions.minList;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Constructs a bootstrap CI with the BCa corrections
 * @author Peter
 *
 */
public class BCaCI extends PercentileCI
{

	private double a;
	private double b;
	
	public BCaCI(List<Double> data, int bootstrapSamples)
	{
		super(data, bootstrapSamples);

		// * estimate a: jackknife method
		// Mean estimates for each jackknife fold
		List<Double> means = new ArrayList<Double>(data.size());
		for(int i : series(data.size()))
		{
			double foldMean = 0.0;
			for(double datum : minList(data, i))
				foldMean += datum;
			foldMean /= data.size() - 1;
					
			means.add(foldMean);
		}
		
		double foldsMean = 0.0;
		for(double foldMean : means)
			foldsMean += foldMean;
		foldsMean /= means.size();
		
		double sumDiffsTo3 = 0.0, sumDiffsTo2 = 0.0;
		for(double foldMean : means)
		{
			double diff = foldsMean - foldMean; 
			sumDiffsTo3 += diff * diff * diff;
			sumDiffsTo2 += diff * diff;
		}
		
		double den = Math.sqrt(sumDiffsTo2);
		den = den * den * den;
		
		a = (1.0/6.0) * (sumDiffsTo3/den);
		
		// * estimate b from the number of bootstrap means below the sample mean
		int m = 0;
		while(bootstraps.get(m++) < dataMean);
		
		b = N.inverseCumulativeProbability(m/(double)bootstraps.size());
	}
	
	private static final NormalDistribution N = new NormalDistribution();
	
	public static double beta(double a, double b, double alpha)
	{
		double z = N.inverseCumulativeProbability(alpha);
		return N.cumulativeProbability(b + (b + z)/(1.0 - a * (b+z)));	
	}
	
	@Override
	public double lowerBound(double alpha)
	{ 
		int iBelow = (int) (beta(a, b, alpha) * bootstraps.size());  
		double rem = ((int) beta(a, b, alpha) * bootstraps.size()) - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		// interpolated values
		return bootstraps.get(iBelow) * (1.0 - rem) + bootstraps.get(iBelow+1) * (rem);
	}
	
	@Override
	public double upperBound(double alpha)
	{
		int iBelow = (int) (beta(a, b, 1.0 - alpha) * bootstraps.size());  
		double rem = ((int) (beta(a, b, 1.0 - alpha) * bootstraps.size())) - (double) iBelow;
		
		if(iBelow == bootstraps.size() -1)
			return bootstraps.get(iBelow);
		
		// interpolated values
		return bootstraps.get(iBelow) * (1.0 - rem) + bootstraps.get(iBelow+1) * (rem);
	}	
	

}
