package org.nodes.util;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nodes.UGraph;
import org.nodes.models.USequenceEstimator;
import org.nodes.models.USequenceEstimator.CIMethod;
import org.nodes.models.USequenceEstimator.CIType;
import org.nodes.random.RandomGraphs;
import org.nodes.util.bootstrap.PercentileCI;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

import org.nodes.util.bootstrap.BCaCI;
import org.nodes.util.bootstrap.LogBCaCI;
import org.nodes.util.bootstrap.LogPercentileCI;

public class BootstrapCITest
{

	@Test
	public void test()
	{
		int tries = 100;
		int n = 500;
		
		double var = 1.5;
		double mean = 0.2;

		double aMean = Math.exp(mean + 0.5 * var);
		
		int hits = 0;
		for(int tr : series(tries))
		{
			List<Double> data = new ArrayList<Double>(n);
			for(int i : Series.series(n))
			{
				double x = Math.exp(Global.random().nextGaussian() * Math.sqrt(var) + mean);
				data.add(x);
			}
			
			PercentileCI ci = new PercentileCI(data, 2000);
			
			Pair<Double, Double> pair = ci.twoSided(0.05);
			double lower = pair.first();
			double upper = pair.second();
			
			if(lower < aMean && aMean < upper)
				hits++;
		}
		
		double coverage = (hits/ (double)tries);
		System.out.println("coverage: " + coverage);
		
		assertEquals(0.95, coverage, 0.1);	
	}

	
	@Test
	public void testBCa()
	{
		int tries = 100;
		int n = 500;
		
		double var = 1.5;
		double mean = 0.1;

		double aMean = Math.exp(mean + 0.5 * var);
		
		int hits = 0;
		for(int tr : series(tries))
		{
			List<Double> data = new ArrayList<Double>(n);
			for(int i : Series.series(n))
			{
				double x = Math.exp(Global.random().nextGaussian() * Math.sqrt(var) + mean);
				data.add(x);
			}
			
			BCaCI ci = new BCaCI(data, 2000);
			
			Pair<Double, Double> pair = ci.twoSided(0.05);
			double lower = pair.first();
			double upper = pair.second();
					
			if(lower < aMean && aMean < upper)
				hits++;
		}
		
		double coverage = (hits/ (double)tries);
		System.out.println("coverage: " + coverage);
		
		assertEquals(0.95, coverage, 0.1);	
	}
	
	@Test
	public void testTransform()
	{
		UGraph<String> graph = RandomGraphs.random(10, 25);
		
		USequenceEstimator<String> model = new USequenceEstimator<String>(graph, 10);
		
		System.out.println("perc: " + model.confidence(0.05, CIMethod.PERCENTILE, CIType.TWO_SIDED));
		System.out.println("bca:  " + model.confidence(0.05, CIMethod.BCA, CIType.TWO_SIDED));
		
		List<Double> logSamples = model.logSamples();
		
		PercentileCI perc = new PercentileCI(logSamples, model.BOOTSTRAP_SAMPLES);
		System.out.println("perc (normal): " + perc.twoSided(0.05));
		
		LogPercentileCI percLog = new LogPercentileCI(logSamples, model.BOOTSTRAP_SAMPLES);
		System.out.println("perc (log): " + percLog.twoSided(0.05));

		
		BCaCI bca = new BCaCI(logSamples, model.BOOTSTRAP_SAMPLES);
		System.out.println("bca (normal):  " + bca.twoSided(0.05));
		
		LogBCaCI bcaLog = new LogBCaCI(logSamples, model.BOOTSTRAP_SAMPLES);
		System.out.println("bca (log):  " + bcaLog.twoSided(0.05));


		System.out.println(model.logNumGraphsNaive());
	}
}
