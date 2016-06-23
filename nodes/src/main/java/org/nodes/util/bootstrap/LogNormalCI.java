package org.nodes.util.bootstrap;

import static java.lang.Math.sqrt;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;

public class LogNormalCI
{
	public static final int DEFAULT_BS_SAMPLES = 10000;
	
	public static final double LN2 = Math.log(2.0);
	public static final double LOGE = Functions.log2(Math.E);
	
	private List<Double> logValues;
	private ArrayList<Double> ts;
	private double lnMean;
	private int lnVariance;
	private int n;
	private ArrayList<Double> lnValues;
	
	/**
	 * 
	 * @param logValues data in base 2 log.
	 */
	public LogNormalCI(List<Double> logValues, int bsSamples)
	{
		this.logValues = logValues;
				
		n = logValues.size();
		
		// * convert observations to ln
		lnValues = new ArrayList<Double>(n);
		for(int i : series(n))
			lnValues.add(logValues.get(i) * LN2);
		
		// * compute the mean of the log observations (in ln space, as it were)
		//   NOTE: This is different from lnMeanEstimate
		lnMean = 0.0;
		for(int i : series(n))
			lnMean += lnValues.get(i);
		lnMean /= (double) n;
		
		// * compute the variance of the log observations
		lnVariance = 0;
		for(int i : series(n))
		{
			double diff = lnValues.get(i) - lnMean;
			lnVariance += diff * diff;
		}
		lnVariance /= (double)(n - 1); 
		
		List<Double> ns = new ArrayList<Double>(bsSamples);
		List<Double> chis = new ArrayList<Double>(bsSamples);
		
		for(int i : series(bsSamples))
			ns.add(Global.random().nextGaussian());
		
		for(int i : series(bsSamples))
			chis.add(chiSquaredSample(n - 1));
		
		ts = new ArrayList<Double>(bsSamples);
		for(int i : series(bsSamples))
		{
			double x = chis.get(i)/(n-1);
			double num = ns.get(i) + sqrt(lnVariance) * 0.5 * sqrt(n) * (x - 1);
			double den = sqrt(x * (1.0 + lnVariance * 0.5 * x));
			ts.add(num / den);
		}
		
		Collections.sort(ts);
	} 

	public LogNormalCI(List<Double> logSamples)
	{
		this(logSamples, DEFAULT_BS_SAMPLES);
	}

	private double chiSquaredSample(int k)
	{
		double sumOfSquares = 0.0;
		for(int i : series(k))
		{
			double s = Global.random().nextGaussian();
			sumOfSquares += s * s;
		}
		
		return sumOfSquares;
	}
	
	public double lowerBound(double alpha)
	{
		int upperIndex = (int) Math.floor( (1.0 - alpha) * ts.size());
		double t1 = ts.get(upperIndex);

		return (lnMean + lnVariance * 0.5 - t1 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n)) * LOGE;
	}

	public double upperBound(double alpha)
	{
		int lowerIndex = (int) Math.floor( (alpha) * ts.size());
		double t0 = ts.get(lowerIndex);

		return (lnMean + lnVariance * 0.5 - t0 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n)) * LOGE;
	}
	
	public Pair<Double, Double> twoSided(double alpha)
	{
		return new Pair<Double, Double>(
			lowerBound(alpha*0.5),
			upperBound(alpha*0.5));
	}
	
	
	public double mlMean()
	{
		return mlMean(lnValues) * LOGE;
	}
	
	/**
	 * In base E. 
	 * 
	 * @param lnValues
	 * @return
	 */
	public static double mlMean(List<Double> lnValues)
	{
		double mlMean = 0.0;
		for(int i : series(lnValues.size()))
			mlMean += lnValues.get(i);
		mlMean /= (double) lnValues.size();
		
		// * compute the variance of the log observations
		double sumSq = 0;
		for(int i : series(lnValues.size()))
		{
			double diff = lnValues.get(i) - mlMean;
			sumSq += diff * diff;
		}
		double mlVariance = sumSq / (double) lnValues.size();
		
		double ml = mlMean + 0.5 * mlVariance;		
		
		return ml;
	}
}
