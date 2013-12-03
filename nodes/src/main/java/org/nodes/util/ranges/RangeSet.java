package org.nodes.util.ranges;

import java.util.List;

/**
 * Decribes a class which hold a number of ranges. 
 * 
 * @author peter
 *
 */
public interface RangeSet
{
	/**
	 * @param value
	 * @return Whether the RangeSet has one or more ranges for the given value 
	 */
	public boolean hasRange(double value);	
	
	/**
	 * Returns a range which contains the given value. If multiple ranges 
	 * contain the value, the particulars of which to choose are not specified. 
	 * However, the same value should always result in the same range being 
	 * returned. 
	 * 
	 * @param value
	 * @return A Range which contains this value. null if no such Range is 
	 *         available. 
	 */
	public Range first(double value);
	
	/**
	 * Returns all the ranges which contains this value
	 * 
	 * @param value
	 * @return All Ranges which contains this value. An empty list if no such 
	 *         ranges are available. 
	 */
	public List<Range> all(double value);
	

}
