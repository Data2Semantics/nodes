package org.nodes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Maintains an ordered list of the top k largest elements seen.
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
	
}
