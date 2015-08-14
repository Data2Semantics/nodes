package org.nodes.util;

import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.util.Arrays.asList;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nodes.util.LogNum.LogNumList;

import com.itextpdf.text.log.SysoLogger;

/**
 * A number stored as the logarithm of its magnitude and its sign.
 * 
 * @author Peter
 */
public class LogNum extends Number implements Comparable<LogNum>
{
	private static final long serialVersionUID = -3536380515455166108L;
	
	private double logMag;
	private boolean positive;
	private double base; // non-negative
	
	// maintain a copy of the original value, if one exists. 
	private Double original = null;
	
	public LogNum(double logMag, boolean positive, double base)
	{
		this.logMag = logMag;
		this.positive = positive;
		this.base = base;
	}
	
	private LogNum(double value, double base)
	{	
		logMag = Functions.log2(Math.abs(value));
		positive = (value >= 0.0);
		original = value;
		
		this.base = base;
	}
	
	public LogNum toBase(double newBase)
	{
		double newMag = logMag * Functions.log(base, newBase);
		return new LogNum(newMag, positive, newBase);
	}
	
	/**
	 * Swaps the sign
	 * @return
	 */
	public LogNum neg()
	{
		double newMag = logMag;
		return new LogNum(newMag, ! positive, base);
	}
	
	public LogNum times(LogNum other)
	{
		other = other.toBase(base);
		
		return new LogNum(logMag + other.logMag, positive == other.positive, base);
	}
	
	/**
	 * this / other
	 * @param other
	 * @return
	 */
	public LogNum divide(LogNum other)
	{
		other = other.toBase(base);
		
		return new LogNum(logMag - other.logMag, positive == other.positive, base);	
	}
	
	public LogNum plus(LogNum other)
	{
		return LogNum.sum(asList(this, other));
	}

	public LogNum minus(LogNum other)
	{
		other = other.neg();
		
		return LogNum.sum(asList(this, other));
	}
	
	public LogNum pow(int i)
	{
		boolean even = i % 2 == 0;
		
		return new LogNum(i * logMag, even ? true : positive, base);
	}

	public LogNum root(int i)
	{
		boolean even = i % 2 == 0;
		
		if(even && ! positive)
			throw new IllegalArgumentException("Cannot take an even root (i="+i+") of a negative number "+ this);
		
		return new LogNum((1.0/i) * logMag, positive, base);
	}
	
	public double logMag()
	{
		return logMag;
	}

	/**
	 * 0 is counted as postive.
	 * @return
	 */
	public boolean positive()
	{
		return positive;
	}
	
	public double base()
	{
		return base;
	}

	@Override
	public int intValue()
	{
		return (int)doubleValue();
	}

	@Override
	public long longValue()
	{
		return (long)doubleValue();
	}

	@Override
	public float floatValue()
	{
		return (float) doubleValue();
	}

	@Override
	public double doubleValue()
	{
		if(original != null)
			return original;
		
		return positive ? Functions.exp2(logMag) : -Functions.exp2(logMag);
	}

	@Override
	public int compareTo(LogNum other)
	{
		if(other.base != base)
			other = other.toBase(base);
		
		if(positive && other.positive)
			return Double.compare(logMag, other.logMag);
		if((!positive) && (!other.positive))
			return - Double.compare(logMag, other.logMag);
		if(positive)
			return 1;
		return -1;
	}
	
	
	/**
	 * Takes the base from the first element of the list. If the list is empty,
	 * 0.0 in base e is returned.
	 * 
	 * @param values
	 * @return
	 */
	public static LogNum sum(List<LogNum> values)
	{
		if(values.isEmpty())
			return LogNum.fromDouble(0.0, Math.E);
		double base = values.get(0).base();
		return sum(values, base);
	}
	/**
	 * Sums a list of lognums and returns the result.
	 * 
	 * @param base The base of the resulting LogNum
	 */
	public static LogNum sum(List<LogNum> values, double base)
	{
		// * Re-base everything
		List<LogNum> vals = new ArrayList<LogNum>(values.size());
		for(LogNum old : values)
			vals.add(old.toBase(base));
		values = null; // safety
		
		List<LogNum> pos = new ArrayList<LogNum>(vals.size());
		List<LogNum> neg = new ArrayList<LogNum>(vals.size());
		
		for(LogNum num : vals)
			if(num.positive())
				pos.add(num);
			else
				neg.add(num);
		
		double posMag = magSum(pos),
		       negMag = magSum(neg);
						
		if(posMag == negMag)
			return new LogNum(0.0, base);
		if(posMag > negMag)
			return new LogNum(Functions.logMin(base, posMag, negMag), true, base);
		
		return new LogNum(Functions.logMin(base, negMag, posMag), false, base);
	}

	/**
	 * The log magnitude of the sum of the supplied values, assuming that they
	 * all have the same sign and base.
	 * 
	 * @param values
	 * @param base
	 * @return
	 */
	private static double magSum(List<LogNum> values)
	{
		if(values.isEmpty())
			return Double.NEGATIVE_INFINITY; 
		
		double base = values.get(0).base();
		double max = Double.NEGATIVE_INFINITY;
		for(LogNum v : values)
			max = max(max, v.logMag());
		
		if(max == Double.NEGATIVE_INFINITY)
			return Double.NEGATIVE_INFINITY;
		
		double sum = 0.0;
		for(LogNum v : values)
			sum += Math.pow(base, v.logMag() - max);
		
		return Functions.log(sum, base) + max;
	}

	public static LogNum fromDouble(double value, double base)
	{
		return new LogNum(value, base);
	}
	
	public String toString()
	{
		return doubleValue() + "["+logMag+","+positive+"]";
	}
	
	/**
	 * Computes the skewness of the data, given in log-form
	 * 
	 * (opposite of a in the BCa model?)
	 * 
	 * @param data
	 */
	public static LogNum skewness(List<LogNum> data)
	{
		LogNum mean = mean(data);
		
		List<LogNum> diffs = new ArrayList<LogNum>(data.size());
		for(LogNum datum : data)
			diffs.add(datum.minus(mean));
		
		List<LogNum> diffsTo2 = new ArrayList<LogNum>(data.size());
		for(LogNum diff : diffs)
			diffsTo2.add(diff.pow(2));
		
		List<LogNum> diffsTo3 = new ArrayList<LogNum>(data.size());
		for(LogNum diff : diffs)
			diffsTo3.add(diff.pow(3));
		
		LogNum num = LogNum.sum(diffsTo3);
		num = num.divide(LogNum.fromDouble(data.size(), 2.0));
		
		LogNum den = LogNum.sum(diffsTo2);
		den = den.divide(LogNum.fromDouble(data.size() - 1, 2.0));
		den = den.root(2).pow(3);
		
		return num.divide(den);
	}
	
	public static LogNum mean(List<LogNum> data)
	{
		LogNum sum = LogNum.sum(data);
		double base = data.isEmpty() ? 2.0 : data.get(0).base();
		return sum.divide(LogNum.fromDouble(data.size(), base));
	}
	
	public static List<LogNum> list(List<Double> in, double base)
	{
		return new LogNumList(in, base);
	}

	public static class LogNumList extends AbstractList<LogNum>
	{
		private List<Double> master;
		private double base;
		
		public LogNumList(List<Double> master, double base)
		{
			this.master = master;
			this.base = base;
		}

		@Override
		public LogNum get(int index)
		{
			return fromDouble(master.get(index), base);
		}

		@Override
		public int size()
		{
			return master.size();
		}
	
	}
}