package org.nodes.util;

import java.io.Serializable;

/**
 * Simple semi-immutable pair to store two objects
 *
 */
public class Pair<A, B> implements Serializable
{
	private static final long serialVersionUID = -6354218720393781405L;

	private A first;
	private B second;

	public Pair(A one, B two)
	{
		this.first = one;
		this.second = two;
	}

	public A first()
	{
		return first;
	}

	public B second()
	{
		return second;
	}

	public boolean equals(Object obj)
	{
		if(obj instanceof Pair)
		{
			Pair<?, ?> pair2 = (Pair) obj;
			
			return (
				first == null ? pair2.first == null : first.equals(pair2.first) && 
				second == null ? pair2.second == null : second.equals(pair2.second)
				);
		}

		return false;
	}

	/**
	 * Returns the hashcode for this pair. The method to calculate the hashcode 
	 * is based on the method used by java's AbstractList 
	 * 
	 * @return The hashcode for this pair
	 */
	public int hashCode()
	{
		int hashCode = 1;
		
		hashCode = 31*hashCode + (first==null ? 0 : first.hashCode());
		hashCode = 31*hashCode + (second==null ? 0 : second.hashCode());
		
		return hashCode;
	}

	public String toString()
	{
		return "[" + first + ", " + second + "]";
	}
}