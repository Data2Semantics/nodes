package org.nodes.models;

import static java.lang.Math.E;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.nodes.models.USequenceModel.CIMethod.STANDARD;
import static org.nodes.util.Functions.choose;
import static org.nodes.util.Functions.exp2;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Min;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.LogNum.fromDouble;
import static org.nodes.util.Pair.first;
import static org.nodes.util.Series.series;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.Functions;
import org.nodes.util.Generator;
import org.nodes.util.LogNum;
import org.nodes.util.MaxObserver;
import org.nodes.util.Pair;
import org.nodes.util.Series;

/**
 * Implementation of the Diaconis/Blitzstein sequential importance sampling 
 * algorithm.
 * 
 *  
 * @author Peter
 *
 * @param <L>
 */
public class USequenceModel<L> implements Model<L, UGraph<L>>
{	
	public static final boolean PICK_CANDIDATE_BY_DEGREE = true;
	private L label = null;
	private List<Integer> sequence;
	
	private List<Double> logSamples;

	public USequenceModel(Graph<?> data, int samples)
	{
		this(data);
		
		for(int i : series(samples))
			nonuniform();
	}
	
	public USequenceModel(Graph<?> data)
	{

		sequence = new ArrayList<Integer>(data.size());
		
		for(Node<?> node : data.nodes())
			sequence.add(node.degree());
							
		this.logSamples = new ArrayList<Double>();
	}
	
	public USequenceModel(List<Integer> sequence, int samples)
	{
		this(sequence);

		Functions.tic();
		for(int i : series(samples))
		{
			nonuniform();
			if(Functions.toc() > 10)
				System.out.println("\r " + logSamples.size() + "samples completed");
		}
	}
	
	public USequenceModel(List<Integer> sequence)
	{
		this.sequence = new ArrayList<Integer>(sequence);
					
		this.logSamples = new ArrayList<Double>();
	}
	
	public List<Double> logSamples()
	{
		return Collections.unmodifiableList(logSamples);
	}
	
	public static final int BOOTSTRAP_SAMPLES = 10000;
		
	public double logNormalMean()
	{
		double ln2 = Math.log(2.0);
		int n = logSamples.size();
		
		// * convert observations to ln
		List<Double> lnObservations = new ArrayList<Double>(n);
		
		for(int i : series(n))
			lnObservations.add(logSamples.get(i) * ln2);
		
		// * compute the mean of the log observations
		//   NOTE: This is different from lnMeanEstimate
		//         They'll coincide if the source is properly logNormal, but 
		//         that's usually not quite the case
		double lnMean = 0.0;
		for(int i : series(n))
			lnMean += lnObservations.get(i);
		lnMean /= (double) n;
		
		// * compute the variance of the log observations
		double lnVariance = 0;
		for(int i : series(n))
		{
			double diff = lnObservations.get(i) - lnMean;
			lnVariance += diff * diff;
		}
		lnVariance /= (double)(n - 1); 
		
		
		return (lnMean + 0.5 * lnVariance) * Functions.log2(Math.E);
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
	
	public double effectiveSampleSize()
	{
		List<Double> squared = new ArrayList<Double>(logSamples.size());
		for(int i : series(logSamples.size()))
			squared.add(2.0 * logSamples.get(i)); 
		
		double logEffSampleSize = 2.0 * log2Sum(logSamples) - log2Sum(squared);
		
		return pow(2.0, logEffSampleSize);
	}
	
	public double logStdError()
	{
		return logStdDev() - 0.5 * log2(logSamples.size());
	}

	public double logStdDev()
	{
		double logMean = logNumGraphs();
		
		List<Double> logDiffs = new ArrayList<Double>(logSamples.size());
		for(int i : series(logSamples.size()))
		{
			double max = Math.max(logSamples.get(i), logMean);
			double min = Math.min(logSamples.get(i), logMean);
			logDiffs.add(2.0 * log2Min(max, min));
		}
	
		return - 0.5 * log2(logSamples.size() - 1.0) + 0.5 * log2Sum(logDiffs);
	}
	
	public double logProb()
	{
		return - logNumGraphs();
	}
	
	public double logNumGraphs()
	{
		return log2Sum(logSamples) - log2(logSamples.size());
	}

	public double numGraphs()
	{
		return pow(2.0, logNumGraphs());
	}
	
	@Override
	public double logProb(UGraph<L> graph)
	{
		// TODO: check if graph matches sequence
		
		return - logNumGraphs();
	}
	
	public static enum CIMethod {
		/**
		 * The CI method for the mean estimator, based on the T statistic. Note 
		 * that this method is _highly_ unreliable in this setting. 
		 */
		STANDARD,
		/**
		 * A parametric bootstrap method, based on the assumption that the 
		 * samples are log-normally distributed.   
		 */
		LOG_NORMAL,
		/**
		 * Standard non-parametric bootstrap method. Unreliable, since our source 
		 * distribution is highly skewed. 
		 */
		PERCENTILE,
		/**
		 * Bias-corrected version of the percentile method.
		 */
		BCA
	}
	
	public static enum CIType
	{
		/**
		 * Leaves exactly alpha/2 of probability mass
		 * on either side of the confidence interval
		 */
		TWO_SIDED,
		/**
		 * Leaves alpha of probability mass below the confidence interval and 
		 * none above
		 */
		LOWER_BOUND,
		/**
		 * Leaves alpha of probability mass above the confidence interval and 
		 * none below
		 */
		UPPER_BOUND
		
	}
	
	public Pair<Double, Double> confidence(double alpha, CIType type)
	{
		return confidence(alpha, CIMethod.STANDARD, type);
	}
	
	/**
	 * Returns a confidence interval, in log space, for the estimate of the 
	 * logarithm of the number of graphs. 
	 *  
	 * @param alpha
	 * @param method 
	 * @param type
	 * @return
	 */
	public Pair<Double, Double> confidence(double alpha, CIMethod method, CIType type)
	{
		if(logSamples.isEmpty())
			throw new IllegalStateException("logSamples is empty. Cannot compute CI before samples have been created");

		if(method == STANDARD)
			return confidenceStandard(alpha, type);
		
		if(method == CIMethod.LOG_NORMAL)
			return confidenceLogNormal(alpha, type);
		
		if(method == CIMethod.PERCENTILE || method == CIMethod.BCA)
			return confidenceBootstrap(alpha, method, type);
		
		return null;
		
	}
	
	protected Pair<Double, Double> confidenceStandard(double alpha, CIType type)
	{
		// * Compute the effective standard error
		double logNumGraphs = logNumGraphs(), logStdError = logStdError();
		
		TDistribution t = new TDistribution(logSamples.size() - 1);
		
		if(type == CIType.TWO_SIDED)
		{
			double crit = t.inverseCumulativeProbability(1.0 - (alpha/2.0));
			
			System.out.println(crit + " " + logStdError);
			
			double confLower = logNumGraphs > log2(crit) + logStdError ? 
					log2Min(logNumGraphs, log2(crit) + logStdError) :
					Double.NEGATIVE_INFINITY;
			double confUpper = log2Sum(logNumGraphs, log2(crit) + logStdError);
			
			return new Pair<Double, Double>(confLower, confUpper);
		} else if(type == CIType.LOWER_BOUND)
		{
			double crit = t.inverseCumulativeProbability(1.0 - alpha);
			
			double confLower = logNumGraphs > log2(crit) + logStdError ? 
					log2Min(logNumGraphs, log2(crit) + logStdError) :
					Double.NEGATIVE_INFINITY;			
			return new Pair<Double, Double>(confLower, Double.POSITIVE_INFINITY);
		} else // type == CIType.UPPER_BOUND
		{
			double crit = t.inverseCumulativeProbability(1.0 - alpha);

			double confUpper = log2Sum(logNumGraphs, log2(crit) + logStdError);
			
			return new Pair<Double, Double>(Double.NEGATIVE_INFINITY, confUpper);
		}
	}
	
	protected Pair<Double, Double> confidenceLogNormal(double alpha, CIType type)
	{
		double ln2 = Math.log(2.0);
		double loge = Functions.log2(Math.E);
		int n = logSamples.size();
		
		// * convert observations to ln
		List<Double> lnObservations = new ArrayList<Double>(n);
		
		for(int i : series(n))
			lnObservations.add(logSamples.get(i) * ln2);
		
		// * compute the mean of the log observations
		//   NOTE: This is different from lnMeanEstimate
		//         They'll coincide if the source is properly logNormal, but 
		//         that's usually not quite the case
		double lnMean = 0.0;
		for(int i : series(n))
			lnMean += lnObservations.get(i);
		lnMean /= (double) n;
		
		// * compute the variance of the log observations
		double lnVariance = 0;
		for(int i : series(n))
		{
			double diff = lnObservations.get(i) - lnMean;
			lnVariance += diff * diff;
		}
		lnVariance /= (double)(n - 1); 
		
		// * This is our estimate of the mean in log_2, based on the 
		//   assumption that we have a log-normal distribution
		//   This is usually different from the real mean, because the distribution
		//   isn't truly logNormal		
		//   logNormalMean = (lnMean + 0.5 * lnVariance) * log2(E);
		
		List<Double> ns = new ArrayList<Double>(BOOTSTRAP_SAMPLES);
		List<Double> chis = new ArrayList<Double>(BOOTSTRAP_SAMPLES);
		
		for(int i : series(BOOTSTRAP_SAMPLES))
			ns.add(Global.random().nextGaussian());
		
		for(int i : series(BOOTSTRAP_SAMPLES))
			chis.add(chiSquaredSample(n - 1));
		
		List<Double> ts = new ArrayList<Double>();
		for(int i : series(BOOTSTRAP_SAMPLES))
		{
			double x = chis.get(i)/(n-1);
			double num = ns.get(i) + sqrt(lnVariance) * 0.5 * sqrt(n) * (x - 1);
			double den = sqrt(x * (1.0 + lnVariance * 0.5 * x));
			ts.add(num/den);
		}
		
		Collections.sort(ts);
		
		double lowerBound, upperBound;
		
		if(type == CIType.TWO_SIDED)
		{
			int lowerIndex = (int) Math.floor( (alpha*0.5) * BOOTSTRAP_SAMPLES );
			double t0 = ts.get(lowerIndex);

			int upperIndex = (int) Math.floor( (1.0 - alpha*0.5) * BOOTSTRAP_SAMPLES );
			double t1 = ts.get(upperIndex);

			lowerBound = lnMean + lnVariance * 0.5 - t1 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
			upperBound = lnMean + lnVariance * 0.5 - t0 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
		} else if(type == CIType.LOWER_BOUND)
		{
			int upperIndex = (int) Math.floor( (1.0 - alpha) * BOOTSTRAP_SAMPLES );
			double t1 = ts.get(upperIndex);

			lowerBound = lnMean + lnVariance * 0.5 - t1 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
			upperBound = Double.POSITIVE_INFINITY;			
		} else // type == CIType.UPPER_BOUND
		{
			int lowerIndex = (int) Math.floor( (alpha) * BOOTSTRAP_SAMPLES );
			double t0 = ts.get(lowerIndex);

			lowerBound = Double.NEGATIVE_INFINITY;
			upperBound = lnMean + lnVariance * 0.5 - t0 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
		}
		
		/**
		 * Convert to base 2 and return
		 */
		return new Pair<Double, Double>(lowerBound * loge, upperBound * loge);
	}
	
	protected Pair<Double, Double> confidenceBootstrap(double alpha, CIMethod method, CIType type)
	{
		double logMean = logNumGraphs(); 
		
		List<Double> bsMeans = new ArrayList<Double>(BOOTSTRAP_SAMPLES);

		List<Double> bsData = new ArrayList<Double>(logSamples.size());
		for(int i : series(logSamples.size()))
				bsData.add(null);
		
		for(int i : series(BOOTSTRAP_SAMPLES))
		{
			// * sample a bootstrap dataset
			for(int j : series(bsData.size()))
				bsData.set(j, choose(logSamples)); 
			
			// * compute the mean on the bootstrap dataset
			bsMeans.add(log2Sum(bsData) - log2(bsData.size()));
		}
		
		Collections.sort(bsMeans);
		
		Pair<Double, Double> result = null;
		if(method == CIMethod.PERCENTILE)
		{
			if(type == CIType.TWO_SIDED)
			{
				int lowerIndex = (int) Math.floor( (alpha*0.5) * BOOTSTRAP_SAMPLES );
				int upperIndex = (int) Math.floor( (1.0 - alpha*0.5) * BOOTSTRAP_SAMPLES );
			
				result = new Pair<Double, Double>(bsMeans.get(lowerIndex), bsMeans.get(upperIndex));
			} else if(type == CIType.LOWER_BOUND)
			{
				int lowerIndex = (int) Math.floor( alpha * BOOTSTRAP_SAMPLES );
			
				result = new Pair<Double, Double>(bsMeans.get(lowerIndex), Double.POSITIVE_INFINITY);
			
			} else // type == CIType.UPPER_BOUND
			{				
				int upperIndex = (int) Math.floor( (1.0 - alpha) * BOOTSTRAP_SAMPLES );

				result = new Pair<Double, Double>(Double.NEGATIVE_INFINITY, bsMeans.get(upperIndex));
			}
		} else if (method == CIMethod.BCA)
		{
			// * estimate b from the number of bootstrap means below the sample mean
			int m = 0;
			while(bsMeans.get(m) < logMean)
				m++;
			
			double b = (new NormalDistribution()).inverseCumulativeProbability(m/(double)bsMeans.size());
			
			// * estimate a: jackknife method
			// Mean estimates for each jackknife fold
			List<LogNum> means = new ArrayList<LogNum>(logSamples.size());
			for(int i : series(logSamples.size()))
			{
				List<Double> fold = Functions.minList(logSamples, i);
				double foldLogMean = log2Sum(fold) - log2(logSamples.size() - 1);
				means.add(LogNum.fromDouble(foldLogMean, 2.0));
			}
			
			LogNum a = computeA(means);
			
			// * Compute corrected indices
			
			if(type == CIType.TWO_SIDED)
			{

				double betaLower = beta(a.doubleValue(), b, alpha/2.0);
				double betaUpper = beta(a.doubleValue(), b, 1.0 - alpha/2.0);
				
				int lowerIndex = (int) floor( betaLower * BOOTSTRAP_SAMPLES );
				int upperIndex = (int) floor( betaUpper * BOOTSTRAP_SAMPLES );
			
				result = new Pair<Double, Double>(bsMeans.get(lowerIndex), bsMeans.get(upperIndex));
			} else if(type == CIType.LOWER_BOUND)
			{
				double betaLower = beta(a.doubleValue(), b, alpha);
				
				int lowerIndex = (int) floor( betaLower * BOOTSTRAP_SAMPLES );
			
				result = new Pair<Double, Double>(bsMeans.get(lowerIndex), Double.POSITIVE_INFINITY);
			
			} else // type == CIType.UPPER_BOUND
			{				
				double betaUpper = beta(a.doubleValue(), b, 1.0 - alpha);
				
				int upperIndex = (int) floor( betaUpper * BOOTSTRAP_SAMPLES );
				
				result = new Pair<Double, Double>(Double.NEGATIVE_INFINITY, bsMeans.get(upperIndex));
			}
		}
		
		return result;
	}
	
	
	private static final NormalDistribution N = new NormalDistribution(); 
	protected static double beta(double a, double b, double alpha)
	{
		double z = N.inverseCumulativeProbability(alpha);
		
		return N.cumulativeProbability(b + (b + z)/(1.0 - a * (b+z))); 
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


	
	/**
	 * Generates a random graph with the given degree  
	 * 
	 * @return
	 */
	public Result nonuniform()
	{
		// * Create an empty graph
		UGraph<L> graph = new MapUTGraph<L, String>();
		
		int n = sequence.size();
		
		for(int i : Series.series(n))
			graph.add(label);
		
		List<Integer> sequence = new ArrayList<Integer>(this.sequence);
		
		// * The log number of edge sequences producing the graph that will be
		//   sampled
		double logCY = 0; 
		// * The log probability of the edge sequence that we've sampled 
		double logSigY = 0;
		
		while(sum(sequence) > 0) // TODO: keep running value of sum
		{			
			// * Find the smallest i whose index is nonzero and minimal
			int fromIndex = -1, fromVal = Integer.MAX_VALUE;
			for(int index : Series.series(n))
			{
				int val = sequence.get(index);

				if(val != 0 && val < fromVal)
				{
					fromIndex = index;
					fromVal = val;
				}
			}
			
			logCY += Functions.logFactorial(fromVal, 2.0);
			
			while(sequence.get(fromIndex) > 0)
			{
				Node<L> from = graph.get(fromIndex);
				
				List<Integer> toCandidates = new ArrayList<Integer>(n);
				
				toCandidates = findAcceptableSet(sequence, graph, from);
				int i;
				
				if(PICK_CANDIDATE_BY_DEGREE) // choose the candidate by weighted degree
				{
					List<Double> candidateWeights = new ArrayList<Double>(n);
					int sum = 0;
					
					for(int toIndex : toCandidates)
					{
						candidateWeights.add((double)sequence.get(toIndex));
						sum += sequence.get(toIndex);
					}
					
					i = choose(candidateWeights, sum);
					logSigY += log2(candidateWeights.get(i)/sum);

				} else // uniform choice
				{
					i = Global.random().nextInt(toCandidates.size());
					logSigY += - log2(toCandidates.size());
				}
				
				int toIndex = toCandidates.get(i);
				Node<L> to = graph.get(toIndex);
				
				to.connect(from);
				
				sub(sequence, fromIndex, toIndex);
			}
		}
		
		logSamples.add(- logCY - logSigY);
		
		return new Result(graph, logCY, logSigY);
	}
	
	public static boolean isGraphicalOld(List<Integer> sequence)
	{
		if(sum(sequence) % 2 != 0)
			return false;

		List<Integer> seq = new ArrayList<Integer>(sequence);
		Collections.sort(seq, Collections.reverseOrder());
		sequence = null; // stop myself from accidentally using the unsorted one
		
		int sum = 0;
		for(int k : series(1, durfee(seq) + 1))
		{
			int dk = seq.get(k - 1);
			sum += dk;
			int inSum = 0;
			for(int i : series(k+1, seq.size() + 1))
				inSum += min(k, seq.get(i - 1));
						
			if(sum > k * (k - 1) + inSum)
				return false;
		}
		
		return true;
	}
	
	public static boolean isGraphical(List<Integer> sequence)
	{
		if(sum(sequence) % 2 != 0)
			return false;
		
		List<Integer> seq = new ArrayList<Integer>(sequence);
		Collections.sort(seq, Collections.reverseOrder());
		sequence = null; // stop myself from accidentally using the unsorted one
		
		seq = seq.subList(0, seq.size() - numZeroes(seq));
		
		if(seq.size() == 0)
			return true;
		
		List<Integer> xks = findxks(seq);
		// System.out.println(xks);
		
		int lk = seq.get(0);
		int rk = seq.size() - 1;
		
		if(lk > rk)
			return false;
						
		for(int k : series(1, seq.size()))
		{
			// 	System.out.println("l"+(k-1) + "=" + lk + ", r"+(k-1)+"="+rk);
			int dk = seq.get(k);
			
			lk = lk + dk;
			
			if(k < xks.size())
				rk = rk + xks.get(k) -1;
			else
				rk = rk + 2*k - dk;
			
			if(lk > rk)
				return false;
		}
		
		return true;
	}
	
	protected static int numZeroes(List<Integer> seq)
	{
		int numZeroes = 0;
		int i = seq.size();
		
		while(i > 0 && seq.get(--i) <= 0)
			numZeroes ++;
			
		return numZeroes;
	}
	
	static List<Integer> findxks(List<Integer> seq)
	{
		// xks[k] contains the first index xk in seq such that 
		// --   seq[xk] < k+1
		List<Integer> xks = new ArrayList<Integer>(seq.size());
		
		int xk = seq.size() - 1;
		while(xk >= 0)
		{
			// while seq[xk] > k
			while(seq.get(xk) > xks.size())
			{
				if(xk + 1 < xks.size() + 1) // we've reached k-star
					return xks;
				
				xks.add(xk + 1);
			}
			
			xk --;
		}
		
		return xks;
	}
	
	/**
	 * Find the maximum fail degree (algorithm explained on page 4 of the paper 
	 * by Del Genio et al)
	 * 
	 * @param seq The residual degree sequence (sorted in descending order). 
	 *   This sequence must be graphical
	 * @param allowed A set of nodes that we are not allowed to connect the 
	 *   hub to (should include the hub itself).
	 * @return The maximum fail degree for this sequence. 0 if no fail degrees
	 *   exist. The logic is that connecting to a node with residual degree 0 is
	 * 	 always illegal.
	 */
	protected static int findMaxFailDegree(List<Index> seq)
	{
		seq = seq.subList(0, seq.size() - numZeroes(degrees(seq)));
		
		if (seq.size() == 0)
			return -1;
		
		List<Integer> xks = findxks(degrees(seq));
		
		int lk = seq.get(0).degree;
		int rk = seq.size() - 1;
		int k = 0;
		
		// find the max k
		int maxK = 0;
		while(maxK < seq.size() && seq.get(maxK).degree >= maxK)
			maxK ++;

		int failDegree = 0;
		boolean finished = false;
		while (k < maxK && ! finished)
		{
			if(lk > rk)
				throw new IllegalStateException("Argument seq was not a graphical sequence: " + seq);

			// check whether there are fail degrees at this k
			if(lk == rk)
			{
				// find the first non-forbidden i with i > k
				// c is the largest fail degree and we won't find a larger one.
				int c = k + 1;
				while(c < seq.size() && ! seq.get(c).allowed)
					c++;
				
				if(c < seq.size())
					failDegree = seq.get(c).degree;
				
				finished = true;
			} else if(lk == rk - 1) 
			{
				// find the first non-forbidden i with i > k and d_i < k + 2
				// i is the largest fail-degree for this k, but there 
				// may be larger for other k.
				
				int c = k + 1;
				while(c < seq.size() && ((! seq.get(c).allowed) || seq.get(c).degree >= k + 2))
					c++;
				
				if(c < seq.size())
					failDegree = seq.get(c).degree;
			}
			
			// increment k and update lk and rk
			k++;
			int dk = seq.get(k).degree;
			
			lk = lk + dk;
			
			if(k < xks.size())
				rk = rk + xks.get(k) -1;
			else
				rk = rk + 2*k - dk;
		}
		
		return failDegree;
	} 
	
	/**
	 * Returns the set of nodes we can connect to the node hub without creating 
	 * an ungraphical degree sequence.
	 *
	 * @param residualDegrees
	 * @param graphSoFar
	 * @param hub
	 * @return
	 */
	public static <L> List<Integer> findAcceptableSet(
			List<Integer> residualDegrees, Graph<L> graphSoFar, Node<L> hub)
	{
		// * Cache the set of forbidden nodes
		boolean[] forbidden = new boolean[residualDegrees.size()];
		for(Node<L> node : hub.neighbors())
			forbidden[node.index()] = true;
		forbidden[hub.index()] = true;
				
		// * Integrate which nodes are forbidden into the residual degree sequence
		//   If the second element of the pair is false, the node is forbidden.
		//   (this combined combined list allows us to sort the degree sequence 
		//    without losing track of which nodes are forbidden). 
		List<Index> res = new ArrayList<Index>(residualDegrees.size());
		
		for (int i : series(residualDegrees.size()))
			res.add(new Index(residualDegrees.get(i), !forbidden[i]));
		Index hubIndex = res.get(hub.index());
		
		int hubDegree = hubIndex.degree;
		
		// * Pretend that we've connected all but one of the leftmost adjacency 
		//   set to the hub
		
		// ** Set the hub's residual degree to 1
		hubIndex.degree = 1;
		
		// * This comparator will allow us to use quickselect to select the 
		//   largest k allowed elements in res.
		class SpecialComparator implements Comparator<Index>
		{
			@Override
			public int compare(Index o1, Index o2)
			{
				if(o1.allowed == o2.allowed)
					return - Integer.compare(o1.degree, o2.degree);
				if(o1.allowed)
					return -1;
				else
					return 1;
			}
		}
		
		List<Index> leftMost = MaxObserver.quickSelect(hubDegree - 1, res, new SpecialComparator(), false);
						
		for(Index index : leftMost)
		{
			index.degree--;
			index.allowed = false;
		}
		
		// * Sort again
		Collections.sort(res, Collections.reverseOrder());
		
		// * Find the maximum fail degree
		int maxFailDegree = findMaxFailDegree(res);
		
		List<Integer> result = new ArrayList<Integer>();
		
		// * Add all nonforbidden nodes (except the hub) with degree above 
		//   maxFailDegree to the result set
		for(int i : series(residualDegrees.size()))
			if(residualDegrees.get(i) > maxFailDegree 
					&& (! forbidden[i]))
			{
				result.add(i);
			}
		
		return result;
	}
	
	protected static class Index implements Comparable<Index>
	{
		// Accessible fields for optimization
		int degree;
		boolean allowed;
		
		public Index(int degree, boolean allowed)
		{
			this.degree = degree;
			this.allowed = allowed;
		}

		@Override
		public int compareTo(Index o)
		{
			int comparison = Integer.compare(this.degree, o.degree);
			return comparison != 0 ? comparison : Boolean.compare(this.allowed, o.allowed);
		}
		
		public String toString()
		{
			return degree + " " + allowed;
		}
	}
	
	private static List<Integer> degrees(final List<Index> in)
	{
		return new AbstractList<Integer>(){
			
			@Override
			public Integer get(int i)
			{
				return in.get(i).degree;
			}

			@Override
			public int size()
			{
				return in.size();
			}};
	}

	
	/**
	 * @param sequence Should be sorted
	 * @return
	 */
	private static int durfee(List<Integer> sequence)
	{
		for(int j : series(1, sequence.size() + 1))
			if(sequence.get(j - 1) < j-1)
				return j;
		
		return sequence.size();
	}
	
	private void sub(List<Integer> sequence, int... indices)
	{
		for(int i : indices)
			sequence.set(i, sequence.get(i) - 1);
	}
	
	static int sum(List<Integer> seq)
	{
		int sum = 0;
		for(int val : seq)
			sum += val;
		
		return sum; 
	}
	
	public class Result 
	{
		private UGraph<L> graph;
		private double c;
		private double sigma;
		
		public Result(UGraph<L> graph, double c, double sigma)
		{
			this.graph = graph;
			this.c = c;
			this.sigma = sigma;
		}
		
		public UGraph<L> graph()
		{
			return graph;
		}
		public double logC()
		{
			return c;
		}
		public double logSigma()
		{
			return sigma;
		}
	}
	
	public Generator<UGraph<L>> uniform()
	{
		return null;
	}	
}
