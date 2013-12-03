package org.nodes.util;

import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static org.apache.commons.math3.util.ArithmeticUtils.binomialCoefficientLog;
import static org.nodes.util.Series.series;

import java.util.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.*;
import java.text.*;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.nodes.Global;
import org.nodes.draw.Point;

/**
 * This class contains several static functions (mainly mathematical)
 * That are used throughout the library.
 * 
 * @author Peter Bloem
  */
public class Functions
{
	public static int factCache = 100;
	public static double[] factorials = new double[factCache + 1];
	public static boolean factsInited = false;
	public static NumberFormat nf = NumberFormat.getNumberInstance();
	
	/**
	 * Return the natural logarithm of the gamma function for the nonnegative integer x.
	 * 
	 * (see "Numerical recipes in c")
	 * @param x The integer to return ln(gamma(x)) for. x Can't   
	 * @return
	 */
		
	public static double logGamma(double in)
	{
		if(in < 0)
			throw new IllegalArgumentException("Cant derive log gamma for negative integer " + in + ".");
		
		double x, y, tmp, ser;
		
		double[] cof = new double[6];
		cof[0] = 76.18009172947146; 
		cof[1] = -86.50532032941677;			
		cof[2] = 24.01409824083091;
		cof[3] = -1.231739572450155;
		cof[4] = 0.1208650973866179e-2;
		cof[5] = -0.5395239384953e-5;

		x = in;
		y = in;

		tmp = x+5.5;
		tmp -= (x+0.5)* Math.log(tmp);
		ser = 1.000000000190015;
		
		for(int j=0;j<=5;j++) 
			ser += cof[j]/++y;
		
		return -tmp + Math.log(2.5066282746310005 * ser / x);
	}
	
	public static double logFactorial(int n, double base)
	{
		return logFactorial(n) / Math.log(base);
	}
	
	
	/**
	 * Calculates the log of the factorial of an integer n. Uses gamma
	 * functions for n > 100;
	 * @param n
	 * @return
	 */
	public static double logFactorial(int n)
	{
		//initialize the lookup table for values < 101 
		if(!factsInited)
		{
			factorials[0] = 1.0;
			for(int i = 1; i <= factCache; i++)
				factorials[i] = i * factorials[i-1];
						
			factsInited = true;
		}			
		
		if (n <= factCache) 
			return Math.log(factorials[n]);
		else 
			return logGamma(n + 1.0);
	}
	
	/**
	 * Calculates the log of the factorial of an integer n. Uses gamma
	 * functions.
	 * @param n 
	 * @return
	 */
	public static double logFactorial(double n)
	{
		if(n < 0.0) throw new IllegalArgumentException("Parameter n ("+n+")should be positive");
		return logGamma(n + 1.0);
	}
	
	
	/**
	 * Calculates a factorial of an integer n. Uses gamma functions for factorials > 100;
	 * @param n
	 * @return
	 */
	public static double factorial(int n)
	{
		//initialize the lookup table for values < 101 
		if(!factsInited)
		{
			factorials[0] = 1.0;
			for(int i = 1; i <= factCache; i++)
				factorials[i] = i * factorials[i-1];
						
			factsInited = true;
		}			

		if (n <= factCache) 
			return factorials[n];
		else 
			return Math.exp(logGamma(n + 1.0));
	}
	
	/**
	 * Calculates the set overlap between two sets.
	 * 
	 * @param a the first set
	 * @param b the second set
	 * @return The the number of elements present in both sets. 
	 */
	public static int overlap(Collection<?> a, Collection<?> b)
	{
		Collection<?> smallest;
		Collection<?> largest;
		if(a.size() >= b.size())
		{
			largest = a;
			smallest = b;
		}else
		{
			largest = b;
			smallest = a;
		}
		
		int overlap = 0;
		Iterator<?> it = smallest.iterator();
		while(it.hasNext())
		{
			if(largest.contains(it.next()))
				overlap++;			
		}
		
		return overlap;
	}
	
	
	public static String matrixToString(int[][] n)
	{
		
		if(n == null) return "null";
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < n.length; i++)
		{
			if(i > 0)
				sb.append("\n");
			for(int j = 0; j < n[i].length;j++)
			{
				sb.append(n[i][j]).append("\t");
			}			
		}
		
		return sb.toString();  
	}
	
	public static String matrixToString(double[][] n)
	{
		nf.setMinimumFractionDigits(2);
		
		if(n == null) return "null";
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < n.length; i++)
		{
			if(i > 0)
				sb.append("\n");
			for(int j = 0; j < n[i].length;j++)
			{
				sb.append(nf.format(n[i][j])).append("\t");
			}			
		}
		
		return sb.toString();  
	}
	
	/**
	 * Calculates the mean and variance from a collection of numbers.
	 *  
	 * @param in
	 * @param sample If true, the sample variance is computed (dividing by n-1, instead of n)
	 * @returnA pair containing the mean first and the variance second 
	 */
	public static Pair<Double, Double> getMeanAndVariance(Collection<? extends Number> in, boolean sample)
	{
		Iterator<? extends Number> it = in.iterator();
		int n = 0;
		double total = 0.0;
		while(it.hasNext())
		{
			n++;
			total += it.next().doubleValue();
		}
		
		double mean;
		if(n == 0)
			mean = 0.0;				
		else		
			mean = total / (double)n;
 		
		it = in.iterator();
		total = 0.0;
		// compute variance
		while(it.hasNext())
			total += Math.pow((it.next().doubleValue() - mean), 2.0);
		
		double variance;
		if(sample)
			if(n == 1) variance = 0.0;
			else variance = total / (double)(n-1);
		else 		
			if(n == 0) variance = 0.0; 
			else variance = total / (double)(n);
		
		return new Pair<Double, Double>(new Double(mean), new Double(variance));
	}
	
	/**
	 * Tokenizes an input sentences by white space, and returns a vector of the 
	 * tokens. Useful for quick testing.
	 * 
	 * 
	 */
	public static List<String> sentence(String sentence)
	{
		StringTokenizer st = new StringTokenizer(sentence);
		
		Vector<String> result = new Vector<String>();
		
		while(st.hasMoreTokens())
			result.add(st.nextToken());
			
		return result;
	}
	
	private static long ticTime = -1;
	public static void tic()
	{
		ticTime = System.currentTimeMillis();
	}
	
	/** 
	 * Returns the number of seconds since tic was last called. <br/>
	 * <br/>
	 * Not thread-safe (at all).
	 * 
	 * @return A double representing the number of seconds since the last call 
	 *         to tic(). 
	 */
	public static double toc()
	{
		if(ticTime  < 0)
			throw new IllegalStateException("Tic has not been called yet");
		return (System.currentTimeMillis() - ticTime)/1000.0;
	}

	/**
	 *  2 log
	 */
	public static double log2(double x)
	{
		return Math.log10(x) / Math.log10(2.0);
	}

	public static double logChoose(double sub, double total, double base)
	{
		return logChoose(sub, total) / Math.log(2.0);
	}	
	
	
	/**
	 * The binary logarithm of the choose function (ie. the binomial coefficient)
	 */
	public static double logChoose(double sub, double total)
	{
		if(sub <= Integer.MAX_VALUE && total <= Integer.MAX_VALUE 
				&& sub == (int)sub && total == (int)total)
			return binomialCoefficientLog((int)total, (int)sub);
		
		double n = total, k = sub;
		return logFactorial(n) - logFactorial(k) - logFactorial(n - k); 
	}
	 
	/**
	 * Equals method for objects that takes into account null values
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equals(Object a, Object b)
	{
		if(a == null)
			return b == null;
		
		return a.equals(b);
	}
	
	/**
	 * Simple random choice from a collection
	 * 
	 * @param in
	 * @return
	 */
	public static <T> T choose(Collection<T> in)
	{
		return choose(in, Global.random());
	}
	
	/**
	 * Simple random choice from a collection
	 * 
	 * If the input is a list, processing time is constant, otherwise the 
	 * collection is traversed to the randomly drawn index.
	 * 
	 * @param in
	 * @return A random element from the collection
	 */
	public static <T> T choose(Collection<T> in, Random random)
	{
		int draw = random.nextInt(in.size());
		if(in instanceof List)
			return ((List<T>)in).get(draw);
		
		int c = 0;
		for(T t : in)
			if(c < draw)
				c++;
			else
				return t;
		
		// * Unreachable code to make the compiler happy
		assert(false); 
		return null; 
	}
	
	/**
	 * Returns a lightweight, reversed list, backed by the original.
	 * @param <T>
	 * @return
	 */
	public static <T> List<T> reverse(List<T> list)
	{
		return new ReverseList<T>(list);
	}

	public static class ReverseList<T> extends AbstractList<T>
	{
		private List<T> master;
		
		public ReverseList(List<T> master)
		{
			this.master = master;
		}

		@Override
		public T get(int index)
		{
			return master.get((master.size()-1)-index);
		}

		@Override
		public int size()
		{
			return 0;
		}
	
	}
	
	
	/**
	 * Draws an integer according to specified probabilities.
	 * 
	 * Draws a random integer i such that i's probability of being drawn is
	 * equal to the i'th double returned by the collection's iterator.
	 * 
	 * @param probabilities A collection of doubles, summing to 1.0, whose n'th 
	 * 			element defines the probability of n being drawn (first 
	 * 			element is 0)
	 * @param sum The sum of the values in probabilities. This valeu is not 
	 * 				calculated from the collection for reasons of efficiency.  
	 */
	public static int draw(Collection<Double> probabilities, double sum)
	{
		return draw(probabilities, sum, Global.random());
	}
	
	public static int draw(Collection<Double> probabilities, double sum, Random rand)
	{
		// * select random element
		double draw = rand.nextDouble();
		double total = 0.0;
		int elem = 0;
		for(double probability : probabilities)
		{
			total += probability/sum;
			if(total > draw)
				break;
			
			elem++;
		}
		
		// account for floating point problems
		if(elem >= probabilities.size())
		{
			elem = probabilities.size()-1;
//			Global.logger.info("Index larger than probabilities.size(): draw = "+draw+", total = "+total+", sum = "+sum+". ");
		}

		return elem;
	}
	
	public static <L extends List<Double>> void toCSV(List<L> data, File csvFile)
			throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(csvFile));
		
		boolean first;
		for(L point : data)
		{
			first = true;
			for(Double val : point)
			{
				if(first) first = false;
				else out.write(", ");
				
				out.write(val.toString());				
			}
			out.write("\n");
		}
		
		out.close();
	}
	
	public static int mod(int x, int range)
	{
		int r = x % range;
		if(r >= 0)
			return r;
		
		return range + x;
	}
	
	public static Comparator<Number> numberComparator()
	{
	 return new NumberComparator();
	}

	public static class NumberComparator implements Comparator<Number>
	{
		@Override
		public int compare(Number n1, Number n2)
		{
			return Double.compare(n1.doubleValue(), n2.doubleValue());
		}
	
	}
	
	/**
	 * Probabilistically rounds the input. The result of this 
	 * method is at least floor(in), and is ceil(in) with probability
	 * (in - floor(in)) 
	 * 
	 * @param in
	 */
	public static double probRound(double in)
	{
		if(Global.random().nextDouble() < in - floor(in))
			return ceil(in);
		
		return(floor(in));
	}

	/**
	 * Samples k distinct values from the first 'size' natural numbers
	 * 
	 * @param k The number of natural numbers to return
	 * @param size The maximum integer possible + 1
	 * @return A uniform random choice from all sets of size k of distinct 
	 * 	integers below the 'size' parameter.
	 */
	public static List<Integer> sample(int k, int size)
	{
		if(0 > k || k > size)
			throw new IllegalArgumentException("Argument k ("+k+") must be non-negative and larger than size ("+size+")");
		// * The algorithm we use basically simulates having an array with the 
		//   values of 0 to n - 1 at their own indices, and for each i, choosing
		//   a random index above it and swapping the two entries.
		//
		//   Since we expect low k, most entries in this array will stay at 
		//   their original index and we only stores the values that deviate.
		
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
				
		for(int i : series(k))
		{
			// Sample a random integer above or equal to i and below 'size'
			int draw = Global.random().nextInt(size - i) + i;
			
			int drawValue = map.containsKey(draw) ? map.get(draw) : draw;
			int iValue = map.containsKey(i) ? map.get(i) : i; 
			
			// swap the values
			map.put(i, drawValue);
			map.put(draw, iValue);
		}
		
		List<Integer> result = new ArrayList<Integer>(k);
		for(int i : series(k))
			result.add(map.get(i));
		
		return result;
	}

	/**
	 * Uses natural logarithms
	 * @param logA
	 * @param logB
	 * @return
	 */
	public static double logSum(double logA, double logB)
	{
		double logMin, logMax;
		
		if(logA < logB)
		{
			logMin = logA;
			logMax = logB;
		} else {
			logMax = logA;
			logMin = logB;
		}
			
		return log(exp(logMin) * (1 + exp(logMax - logMin)));
	}
	
	public static double exp2(double x)
	{
		return Math.exp(x);
	}
	
    public static <T> Set<T> asSet(T... a) {
        Set<T> set = new LinkedHashSet<T>();
        for(T t : a)
        	set.add(t);
        
        return set;
    }
    
    public static <T extends Comparable<? super T>> Comparator<T> natural()
    {
    	return new NaturalComparator<T>();
    }
    
    public static class NaturalComparator<T extends Comparable<? super T>> 
    	implements Comparator<T> 
    {
	
	    @Override
	    public int compare(T first, T second) 
	    {
	        return first.compareTo(second);
	    }
    }
}
