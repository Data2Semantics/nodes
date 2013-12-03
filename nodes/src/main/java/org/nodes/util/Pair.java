package org.nodes.util;

/**
 * Simple semi-immutable pair to store two objects
 *
 * To sort these with a comparator, a specific comparator for
 * the two datatypes is needed.
 */
public class Pair<A, B>
{
	A one;
	B two;

	public Pair(A one, B two)
	{
		this.one = one;
		this.two = two;
	}

	public A first()
	{
		return one;
	}

	public B second()
	{
		return two;
	}

	public boolean equals(Object obj)
	{
		if(obj instanceof Pair)
		{
			Pair<?, ?> pair2 = (Pair) obj;
			
			return (
				one == null ? pair2.one == null : one.equals(pair2.one) && 
				two == null ? pair2.two == null : two.equals(pair2.two)
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
		
		hashCode = 31*hashCode + (one==null ? 0 : one.hashCode());
		hashCode = 31*hashCode + (two==null ? 0 : two.hashCode());
		
		return hashCode;
	}

	public String toString()
	{
		return "[" + one + ", " + two + "]";
	}
}