package org.nodes.models;

import static java.lang.Math.E;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.nodes.util.Functions.choose;
import static org.nodes.util.Functions.exp2;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Min;
import static org.nodes.util.Functions.log2Sum;
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

import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.Functions;
import org.nodes.util.Generator;
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
	private static final double ALPHA = 0.05;
	private L label = null;
	private boolean check = false;
	private int samples; 
	private List<Integer> sequence;
	private int n;
	
	// * (estimated) logprob for graphs with this sequence 
	private double logProb, logConf, numGraphs, logNumGraphs;
	private double logStdDev;
	private double logStdError, confLower, confUpper;
	private double logEffSampleSize; 
	
	private List<Double> logSamples;
	private double confBootstrapLower, confBootstrapUpper;

	public USequenceModel(Graph<?> data, int samples)
	{
		this.samples = samples;
		sequence = new ArrayList<Integer>(data.size());
		
		for(Node<?> node : data.nodes())
			sequence.add(node.degree());
			
		n = sequence.size();
		
		this.check = check;
		
		compute();
	}
	
	public USequenceModel(List<Integer> sequence, int samples)
	{
		this.samples = samples;
		this.sequence = new ArrayList<Integer>(sequence);
			
		n = sequence.size();
				
		compute();
	}
	
	public List<Double> logSamples()
	{
		return Collections.unmodifiableList(logSamples);
	}
	
	private void compute()
	{
		logSamples = new ArrayList<Double>(samples);
		
		List<Double> logSigmas = new ArrayList<Double>(samples);
		List<Double> logCs     = new ArrayList<Double>(samples);
		
		for(int i : series(samples))
		{
			Result result = nonuniform();
			logSigmas.add(result.logSigma());
			logCs.add(result.logC());
			
			logSamples.add(- result.logSigma() - result.logC());
			
			if(i % 10 == 0) System.out.print('.');
			if(i % 1000 == 0) System.out.println("\n"+i);
		}
		
		double logEstimate = log2Sum(logSamples) - log2(samples);
		
		logNumGraphs = logEstimate;
		numGraphs = pow(2.0, logNumGraphs);
		logProb = - logEstimate;
		
		
		// * Compute the standard deviation
		//   This one gets a little complicated to avoid overflows
		//   let c_i be log cY + log sigmaY for sample i
		//   let log m be the logarithm of the estimated number of graphs
		//   a = 2 * logsum c_i, 
		//   b = logsum(c_i + log m)
		//   c = 2 log m + log n
		//   Then the logartihm of the standard deviation is
		//   1 - 0.5 log(n-1) + 0.5 log(2^a - 2^b + 2^c)

		double a, b, c;
		
		List<Double> cs = new ArrayList<Double>(samples);
		for(int i : series(samples))
			cs.add(- 2.0 * (logSigmas.get(i) + logCs.get(i)));
		
		a = log2Sum(cs); 
		cs = null;
		
		List<Double> cPlus = new ArrayList<Double>(samples);
		for(int i : series(samples))
			cPlus.add(logNumGraphs - logSigmas.get(i) - logCs.get(i));
		
		b = log2Sum(cPlus) + 1;
		
		c = 2.0 * logNumGraphs + log2(samples);
				
		logStdDev = - 0.5 * log2(samples - 1.0) + 0.5 * log2Min(log2Sum(a, c), b);
		
		logStdError = logStdDev - 0.5 * log2(samples);
		
		// * Compute the effective standard error
		logEffSampleSize = 2.0 * log2Sum(logSamples) - a;
		
		confLower = log2Min(logNumGraphs, log2(1.96) + logStdError);
		confUpper = log2Sum(logNumGraphs, log2(1.96) + logStdError);
		
		List<Double> lnSamples = new ArrayList<Double>(logSamples.size());
		for(int i : series(logSamples.size()))
			lnSamples.add(logSamples.get(i) * Math.log(2.0));
		
		double[] bounds = computeBootstrapConfidence(lnSamples, ALPHA);
		confBootstrapLower = bounds[0] * log2(E);
		confBootstrapUpper = bounds[1] * log2(E);
	}
	
	private static final int BOOTSTRAP_SAMPLES = 10000;
	
	/**
	 * 
	 * NOTE:Arguments and return values of this function are in ln space, not log2!
	 * @param lnObservations
	 * @param alpha
	 * @return
	 */
	private double[] computeBootstrapConfidence(List<Double> lnObservations, double alpha)
	{
		int n = lnObservations.size();
		
		// * compute the mean of the log observations
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
		logNormalMean = (lnMean + 0.5 * lnVariance) * log2(E);
		
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
		
		int lowerIndex = (int) Math.floor( (alpha*0.5) * BOOTSTRAP_SAMPLES );
		double t0 = ts.get(lowerIndex);

		int upperIndex = (int) Math.floor( (1.0 - alpha*0.5) * BOOTSTRAP_SAMPLES );
		double t1 = ts.get(upperIndex);
		
		double lowerBound = lnMean + lnVariance * 0.5 - t1 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
		double upperBound = lnMean + lnVariance * 0.5 - t0 * sqrt((lnVariance * (1.0 + lnVariance*0.5)) / n);
		
		return new double[]{lowerBound, upperBound};
	}
	
	private double logNormalMean;
	
	public double logNormalMean()
	{
		return logNormalMean;
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
		return pow(2.0, logEffSampleSize);
	}
	
	public double logStdError()
	{
		return logStdError;
	}

	public double logStdDev()
	{
		return logStdDev;
	}
	
	public double logProb()
	{
		return logProb;
	}
	
	public double logNumGraphs()
	{
		return logNumGraphs;
	}

	public double numGraphs()
	{
		return numGraphs;
	}
	
	@Override
	public double logProb(UGraph<L> graph)
	{
		if(check)
		{
			// ...
		}
		return logProb;
	}
	
	/**
	 * Return the lower edge of the 95% confidence interval for the estimate 
	 * of the compression size (in bits).
	 * 
	 * NOTE: This confidence interval is highly unreliable, and is only included
	 * for comparative purposes.
	 * 
	 * @return
	 */
	public double confidenceNaiveLower()
	{
		return confLower;
	}
	
	public double confidenceNaiveUpper()
	{
		return confUpper;
	}
	
	/**
	 * The conifdence interval based on Angus' parametric bootstrap method
	 * @return
	 */
	public double confidenceBootstrapLower()
	{
		return confBootstrapLower;
	}
	
	public double confidenceBootstrapUpper()
	{
		return confBootstrapUpper;
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
