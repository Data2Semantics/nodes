package org.nodes.models.old;

import static java.lang.Math.log;
import static nl.peterbloem.kit.Functions.log2;
import static nl.peterbloem.kit.Functions.logFactorial;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import nl.peterbloem.kit.Global;

/**
 * Abstract implementation of the L-BFGS search for the Barvinok bounds.
 * 
 * Note that extending classes should call the search method in the constructor. 
 * @author Peter
 *
 */
public abstract class AbstractBarvinok
{
	protected static final int MAX_SEARCH_DEPTH = (int)(16.0 * log2(10.0));
	protected static final double STOP_COND_GRAD = 10E-4;
	protected static final double STOP_COND_STEP = 10E-4;
	protected static final int DEFAULT_MEMORY = 7;
	protected static final double PHI = 1.61803398874989484820458683;

	protected int memory = DEFAULT_MEMORY;
	protected int n;
	
	protected double bitsUpperBound, bitsLowerBound;
	/**
	 * r_i
	 */
	protected List<Integer> inSequence;
	/**
	 * c_i
	 */
	protected List<Integer> outSequence;
	
	public AbstractBarvinok(List<Integer> inSequence, List<Integer> outSequence, int memory)
	{
		this.inSequence = inSequence;
		this.outSequence = outSequence;
		n = inSequence.size();
		this.memory = memory;
	}
	
	public void search()
	{
		LinkedList<RealVector> gradientSteps = new LinkedList<RealVector>();
		LinkedList<RealVector> xSteps        = new LinkedList<RealVector>();
		LinkedList<Double>     rhos          = new LinkedList<Double>();

		RealVector x = new ArrayRealVector(2 * n);
		
		for(int i : series(x.getDimension()))
			x.setEntry(i, Global.random().nextDouble());
		
		int i = 0;
		
		RealVector gradient = gradient(x);

		while(true)
		{
			i++;
			
			RealVector direction = hessianMultiply(gradientSteps, xSteps, rhos, gradient);
			direction.mapMultiplyToSelf(-1.0);
			
			double rate = findStepSize(x, direction);
			
			RealVector oldX = x;
			x = x.add(direction.mapMultiply(rate));
			
			RealVector oldGradient = gradient;
			gradient = gradient(x);
			
			RealVector xStep = x.subtract(oldX),
					gradientStep = gradient.subtract(oldGradient);
			
			xSteps.add(xStep);
			gradientSteps.add(gradientStep);
			rhos.add(1.0/xStep.dotProduct(gradientStep));
			
			System.out.println(i + " diff " + xSteps.get(xSteps.size()-1).getNorm());
			System.out.println("Gradient norm " + gradient.getLInfNorm());
			
			double v = value(x)/log(2.0); 
			System.out.println("[" + (v + logCorrection()) + ", "+ v +"]");
			
			if(gradient.getLInfNorm() < STOP_COND_GRAD && xSteps.get(xSteps.size()-1).getNorm() < STOP_COND_STEP)
				break;
			
			while(gradientSteps.size() > memory)
			{
				gradientSteps.remove(0);
				xSteps.remove(0);
				rhos.remove(0);
			}
		}
				
		bitsUpperBound = value(x) / log(2.0);
		bitsLowerBound = bitsUpperBound + logCorrection();
		
		
	}
	
	protected double logCorrection()
	{
		double value = logFactorial(n*n, 2.0);
		value -= 2 * n * n * log2(n);
		for(int i : series(n))
			value += (n - inSequence.get(i)) * log2((n - inSequence.get(i))); 
		
		for(int i : series(n))
			value -= logFactorial(n - inSequence.get(i), 2.0);
		
		for(int j : series(n))
			value += outSequence.get(j) == 0  
					? 0
					: outSequence.get(j) * log2(outSequence.get(j));
		
		for(int j : series(n))
			value -= logFactorial(outSequence.get(j), 2.0);
		
		return value;
	}
	
	/**
	 * Computes an approximation to the inverse Hessian multiplied by the 
	 * direction
	 *  
	 * @param gradients
	 * @param steps
	 * @param direction
	 * @return
	 */
	private RealVector hessianMultiply(
			List<RealVector> y, List<RealVector> s,
			List<Double> rhos,
			RealVector direction)
	{
		RealVector r = new ArrayRealVector(direction);
		
		assert(y.size() == s.size());
		
		int n = y.size();
		
		double[] alpha = new double[n];
		
		for(int i : series(n - 1, -1))
		{
			alpha[i] = rhos.get(i) * s.get(i).dotProduct(r);
			
			r = r.subtract(y.get(i).mapMultiply(alpha[i]));
		}
		
		if(n > 0)
		{
			double gamma = s.get(n-1).dotProduct(y.get(n-1)) / y.get(n-1).dotProduct(y.get(n-1));
			r.mapMultiplyToSelf(gamma);
		}
		
		for(int i : series(n))
		{
			double beta = rhos.get(i) * y.get(i).dotProduct(r);
			r = r.add(s.get(i).mapMultiply(alpha[n - 1 - i] - beta));
		}
		
		return r;
	}
	
	private double findStepSize(RealVector x, RealVector direction)
	{
		// * make unit steps to find a value where the deriv is negative
		double gamma = 1.0;
		
		double vg = value(x.add(direction.mapMultiply(gamma)));
		double vx = value(x);
		
		while(vg < vx)
		{
			gamma *= 2.0;
			vg = value(x.add(direction.mapMultiply(gamma)));
			System.out.print("x");
		}		
		System.out.println();

		// * Golden section search
		double a = 0.0, b = gamma / PHI, c = gamma;
		double va = vx,
		        vb = value(x.add(direction.mapMultiply(b))),
		        vc = value(x.add(direction.mapMultiply(c)));
		
		return findStepSize(x, direction, a, b, c, va, vb, vc, 0);
	}
	
	/**
	 * We use golden section search to find the minimum along the search 
	 * direction. 
	 *  
	 * Note that binary search for the root of the derivative along the 
	 * direction is also an option, but it seems to be less stable once the 
	 * gradient gets shallow.
	 *  
	 * @param x
	 * @param gradient
	 * @param lower
	 * @param upper
	 * @return
	 */
	private double findStepSize(RealVector x, RealVector direction, 
			double lower, double mid, double upper, 
			double vlower, double vmid, double vupper, int depth)
	{
		if(depth >= MAX_SEARCH_DEPTH)
			return Global.random().nextDouble() * (upper - lower) + lower;
		
		// System.out.print('.');
		
		double a = lower, b, c, d = upper, va = vlower, vb, vc, vd = vupper;
		if(mid - lower >= upper - mid)
		{
			b = lower + (mid - lower) / PHI;
			vb = value(x.add(direction.mapMultiply(b)));
			
			c = mid;
			vc = vmid;
		} else
		{
			b = mid;
			vb = vmid;
			
			c = upper - (upper - mid) / PHI;
			vc = value(x.add(direction.mapMultiply(c)));
		}
		
		if(vb <= vc)
			return findStepSize(x, direction, a, b, c, va, vb, vc, depth + 1);
		else
			return findStepSize(x, direction, b, c, d, vb, vc, vd, depth + 1);
	}
	
	/**
	 * The upper bound to the code length (in bits)
	 * @return
	 */
	public double upperBound()
	{
		return bitsUpperBound;
	}
	
	/**
	 * The lower bound to the code length (in bits)
	 * @return
	 */
	public double lowerBound()
	{
		return bitsLowerBound;
	}
	
	/**
	 * Computes the value of the objective function at x. Note that this is the
	 * bottleneck in the search process. This function should be highly optimized. 
	 * 
	 * @param x
	 * @return
	 */
	public abstract double value(RealVector x);
	
	public abstract RealVector gradient(RealVector x);
	
	protected int t(int j)
	{
		return j + n;
	}
	
	protected int s(int i)
	{
		return i;
	}
}
