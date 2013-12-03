package org.nodes.util.ranges;

import java.util.Comparator;


public class RangeComparator implements Comparator<Range>
{

	@Override
	public int compare(Range r1, Range r2)
	{
		if(r1.from() != r2.from())
			return Double.compare(r1.from(), r2.from());
		
		return Double.compare(r1.to(), r2.to());
	}
}
