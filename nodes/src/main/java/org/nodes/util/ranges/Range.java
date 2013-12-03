package org.nodes.util.ranges;

import java.io.Serializable;
import java.util.List;

import org.nodes.util.Series;

/**
 * A range is a simple representation of a range of real numbers.
 * 
 * A range is defined by by an inclusive lower bound and an exclusive upper 
 * bound. Infinite values are allowed as bounds.
 * 
 * @author peter
 */
public class Range implements Serializable
{
	private static final long serialVersionUID = -5652394268089520509L;
	
	private double from;
	private double to;

	/**
	 * Defines a Range from 0.0 to a given value.
	 * 
	 * @param to The upper bound for the range. The upper bound itself is not 
	 *            included in the range.
	 */
	public Range(double to)
	{
		this(0.0, to);
	}

	/**
	 * Defines a Range from 0.0 to a given value.
	 *
	 * @param from The lower bound for the range. The lower bound itelf is 
	 *        included in the range. 
	 * @param to The upper bound for the range. The upper bound itself is not 
	 *        included in the range.
	 * @throws IllegalArgumentException if from > to       
	 */
	public Range(double from, double to)
	{
		if(from > to)
			throw new IllegalArgumentException("From ("+from+") must be smaller than to ("+to+")");
		if(Double.isNaN(from))
			throw new IllegalArgumentException("From is NaN. Must be a number.");
		if(Double.isNaN(to))
			throw new IllegalArgumentException("To is NaN. Must be a number.");
		
		this.from = from;
		this.to = to;
	}

	/**
	 * Checks whether a given number is included in this range.
     *
	 * @param number
	 * 
	 * @return
	 */
	public boolean contains(Number number)
	{
		double d = number.doubleValue();
		return (d >= from) && (d < to);
	}
	
	/**
	 * @return The lower bound of this range
	 */
	public double from()
	{
		return from;
	}
	
	/**
	 * @return The upper bound of this range
	 */
	public double to()
	{
		return to;
	}
	
	/**
	 * @return The size of this range (to - from)
	 */
	public double size()
	{
		return to - from;
	}
	
	public double center()
	{
		return from + (to - from) / 2.0;		
	}
	
	public List<Double> series()
	{
		return Series.series(from, to);
	}
	
	public List<Double> series(double step)
	{
		return Series.series(from, step, to);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(from);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(to);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Range other = (Range) obj;
		if (Double.doubleToLongBits(from) != Double
				.doubleToLongBits(other.from))
			return false;
		if (Double.doubleToLongBits(to) != Double.doubleToLongBits(other.to))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("[% .3f, % .3f]", from, to); 
	}
	
	public String toString(boolean full)
	{
		return full ? String.format("[% .16f|%d, % .16f|%d]", 
				from, Double.doubleToLongBits(from),
				to, Double.doubleToLongBits(to)) : toString(); 
	}	
	
}