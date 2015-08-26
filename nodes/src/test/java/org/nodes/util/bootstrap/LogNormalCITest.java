package org.nodes.util.bootstrap;

import static org.junit.Assert.*;
import static org.nodes.util.Series.series;
import static org.nodes.util.bootstrap.LogNormalCI.LOGE;
import static org.nodes.util.bootstrap.LogNormalCI.LN2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;
import org.nodes.util.Functions;
import org.nodes.util.Pair;
import org.nodes.util.Series;

public class LogNormalCITest
{

	@Test
	public void test()
	{
		int repeats = 100;
		double alpha = 0.05;

		double lnMean = 2000.0;
		double lnSD = 100.0;
		
		NormalDistribution normal = new NormalDistribution(lnMean, lnSD);
		
		double lnXMean = lnMean + 0.5 * lnSD * lnSD;
		
		double logSum = Double.NEGATIVE_INFINITY;
		
		
		for(int num : Arrays.asList(10, 100, 1000, 10000, 100000, 1000000))
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
