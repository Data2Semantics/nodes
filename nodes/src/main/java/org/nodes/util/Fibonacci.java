package org.nodes.util;

import java.util.Arrays;

public class Fibonacci
{
	public static final double PHI = 1.61803398874989484820;

	public static final int MAX_INDEX = 92;
	
	public static final long[] numbers = new long[MAX_INDEX+1];
	
	static {
		numbers[0] = 0;
		numbers[1] = 1;
		
		for(int i = 2; i < numbers.length; i ++)
			numbers[i] = numbers[i-1] + numbers[i-2];
	}
	
	/**
	 * Tests whether the given number is a fibonacci number
	 * @param n
	 * @return
	 */
	public static boolean isFibonacci(long n)
	{
		long s = 5 * n * n;
		return Functions.isSquare(s + 4) || Functions.isSquare(s - 4);
	}
	
	/**
	 * Returns the i-th fibonacci number. The numbers are zero indexed, so that 
	 * the 0th, first and second numbers are 0, 1, 1 respectively.
	 * 
	 * @param i
	 * @return
	 */
	public static long get(int i)
	{
		return numbers[i];
	}
	
	/**
	 * Returns the approximate index of the given numebr. If the number is not a
	 * fibonacci number, a non-integer value is returned indicating the two 
	 * nearest fibonacci numbers (ie. if the returned value is 33.2, the number 
	 * is above the 33rd fibonacci number and below the 34th).
	 *      
	 * @param n
	 * @return
	 */
	public static double getIndexApprox(long n)
	{
		return Math.log(n * sqrt5 + 0.5)/Math.log(PHI);
	}
	
	private static final double sqrt5 = Math.sqrt(5.0); 

	/**
	 * Returns the index for a given fibonacci number
	 * @param n
	 * @return
	 */
	public static int getIndex(long n)
	{
		if(n == 0)
			return 0;
		if(n == 1)
			return 2;
		
		return (int)Math.round(getIndexApprox(n));
	}
	
	/**
	 * Returns the previous fibonacci number.
	 * 
	 * If the input is one, the function returns 1.
	 * @param n
	 * @return
	 */
	public static long previous(long n)
	{
		int i = getIndex(n);
		return i == 0 ? 0 : get(i-1);
	}
	
	public static long next(long n)
	{
		int i = getIndex(n);
		return get(i+1);
	}

	
}
