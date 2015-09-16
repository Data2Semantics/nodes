package org.nodes.models;

import static java.lang.Math.E;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import static org.nodes.util.Functions.choose;
import static org.nodes.util.Functions.exp2;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Min;
import static org.nodes.util.Functions.log2Sum;
import static org.nodes.util.LogNum.fromDouble;
import static org.nodes.util.Pair.first;
import static org.nodes.util.Series.series;
import static org.nodes.util.bootstrap.LogNormalCI.LN2;
import static org.nodes.util.bootstrap.LogNormalCI.LOGE;

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
import java.util.Vector;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.MapDTGraph;
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
public class DSequenceEstimator<L> 
{	
	public static final boolean PICK_CANDIDATE_BY_DEGREE = true;
	private L label = null;
	private List<D> sequence;
	
	private List<Double> logSamples = new Vector<Double>();

	public DSequenceEstimator(DGraph<?> data, int samples)
	{
		this(data);
		
		int dotPer = (int)Math.ceil(samples/100.0);
		for(int i : series(samples))
		{
			if(i % dotPer == 0)
				System.out.print('.');
			nonuniform();
		}
		System.out.println();
	}
	
	public DSequenceEstimator(DGraph<?> data)
	{

		sequence = new ArrayList<D>(data.size());
		
		for(DNode<?> node : data.nodes())
		{
			int in = node.inDegree(), out = node.outDegree();
			sequence.add(new D(in, out));
		}
	}
	
	public DSequenceEstimator(List<Integer> inSequence, List<Integer> outSequence, int samples)
	{
		this(inSequence, outSequence);

		Functions.tic();
		for(int i : series(samples))
		{
			nonuniform();
			if(Functions.toc() > 10)
				System.out.println("\r " + logSamples.size() + "samples completed");
		}
	}
	
	public DSequenceEstimator(List<Integer> inSequence, List<Integer> outSequence)
	{
		this.sequence = new ArrayList<D>(inSequence.size());
		
		for(int i : series(inSequence.size()))
			sequence.add(new D(inSequence.get(i), outSequence.get(i)));
	}
	
	public DSequenceEstimator(List<D> sequence)
	{
		this.sequence = new ArrayList<D>(sequence);
	}
	
	public List<Double> logSamples()
	{
		return Collections.unmodifiableList(logSamples);
	}
	
	public static final int BOOTSTRAP_SAMPLES = 10000;
	private static final double SMOOTH = 0.0;
	
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
		double logMean = logNumGraphsNaive();
		
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
		return - logNumGraphsNaive();
	}
	
	public double logNumGraphsNaive()
	{
		return log2Sum(logSamples) - log2(logSamples.size());
	}
	
	public double logNumGraphsML()
	{
		double ml2Mean = 0.0;
		for(int i : series(logSamples.size()))
			ml2Mean += logSamples.get(i);
		ml2Mean /= (double) logSamples.size();
		
		// * compute the variance of the log observations
		double sum2Sq = 0;
		for(int i : series(logSamples.size()))
		{
			double diff = logSamples.get(i) - ml2Mean;
			sum2Sq += diff * diff;
		}
		double ml2Variance = sum2Sq / (double) logSamples.size();
		return ml2Mean + 0.5 * ml2Variance * LN2; 
		// Since we store the samples in lag base 2, we need the LN2 multiplier
		// on the variance
	}

	public double numGraphs()
	{
		return pow(2.0, logNumGraphsML());
	}
	
	public double logProb(UGraph<L> graph)
	{
		// TODO: check if graph matches sequence
		
		return - logNumGraphsML();
	}
	
	/**
	 * Samples the given number of graphs from this sequence model. The 
	 * resulting log-probabilities are stored in the model.
	 * 
	 * Multithreaded
	 */
	public void nonuniform(final int samples, final int numThreads)
	{
		final int perThread = samples/numThreads;
		final int rem = samples - perThread * numThreads;
		
		List<Thread> threads = new ArrayList<Thread>(numThreads);
		for(final int t : series(numThreads))
		{
			Thread thread = new Thread() {
				public void run()
				{
					for(int i : series(perThread + (t == 0 ? rem : 0)))
						nonuniform();
				}
			};
			threads.add(thread);
		}
		
		for(Thread thread : threads)
		{
			thread.start();
		}

		// * Wait until all threads are finished
		try
		{
			for(Thread thread : threads)
				thread.join();
		} catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Generates a random graph with the given degree  
	 * 
	 * @return
	 */
	public Result nonuniform()
	{
		// * Create an empty graph
		DGraph<L> graph = new MapDTGraph<L, String>();
		
		int n = sequence.size();
		
		for(int i : Series.series(n))
			graph.add(label);
		
		// * Residual degrees
		List<D> sequence = new ArrayList<D>(this.sequence);
		
		// * The log number of edge sequences producing the graph that will be
		//   sampled
		double logCY = 0; 
		// * The log probability of the edge sequence that we've sampled 
		double logSigY = 0;
		
		while(sum(sequence) > 0) // TODO: keep running value of sum
		{			
			// * Find the smallest i whose index is nonzero and minimal
			int fromIndex = -1, fromOut = Integer.MAX_VALUE;
			for(int index : Series.series(n))
			{
				int outDegree = sequence.get(index).out();

				if(outDegree != 0 && outDegree < fromOut)
				{
					fromIndex = index;
					fromOut = outDegree;
				}
			}
			
			logCY += Functions.logFactorial(fromOut, 2.0);
			
			while(sequence.get(fromIndex).out() > 0)
			{
				DNode<L> from = graph.get(fromIndex);
				
				List<Integer> toCandidates = findAcceptableSet(sequence, graph, from);
//				System.out.println("candidates: " + toCandidates);
				
				int i;
				if(toCandidates.isEmpty())
				{
					throw new IllegalStateException("Candidates is empty. Residual degree sequence: " + sequence);
				}
				
				if(PICK_CANDIDATE_BY_DEGREE) // * choose the candidate by weighted indegree
				{
					List<Double> candidateWeights = new ArrayList<Double>(n);
					int sum = 0;
					
					for(int toIndex : toCandidates)
					{
						candidateWeights.add((double)sequence.get(toIndex).in() + SMOOTH);
						sum += sequence.get(toIndex).in() + SMOOTH;
					}
					
					i = choose(candidateWeights, sum);
					logSigY += log2(candidateWeights.get(i)/sum);

				} else // * uniform choice
				{
					i = Global.random().nextInt(toCandidates.size());
					logSigY += - log2(toCandidates.size());
				}
				
				
				int toIndex = toCandidates.get(i);
//				System.out.println("candidate: " + toIndex);
				
				Node<L> to = graph.get(toIndex);
				
				from.connect(to);
				
				sequence.set(fromIndex, sequence.get(fromIndex).decOut());
				sequence.set(toIndex, sequence.get(toIndex).decIn());

			}
		}
		
		logSamples.add(- logCY - logSigY);
		
		return new Result(graph, logCY, logSigY);
	}
	
	private static int numZeroes(List<Integer> seq)
	{
		int numZeroes = 0;
		int i = seq.size();
		
		while(i > 0 && seq.get(--i) <= 0)
			numZeroes ++;
			
		return numZeroes;
	}
	
	/**
	 * Find the maximum fail degree
	 * 
	 * @param seq The residual degree sequence (sorted in descending order). 
	 *   This sequence must be graphical
	 * @param allowed A set of nodes that we are not allowed to connect the 
	 *   hub to (should include the hub itself).
	 * @return The maximum fail degree for this sequence. 0 if no fail degrees
	 *   exist. The logic is that connecting to a node with residual degree 0 is
	 * 	 always illegal.
	 */
	protected static D findMaxFailDegree(List<Index> seq, int j)
	{		
		if (seq.size() == 0)
			return new D(-1, -1);

		// * find k0: the smallest k such that the two parts of the FR condition 
		//   are equal 
		
		int n = seq.size();
		
		List<Integer> out = outI(seq);
		List<Integer> in  = inI(seq);
		
		List<Integer> g1 = g1(out);
		List<Integer> s  = s(out);

		int lk = 0;
		int rk = 0;
		int squigk = 0;
		
		for(int k : series( 1, n))
		{
			// * update lk, squigk, rk
			lk += in.get(k - 1);
				
			if(k == 1)
				rk = n - 1 - g(1, 0, out);
			else
			    rk += n - squigk - i(out.get(k-1) >= k);
			
//			System.out.println("j = " + j + ", k = " + k);
//			System.out.println(lk + " " + rk);
			
			if(!(k == 1 && j == 0))
				if(lk == rk) 
				{
					int index = k;
					while(index < seq.size() && ! seq.get(index).allowed)
						index++;
					if(index >= seq.size())
						return new D(-1, -1);
					
					return seq.get(index).degree;
				}
			
			// * update squigk
			if(k == 1)
				squigk = g(1, 0, out) + g(1, 1, out); 
			else	
				squigk += g1.get(k-1) + s.get(k-1);
		}		
		
		return new D(-1, -1);
	} 
	
	private static List<Integer> outI(final List<Index> seq)
	{
		return new AbstractList<Integer>()
		{
			public Integer get(int index)
			{
				return seq.get(index).degree.out();
			}

			public int size()
			{
				return seq.size();
			}
		};
	}
			
	private static List<Integer> inI(final List<Index> seq)
	{
		return new AbstractList<Integer>()
		{
			public Integer get(int index)
			{
				return seq.get(index).degree.in();
			}

			public int size()
			{
				return seq.size();
			}
		};
	}	
	
	public static List<Integer> out(final List<D> seq)
	{
		return new AbstractList<Integer>()
		{
			public Integer get(int index)
			{
				return seq.get(index).out();
			}

			public int size()
			{
				return seq.size();
			}
		};
	}
			
	public static List<Integer> in(final List<D> seq)
	{
		return new AbstractList<Integer>()
		{
			public Integer get(int index)
			{
				return seq.get(index).in();
			}

			public int size()
			{
				return seq.size();
			}
		};
	}	
	
	/**
	 * TODO: sort the input
	 * 
	 * @param in
	 * @param out
	 * @return
	 */
	public static boolean isGraphical(List<Integer> in, List<Integer> out)
	{
		assert(in.size() == out.size());
		
		int n = in.size();
		
		// * the basics
		for(int d : in)
			if(d > n - 1)
				return false;
		
		for(int d : out)
			if(d > n - 1)
				return false;
		
		if(sums(in) != sums(out))
			return false;
		
		List<Integer> g1 = g1(out);
		List<Integer> s = s(out);

		int lk = 0;
		int rk = 0;
		int squigk = 0;
		
		for(int k : series(1, n))
		{
			// * update lk, squigk, rk
			lk += in.get(k - 1);
				
			if(k == 1)
				rk = n - 1 - g(1, 0, out);
			else
			    rk += n - squigk - i(out.get(k-1) >= k); 
			
			if(lk > rk)
				return false;
			
			// * update squigk
			if(k == 1)
				squigk = g(1, 0, out) + g(1, 1, out); 
			else	
				squigk += g1.get(k-1) + s.get(k-1);
		}
		
		return true;
	}
	
	protected static List<Integer> g1(List<Integer> out)
	{
		int n = out.size();
		List<Integer> g1 = new ArrayList<Integer>(n+1);
		for(int i : series(n+1))
			g1.add(0);
		
		g1.set(out.get(0), 1);
		for(int di : out.subList(1, n))
			if (di > 0)
			{
				g1.set(di-1, g1.get(di-1) + 1); 
			}
	
		return g1;
	}
	
	protected static List<Integer> s(List<Integer> out)
	{
		int n = out.size();
		List<Integer> s = new ArrayList<Integer>(n);
		for(int i : series(n))
			s.add(0);
		
		for(int t : series(2, n + 1))
		{
			int dt = out.get(t-1);
					
			int k = dt + 1; 
			if(t <= k - 1)
				s.set(k-1, s.get(k-1) + 1);
			
			k = dt;
			if(t <= k)
				s.set(k-1, s.get(k-1) - 1);
		}

		return s;
	}

	/**
	 * G_k(p) from the paper
	 */
	public static int g(int k, int p, List<Integer> out)
	{
		int sum = 0;
		for(int i : series(1, out.size() + 1))
			if(p == out.get(i-1) + i(i <= k))
				sum++;
		
		return sum;
	}
	
	private static int i(boolean truth)
	{
		return truth ? 1 : 0;
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
	public <L> List<Integer> findAcceptableSet(
			List<D> residualDegrees, DGraph<L> graphSoFar, DNode<L> hub)
	{
		// * Cache the set of forbidden nodes
		boolean[] forbidden = new boolean[residualDegrees.size()];
		for(DNode<L> node : hub.out())
			forbidden[node.index()] = true;
		forbidden[hub.index()] = true;
				
		// * Integrate which nodes are forbidden into the residual degree sequence
		//   If the boolean element of the Index is false, the node is forbidden.
		//   (this combined list allows us to sort the degree sequence 
		//    without losing track of which nodes are forbidden). 
		List<Index> res = new ArrayList<Index>(residualDegrees.size());
		
		for (int i : series(residualDegrees.size()))
			res.add(new Index(residualDegrees.get(i), !forbidden[i], i));
		Index hubIndex = res.get(hub.index());
		
		D hubDegree = hubIndex.degree; 
		
		// * Pretend that we've connected all but one of the leftmost adjacency 
		//   set to the hub
		
		// ** Set the hub's residual degree to 1
		hubIndex.degree = hubIndex.degree.outToOne();
		
		// * This comparator will allow us to use quickselect to select the 
		//   largest k allowed elements in res 
		class SpecialComparator implements Comparator<Index>
		{
			@Override
			public int compare(Index o1, Index o2)
			{
				if(o1.allowed == o2.allowed)
					return - o1.degree.compareTo(o2.degree);
				if(o1.allowed)
					return -1;
				else
					return 1;
			}
		}
		
		List<Index> leftMost = MaxObserver.quickSelect(hubDegree.out() - 1, res, new SpecialComparator(), false);
						
		for(Index index : leftMost)
		{
			index.degree = index.degree.decIn();
			index.allowed = false;
//			System.out.println("lm " + index );
		}
		
		// * Sort again
		Collections.sort(res, Collections.reverseOrder());
//		System.out.println("res " + res);		
		
		int j = res.indexOf(hubIndex);
		
		// * Find the maximum fail degree
		D maxFailDegree = findMaxFailDegree(res, j);
//		System.out.println("maxfaildegree = "  + maxFailDegree);
				
		List<Integer> result = new ArrayList<Integer>(res.size());
		
		// * Add all nonforbidden nodes with degree above 
		//   maxFailDegree to the result set
		for(int i : series(residualDegrees.size()))
			if(residualDegrees.get(i).compareTo(maxFailDegree) > 0
				&& (! forbidden[i]))
			{
				result.add(i);
			}
		
		return result;
	}
	
	protected static class Index implements Comparable<Index>
	{
		// Accessible fields for optimization
		D degree;
		boolean allowed;
		int index;
		
		public Index(D degree, boolean allowed, int index)
		{
			this.degree = degree;
			this.allowed = allowed;
			this.index = index;
		}

		@Override
		public int compareTo(Index o)
		{
			int comparison = this.degree.compareTo(o.degree);
			return comparison != 0 ? comparison : Boolean.compare(this.allowed, o.allowed);
		}
		
		public String toString()
		{
			return degree + "_" + allowed;
		}
	}
	
	private static List<D> degrees(final List<Index> in)
	{
		return new AbstractList<D>(){
			
			@Override
			public D get(int i)
			{
				return in.get(i).degree;
			}

			@Override
			public int size()
			{
				return in.size();
			}};
	}
	
	private int sum(List<D> seq)
	{
		int sum = 0;
		for(D p : seq)
			sum += p.in() + p.out();
		
		return sum;
	}
	
	private static int sums(List<Integer> seq)
	{
		int sum = 0;
		for(int p : seq)
			sum += p;
		
		return sum; 
	}
	
	public class Result 
	{
		private DGraph<L> graph;
		private double c;
		private double sigma;
		
		public Result(DGraph<L> graph, double c, double sigma)
		{
			this.graph = graph;
			this.c = c;
			this.sigma = sigma;
		}
		
		public DGraph<L> graph()
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

		@Override
		public String toString()
		{
			return graph + " c=" + c + ", sig=" + sigma + "]";
		}
		
		
	}
	
	public static <L> List<D> sequence(DGraph<L> graph)
	{
		List<D> list = new ArrayList<D>(graph.size());
		for(DNode<L> node : graph.nodes())
			list.add(new D(node.inDegree(), node.outDegree()));
		
		return list;
	}
	
	public static final class D implements Comparable<D> {
		int in;
		int out;
		
		public D(int in, int out)
		{
			this.in = in;
			this.out = out;
		}
		
		public D outToOne()
		{
			return new D(in, 1);
		}

		public D decIn()
		{
			return new D(in - 1, out);
		}

		public D decOut()
		{
			return new D(in, out - 1);
		}

		public int in()
		{
			return in;
		}
		
		public int out()
		{
			return out;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + in;
			result = prime * result + out;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			
			D other = (D) obj;

			if (in != other.in)
				return false;
			if (out != other.out)
				return false;
			return true;
		}

		@Override
		public int compareTo(D o)
		{
			int c = Integer.compare(in, o.in);
			if(c != 0)
				return c;
			return Integer.compare(out, o.out);
		}

		@Override
		public String toString()
		{
			return in + "_" + out;
		}
		
		
	}
}
