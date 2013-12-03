package org.nodes.draw;

/**
 * A single point in a euclidean space (ie. a vector of double values).
 * 
 */

import java.io.Serializable;
import java.util.*;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.nodes.Global;

public class Point 
	extends AbstractList<Double>
	implements Serializable //, Metrizable<Point>
{
	private static final long serialVersionUID = 3199154235479870697L;
	
	private double[] values; 

	private Point()
	{
	}
	
	/**
	 * Creates a Point of the given dimensionality, with all zero values. 
	 * 
	 * @param dimensionality
	 */
	public Point(int dimensionality)
	{
		this.values = new double[dimensionality];
	}
	
	/**
	 * Creates a point directly from values specified in the parameters.
	 * 
	 * The point will be not be backed by the input array. 
	 */
	public Point(double... values) 
	{
		this.values = Arrays.copyOf(values, values.length);
	}
	
	/**
	 * Creates a point directly from values specified in the parameters.
	 * 
	 * The point will be not be backed by the input list. 
	 */
	public Point(List<Double> params) 
	{
		this.values = new double[params.size()];
		for(int i = 0; i < values.length; i++)
			this.values[i] = params.get(i);
	}	
	
	/**
	 * @param values
	 */
	public Point(RealVector values)
	{
		this.values = values.toArray();
	}

	public int dimensionality()
	{
		return values.length;
	}
	
	public Double set(int index, Double value)
	{
		double old = values[index];
		values[index] = value;
		
		return old;
	}
	
	@Override
	public int size()
	{
		return values.length;
	}

	@Override
	public Double get(int index)
	{
		return values[index];
	}

	// @Override
	public double distance(Point other)
	{
		return distance(this, other);
	}
	
	/**
	 * Returns this Point represented as a one dimensional matrix.
	 */
	public RealVector getVector()
	{
		return new ArrayRealVector(values);
	}
	
	/**
	 * Returns this Point represented as an array of double values.
	 * 
	 * The point is backed by this array, so this method should be used with 
	 * extreme care, only in situation where optimization is important  
	 */
	public double[] getBackingData()
	{
		return values;
	}	
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		boolean first = true;
		
		for(int i = 0; i < size(); i++)
		{
			if(first) first = false;
			else sb.append(", ");
			
			sb.append(get(i));
		}
		sb.append("]");		
		
		return sb.toString();
	}

	/**
	 * Defines the euclidean distance between two points.
	 * 
	 * If the dimensionality of one of the points is smaller than the other,
	 * its values for the remaining dimensions are assumed to be zero.
	 */
	public static double distance(Point a, Point b)
	{
		Point mi, ma;
		if(a.values.length < b.values.length)
		{
			mi = a;
			ma = b;
		} else
		{
			mi = b;
			ma = a;
		}
					
		double distSq = 0.0, d;
		
		int i;
		for(i = 0; i < mi.size(); i++)
		{
			d = mi.values[i] - ma.values[i];
			distSq += d * d;
		}
		
		for(; i < ma.size(); i++)
		{
			d = ma.values[i];
			distSq += d * d;
		}
		
		return Math.sqrt(distSq);
	}

	public static Point random(int dim, double var)
	{
		Point p = new Point(dim);
		for(int i = 0; i < dim; i++)
			p.set(i, Global.random().nextGaussian() * var);
		
		return p;
	}
	
	/**
	 * Produces a point with the given value for all dimensions
	 * @param dim
	 * @param val
	 * @return
	 */
	public static Point value(int dim, double val)
	{
		Point p = new Point(dim);
		for(int i = 0; i < dim; i++)
			p.set(i, val);
		
		return p;
	}	
	
	public static Point point(List<Double> in)
	{
		if(in instanceof Point)
			return (Point)in;
		
		return new Point(in);
	}
	
	/**
	 * Creates a point with the given data as backing data. This will reduces
	 * memory use/increase speed in many cases, but at the risk of introducing 
	 * nasty bugs. Use with caution. 
	 * 
	 * @param data
	 * @return
	 */
	public static Point fromRaw(double[] data)
	{
		Point p = new Point();
		p.values = data;
		
		return p;
	}
}
