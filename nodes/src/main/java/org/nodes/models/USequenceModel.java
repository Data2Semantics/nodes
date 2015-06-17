package org.nodes.models;

import static java.lang.Math.min;
import static java.lang.Math.pow;
import static org.nodes.util.Functions.choose;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Min;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Graph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.util.Functions;
import org.nodes.util.Generator;
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
	private L label = null;
	private boolean check = false;
	private int samples; 
	private List<Integer> sequence;
	private int n;
	
	// * (estimated) logprob for graphs with this sequence 
	private double logProb, logConf, numGraphs, logNumGraphs;
	private double logStdDev;
	private double logStdError;
	private double logEffSampleSize; 

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
	
	private void compute()
	{
		List<Double> logSigmas = new ArrayList<Double>(samples);
		List<Double> logCs     = new ArrayList<Double>(samples);
		
		for(int i : series(samples))
		{
			Result result = nonuniform();
			logSigmas.add(result.logSigma());
			logCs.add(result.logC());
			
			if(i % 1000 == 0) System.out.println(i);
		}

		List<Double> series = new ArrayList<Double>(samples);
		for(int i : series(samples))
			 series.add(- logSigmas.get(i) - logCs.get(i));
		
		double logEstimate = log2Sum(series) - log2(samples);
		
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
		logEffSampleSize = 2.0 * log2Sum(series) - a;
		
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
	 * Return the 99% confidence interval of the estimate (in bits).
	 * 
	 * Note that this interval may be misleading, and the effective sample 
	 * size should be considered as well.
	 * 
	 * @return
	 */
	public double[] confidence()
	{
		return new double[2];
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
		
		while(sum(sequence) > 0) // keep running value of sum
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
				List<Double> candidateWeights = new ArrayList<Double>(n);
				int sum = 0;
				
				for(int toIndex : series(n))
					if(
							toIndex != fromIndex && 
							sequence.get(toIndex) > 0 && 
							! graph.get(fromIndex).connected(graph.get(toIndex)))
					{		
						// * check if the sequence is still graphical if we subtract 
						//   1 from i and j
						List<Integer> newSequence = new ArrayList<Integer>(sequence);
						sub(newSequence, fromIndex, toIndex);
						
						if(isGraphical(newSequence))
						{
							toCandidates.add(toIndex);
							candidateWeights.add((double)sequence.get(toIndex));
							sum += sequence.get(toIndex);
						}
					}
				
				int i = choose(candidateWeights, sum);
				
				if(i == -1)
				{
					System.out.println("- " + sequence);
					System.out.println("- " + toCandidates);
					System.out.println("- " + candidateWeights);
				}
				
				int toIndex = toCandidates.get(i);
				Node<L> to = graph.get(toIndex);
								
				logSigY += log2(candidateWeights.get(i)/sum);
				
				to.connect(from);
				
				sub(sequence, fromIndex, toIndex);
			}
		}
		
		return new Result(graph, logCY, logSigY); 
	}
	
	public static boolean isGraphical(List<Integer> sequence)
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
	
	private static int sum(List<Integer> seq)
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
