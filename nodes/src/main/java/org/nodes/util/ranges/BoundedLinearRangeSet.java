package org.nodes.util.ranges;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BoundedLinearRangeSet implements Serializable, RangeSet
{
	private double from,
	               to,
	               step;
	private boolean includeEdges;
	
	private List<Range> empty = Collections.emptyList();

	public BoundedLinearRangeSet(double from, double step, double to)
	{
		this(from, step, to, false);
	}	
	
	public BoundedLinearRangeSet(
			double from, double step, double to, boolean includeEdges)
	{
		this.from = from;
		this.to = to;
		this.step = step;
		
		this.includeEdges = includeEdges;
	}
	
	@Override
	public boolean hasRange(double value)
	{
		if(Double.isInfinite(value) || Double.isNaN(value))
			return false;		
		if(value < from || value >= to)
				return includeEdges;
		
		return true;
	}	

	@Override
	public Range first(double value)
	{
		if(value >= to)
			return includeEdges ? new Range(to, Double.POSITIVE_INFINITY) : null;
		if(value < from)
			return includeEdges ? new Range(Double.NEGATIVE_INFINITY, from) : null;
		
		// * The index of the bin, counting from 0 as the bin that starts from 
		//   the origin
		double index = Math.floor( (value - from) / step );
	
		double fromRange = from + index * step,
		       toRange   = from + (index + 1.0) * step;
		
		return new Range(fromRange, toRange);		
	}

	@Override
	public List<Range> all(double value)
	{
		return hasRange(value) ? empty : Arrays.asList(first(value));
	}

}
