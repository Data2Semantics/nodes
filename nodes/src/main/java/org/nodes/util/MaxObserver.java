package org.nodes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

import org.nodes.Global;

/**
 * Maintains a list of the top k largest elements seen.
 * 
 * @author Peter
 *
 */
public class MaxObserver<T>
{
	private int k;
	private T smallest = null; 
	private List<T> elements;
	
	private Comparator<T> comp, wrap;
	
	public MaxObserver(int k)
	{
		this(k, null);
	}
	
	public MaxObserver(int k, Comparator<T> comp)
	{
		this.k = k;
		this.comp = comp;
		wrap = new Wrap();
		
		elements = new ArrayList<T>(k + 1);
	}
	
	public void observe(Collection<? extends T> elements)
	{
		for(T element : elements)
			observe(element);
	}
	
	public void observe(T element)
	{
		if(k == 0)
			return;
		
		// * The buffer isn't full yet 
		if(elements.size() < k)
		{
			elements.add(element);
			smallest = min(smallest, element);
			
			return;
		}	
		
		// * The buffer is full, and the element is below the smallest in the 
		//   buffer or equal to it 
		if(ordered(element, smallest))
			return;
		
		// * the buffer is full and we're seeing an element that should be in it
		elements.add(element);
		elements.remove(smallest);
		
		assert(elements.size() == k);
		
		// ** Set the smallest element of the buffer
		smallest = null;
		for(T elem : elements)
			smallest = min(smallest, elem);
	}
	
	/**
	 * Returns the list of largest elements encountered so far, ordered with 
	 * the largest first.
	 * 
	 * @return
	 */
	public List<T> elements()
	{
		return elements(true);
	}
	
	public List<T> elements(boolean sorted)
	{
		if(sorted)
			Collections.sort(elements, Collections.reverseOrder(wrap));
		return Collections.unmodifiableList(elements);
	}
	
	private class Wrap implements Comparator<T>
	{
		@SuppressWarnings("unchecked")
		@Override
		public int compare(T first, T second)
		{
			if(comp != null)
				return comp.compare(first, second);
			
			if(! (first instanceof Comparable && second instanceof Comparable))
				throw new IllegalStateException("MaxObserver created without comparator can only analyze elements that are Comparable.");
			
			Comparable<T> cFirst = (Comparable<T>) first, cSecond = (Comparable<T>) second;
			return cFirst.compareTo((T)cSecond);		
		}
		
	}
	
	private T min(T first, T second)
	{
		if(first == null) return second;
		if(second == null) return first;
		
		if(wrap.compare(first, second) <= 0)
			return first;
		return second;
	}
	
	private T max(T first, T second)
	{
		if(first == null) return second;
		if(second == null) return first;

		if(wrap.compare(first, second) <= 0)
			return second;
		return first;
	}
	
	/**
	 * Returns true if the arguments are in non-decreasing order.
	 * 
	 * @param first
	 * @return
	 */
	private boolean ordered(T first, T second)
	{
		if(first == null || second == null) return false;
		
		return (wrap.compare(first, second) <= 0);
	}
	
	/**
	 * Returns 
	 * 
	 * @param k
	 * @param in
	 * @param comp
	 * @param copy If true, the original list is copied. If false, the original list is used,
	 *   and the order will be changed.
	 * @return
	 */
	public static <T> List<T> quickSelect(int k, List<T> in, Comparator<T> comp, boolean copy)
	{
		if(copy)
			in = new ArrayList<T>(in);
		
		select(k, in, 0, in.size(), comp);
		
		return in.subList(0, k);
	}
	
	protected static <T> int select(int k, List<T> list, int from, int to, Comparator<T> comp)
	{
		if(from == to -1)
			return from;
		
		int pivotIndex = Global.random().nextInt(to - from) + from;
		pivotIndex = partition(list, from, to, pivotIndex, comp);
		
		if(k == pivotIndex)
			return pivotIndex;
		else if(k < pivotIndex)
			return select(k, list, from, pivotIndex, comp);
		else 
			return select(k, list, pivotIndex + 1, to, comp);
	}
	
	/**
	 * 
	 * @param list
	 * @param from Range start inclusive
	 * @param to Range end exclusive
	 * @param pivotIndex
	 */
	public static <T> int partition(List<T> list, int from, int to, int pivotIndex, Comparator<T> comp)
	{
		T pivotValue = list.get(pivotIndex);
		
		swap(list, pivotIndex, to - 1);
		int storeIndex = from;
		for(int i : Series.series(from, to - 1))
			if( comp.compare(list.get(i), pivotValue) < 0)
				swap(list, storeIndex ++, i);
		swap(list, to - 1, storeIndex);
		return storeIndex;
	}
	
	public static <T> void swap(List<T> list, int first, int second)
	{
		T temp = list.get(first);
		list.set(first, list.get(second));
		list.set(second, temp);
	}
}
