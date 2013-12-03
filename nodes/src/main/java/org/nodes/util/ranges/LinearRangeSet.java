package org.nodes.util.ranges;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generates an unbounded RangeSet. This RangeSet cuts the range of real numbers
 * into an infinite number of non-overlapping ranges. For instance, for binSize
 * 10, the resuling bins would be: <br/>
 * <code>
 * ..., -20:-10, -10:0, 0:10, 10:20, 30:40, ...
 * </code> 
 * 
 * @author peter
 */
public class LinearRangeSet implements Serializable, RangeSet
{
	private static final long serialVersionUID = -8040289562901746333L;
	
	double binSize;
	double origin;

	private List<Range> empty = Collections.emptyList();

	public LinearRangeSet(double rangeSize)
	{
		this(rangeSize, 0.0);
	}
	
	public LinearRangeSet(double binSize, double origin)
	{
		this.binSize = binSize;
		this.origin = origin;
	}
	
	@Override
	public boolean hasRange(double value)
	{
		if(Double.isInfinite(value) || Double.isNaN(value))
			return false;
		
		return true;
	}

	@Override
	public Range first(double value)
	{
		if(Double.isInfinite(value) || Double.isNaN(value))
			return null;
		
		// * The index of the bin, counting from 0 as the bin that starts from 
		//   the origin
		double index = Math.floor( (value - origin) / binSize );
	
		double from = origin + index * binSize,
		       to   = origin + (index + 1.0) * binSize;
		
		return new Range(from, to);
	}

	@Override
	public List<Range> all(double value)
	{
		Range range = first(value);
		return range == null ? empty  : Arrays.asList(range);
	}
}
