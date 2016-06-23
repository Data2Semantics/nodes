package org.nodes.util.bootstrap;

import static java.lang.Math.log;
import static nl.peterbloem.kit.Functions.logSum;
import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;
import static org.nodes.util.bootstrap.LogNormalCI.LOGE;
import static org.nodes.util.bootstrap.LogNormalCI.LN2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

public class LogNormalCITest
{

	/**
	 * Tests the behavior of different estimators for the mean of a log-normal 
	 * distribution
	 */
	// @Test
	public void test()
	{
		int repeats = 100;
		double alpha = 0.05;

		double lnMean = 200.0;
		double lnSD = 10.0;
		
		NormalDistribution normal = new NormalDistribution(lnMean, lnSD);
		
		double lnXMean = lnMean + 0.5 * lnSD * lnSD;
		
		double logSum = Double.NEGATIVE_INFINITY;
		
		
		for(int num : Arrays.asList(10, 100, 1000, 10000, 100000, 1000000, 10000000))
		{
			double[] sample = normal.sample(num);
			
			// * Naive estimator
			double naive = Functions.logSum(Math.E, sample) - Math.log(sample.length);
			
			// * ML estimator
			double mlMean = 0.0;
			for(int i : series(sample.length))
				mlMean += sample[i];
			mlMean /= (double) sample.length;
			
			// * compute the variance of the log observations
			double sumSq = 0;
			for(int i : series(sample.length))
			{
				double diff =sample[i] - mlMean;
				sumSq += diff * diff;
			}
			double mlVariance = sumSq / (double)sample.length;
			
			double ml = mlMean + 0.5 * mlVariance;
			
			double ml2Mean = 0.0;
			for(int i : series(sample.length))
				ml2Mean += sample[i] * LOGE;
			ml2Mean /= (double) sample.length;
			
			// * compute the variance of the log observations
			double sum2Sq = 0;
			for(int i : series(sample.length))
			{
				double diff = sample[i] * LOGE - ml2Mean;
				sum2Sq += diff * diff;
			}
			double ml2Variance = sum2Sq / (double)sample.length;
			double ml2 = ml2Mean + 0.5 * ml2Variance * LN2;
			
			double fancy = mlMean + ((num - 1.0) * sumSq)/(2.0 * (num + 4) * (num - 1) + 3.0 * sumSq);
			
			System.out.println("sample size: " + num);
			System.out.println("	true mean " + lnXMean);
			System.out.println("	naive estimator " + naive);
			System.out.println("	ml estimator " + ml);
			System.out.println("	ml2 estimator " + ml2 * LN2);
			System.out.println("	fancy estimator " + fancy);
		}
		
		for(int mult : Arrays.asList(1, 10, 100, 1000, 10000))
		{
			double sum = Double.NEGATIVE_INFINITY;
			double n = 0;
		
			for(int i : series(mult))
			{
				int num = 1000000;
				double[] sample = normal.sample(num);
	
				sum = logSum(Math.E, sum, logSum(Math.E, sample));
				n += num;	
			}
			
			System.out.println(mult + ":" + (sum - log(n)));
		}
				
		System.out.println("1000 unbiased estimates: ");
		for(int i : series(1000000))
		{
			int num = 1000000;
			double[] sample = normal.sample(num);

			// * Naive estimator
			double naive = logSum(Math.E, sample) - log(sample.length);
			double diff = 250 - naive;
			if(diff < 0.0)
				System.out.println("\n!!!! " + diff);
			
			Functions.dot(i, 1000000);
		}
		
//		double gold = lnXMean * LOGE; 
//		
//		for(int n : Series.series(10, 10, 150))
//		{
//			int hits = 0;
//		
//			for(int i : Series.series(repeats))
//			{
//				double[] sample = normal.sample(n);
//				List<Double> log2Sample = new ArrayList<Double>(sample.length);
//				
//				for(double lnSample : sample)
//					log2Sample.add(lnSample * LOGE);
//				
//				LogNormalCI ci = new LogNormalCI(log2Sample, 10000);
//				
//				Pair<Double, Double> interval = ci.twoSided(alpha);
//				
//				if(gold > interval.first() && gold < interval.second())
//					hits ++;
//			}	
//			
//			System.out.println("coverage at n="+n+": "+ (hits/(double)repeats));
//		}
	}
}
