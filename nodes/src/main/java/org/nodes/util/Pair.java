package org.nodes.util;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Comparator;
import java.util.List;

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
	
	public static class PairComparator<A, B> implements Comparator<Pair<A, B>>
	{
		private Comparator<A> aComparator;
		private Comparator<B> bComparator;
		
		public PairComparator(
				Comparator<A> aComparator,
				Comparator<B> bComparator)
		{
			this.aComparator = aComparator;
			this.bComparator = bComparator;
		}

		@Override
		public int compare(Pair<A, B> one, Pair<A, B> two)
		{
			if(one == null && two == null)
				return 0;
			
			if(one == null)
				return -1;
			
			if(two == null)	
				return 1;
			
			int comparison = aComparator.compare(one.first(), two.first());
			if(comparison != 0)
				return comparison;
			
			return bComparator.compare(one.second(), two.second());
		}
	}
	
	public static class NaturalPairComparator<A extends Comparable<A>, B extends Comparable<B>> implements Comparator<Pair<A, B>>
	{

		@Override
		public int compare(Pair<A, B> one, Pair<A, B> two)
		{
			if(one == null && two == null)
				return 0;
			
			if(one == null)
				return -1;
			
			if(two == null)	
				return 1;
			
			// Compare by first
			int comparison;
			
			if(one.first == null && two.first == null)
				comparison = 0;
			else if(one.first == null)
				comparison = -1;
			else if(two.first == null)
				comparison = 1;
			else		
				comparison = one.first().compareTo(two.first());
			
			if(comparison != 0)
				return comparison;
			
			// Compare by second
			if(one.second == null && two.second == null)
				comparison = 0;
			else if(one.second == null)
				comparison = -1;
			else if(two.second == null)
				comparison = 1;
			else		
				comparison = one.second().compareTo(two.second());
			
			return comparison;
		}
	}
	
	/**
	 * Returns a list of containing the first elements in all the pairs in the
	 * list provided. The returned list is backed by the provided list.
	 *   
	 * @param in A list of pairs
	 * @return
	 */
	public static <A, B> List<A> first(List<Pair<A, B>> in)
	{
		return new LeftList<A, B>(in);
	}

	/**
	 * Returns a list of containing the second elements in all the pairs in the
	 * list provided. The returned list is backed by the provided list.
	 *   
	 * @param in A list of pairs
	 * @return
	 */
	public static <A, B> List<B> second(List<Pair<A, B>> in)
	{
		return new RightList<A, B>(in);		
	}
	
	private static class LeftList<A, B> extends AbstractList<A>
	{
		private List<Pair<A, B>> master;
		
		public LeftList(List<Pair<A, B>> master)
		{
			this.master = master;
		}

		@Override
		public A get(int i)
		{
			return master.get(i).first(); 
		}

		@Override
		public int size()
		{
			return master.size();
		}
	}
	
	private static class RightList<A, B> extends AbstractList<B>
	{
		private List<Pair<A, B>> master;
		
		public RightList(List<Pair<A, B>> master)
		{
			this.master = master;
		}

		@Override
		public B get(int i)
		{
			return master.get(i).second(); 
		}

		@Override
		public int size()
		{
			return master.size();
		}
	}
}