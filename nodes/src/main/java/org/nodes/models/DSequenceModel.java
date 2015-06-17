package org.nodes.models;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.util.Arrays.asList;
import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.logFactorial;
import static org.nodes.util.Functions.tic;
import static org.nodes.util.Functions.toString;
import static org.nodes.util.Functions.toc;
import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonPositiveDefiniteMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.Global;
import org.nodes.Node;
import org.nodes.util.Functions;
import org.nodes.util.Series;

/**
 * This model represents a uniform distribution over all (directed) graphs with 
 * the given degree sequence.
 * 
 * @author Peter
 *
 */
public class DSequenceModel<L> implements Model<L, DGraph<L>>
{
	private static final int MAX_SEARCH_DEPTH = (int)(16.0 * log2(10.0));
	private static final double STOP_COND_GRAD = 10E-4;
	private static final double STOP_COND_STEP = 10E-4;
	private static final int DEFAULT_MEMORY = 7;
	private static final double PHI = 1.61803398874989484820458683;
	
	private boolean check = false;
	/**
	 * r_i
	 */
	private List<Integer> inSequence;
	/**
	 * c_i
	 */
	private List<Integer> outSequence;
	private int n;
	
	private int memory = DEFAULT_MEMORY; 
	
	// * (estimated) logprob for graphs with this sequence 
	private double logProb, bitsUpperBound, bitsLowerBound; 

	public DSequenceModel(DGraph<?> data, boolean check)
	{
		inSequence = new ArrayList<Integer>(data.size());
		outSequence = new ArrayList<Integer>(data.size());
		
		for(DNode<?> node : data.nodes())
		{
			inSequence.add(node.inDegree());
			outSequence.add(node.outDegree());
		}
			
		n = inSequence.size();
		
		this.check = check;
		
		computeNewton();
	}
	
	/**
	 * 
	 * @param inSequence
	 * @param outSequence
	 * @param check Whether to check if the provided graph has the correct 
	 * degree sequence.
	 */
	public DSequenceModel(
			List<Integer> inSequence, List<Integer> outSequence, 
			boolean check)
	{
		this.inSequence = new ArrayList<Integer>(inSequence);
		this.outSequence = new ArrayList<Integer>(outSequence);
		n = inSequence.size();
		
		this.check = check;
		
		computeNewton();
	}
	
	private double logCorrection()
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

	private void computeNewton()
	{
		
		LinkedList<RealVector> gradientSteps = new LinkedList<RealVector>();
		LinkedList<RealVector> xSteps     = new LinkedList<RealVector>();
		LinkedList<Double>     rhos       = new LinkedList<Double>();

		
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
			// System.out.println("x " + x);
			// System.out.println("direction: " + direction);
			// System.out.println("gradient at x :" + gradient(x));
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

	/**
	 * Gives the matrix a little push so that it's guaranteed to be positive 
	 * semidefinite
	 * 
	 * @param matrix
	 * @return
	 */
	private RealMatrix nudge(RealMatrix matrix)
	{	
		System.out.println(Functions.toString(matrix, 2));
		
		EigenDecomposition decomp = new EigenDecomposition(matrix, -1.0);
		RealMatrix v = decomp.getV();
		RealMatrix d = decomp.getD();
		RealMatrix vt = decomp.getVT();
				
		System.out.println(Functions.toString(d));

		for(int i : series(2*n))
			d.setEntry(i, i, Math.abs(decomp.getRealEigenvalue(i)));
		
		System.out.println(Functions.toString(d));
		
		matrix = v.multiply(d.multiply(v.transpose()));
		
		// Make symmetric, fix rounding errors
		for(int i : series(matrix.getColumnDimension()))
			for(int j : series(i, matrix.getRowDimension()))
				matrix.setEntry(i, j, matrix.getEntry(j, i));
		
		System.out.println(Functions.toString(matrix, 2));
		
		return matrix;	
	}
	
	public int getDim()
	{
		return n * 2;
	}

	public RealVector gradient(RealVector x)
	{
		RealVector gradient = new ArrayRealVector(2 * n); 

		for(int i : series(n))
		{
			double sum = 0.0;
			for(int j : series(n))
			{
				double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
				sum += part / (1 + part);
			}
			gradient.setEntry(s(i), sum - inSequence.get(i));
		}
		
		for(int j : series(n))
		{
			double sum = 0.0;
			for(int i : series(n))
			{
				double part = exp(x.getEntry(t(j)) + x.getEntry(s(i)));
				sum += part / (1 + part);
			}
			gradient.setEntry(t(j), sum - outSequence.get(j) );
		}
		
		return gradient;
	}
		
	private int t(int j)
	{
		return j + n;
	}
	
	private int s(int i)
	{
		return i;
	}

	public RealMatrix hessian(RealVector x)
	{
		// Make a 2n x 2n empty matrix.
		RealMatrix hessian = new Array2DRowRealMatrix(2 * n, 2 * n);

		// * Compute the diagonal
		for (int i : series(n))
		{
			double sum = 0.0;
			for (int j : series(n))
			{
				double part = exp(x.getEntry(s(i)) + x.getEntry(t(j)));
				sum += part / ((1+ part) * (1+part));
			}
			
			hessian.setEntry(s(i), s(i), sum);
		}
		
		for (int j : series(n))
		{
			double sum = 0.0;
			for (int i : series(n))
			{
				double part = exp(x.getEntry(s(i)) + x.getEntry(t(j)));
				sum += part / ((1 + part) * (1 + part));
			}
			
			hessian.setEntry(t(j), t(j), sum);
		}
		
		// * Compute lower left (s vs t) and upper right.
		//   The Hessian is symmetric, so we can do this in one go.
		for(int i : series(n))
			for(int v : series(n))
			{
				double part = exp(x.getEntry(s(i)) + x.getEntry(t(v)));
				double val = part / ((1 + part) * (1 + part));
				
				hessian.setEntry(s(i), t(v), val);
				hessian.setEntry(t(v), s(i), val);
			}
		
		// * upper left and lower right are zero, so the default values 
		//   don't need to be changed.
		
		return hessian;
	}

	public double value(RealVector x)
	{
		double value = 0.0;
		double[] xa = ((ArrayRealVector)x).getDataRef();// nasty trick.
		int nn = 2 * n;
		for(int i = 0; i < n; i++)
			for(int j = n; j < nn; j++)
				value += Math.log1p( exp(xa[i] + xa[j]));
		
		for(int i = 0; i < n; i++)
			value -= x.getEntry(s(i)) * inSequence.get(i);
		
		for(int j= 0; j < n; j++)
			value -= x.getEntry(t(j)) * outSequence.get(j);
		
		return value;
	}
	
	/**
	 *  Compute the value of the objective function at x
	 *  
	 */
	private double objective(List<Double> x)
	{
		List<Double> s = x.subList(0, n);
		List<Double> t = x.subList(n, 2 * n);
		
		double v = 0.0; 
		for(int i : series(n))
			for(int j : series(n))
				v += log(1.0 + exp(s.get(i)+t.get(j)));
				
		for(int i : series(n))
			v -= s.get(i) * inSequence.get(i);
		
		for(int j : series(n))
			v -= t.get(j) * outSequence.get(j);
		
		return v / log(2.0);
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
	
	private double directionalDerivative(RealVector x, RealVector direction)
	{
		return gradient(x).dotProduct(direction);
	}
	
	private List<Double> gradient(List<Double> x)
	{
		List<Double> s = x.subList(0, n);
		List<Double> t = x.subList(n, 2 * n);
		
		List<Double> gradientS = new ArrayList<Double>(n);
		List<Double> gradientT = new ArrayList<Double>(n);
		for(int i : Series.series(n))
			gradientS.add(0.0);
		for(int i : Series.series(n))
			gradientT.add(0.0);
		
		// * compute the gradient
		// - first half
		for(int i : series(n))
		{
			double g = - inSequence.get(i);
			
			for(int j : series(n)) 
				g += 
					Math.exp(s.get(i) + t.get(j)) / 
					(1.0 + Math.exp(s.get(i) + t.get(j)));
			
			gradientS.set(i, g);
		}
		
		// - second half
		for(int j : series(n))
		{
			double g = - outSequence.get(j);
			
			for(int i : series(n)) 
				g += 
					Math.exp(s.get(i) + t.get(j)) / 
					(1.0 + Math.exp(s.get(i) + t.get(j)));
			
			gradientT.set(j, g);
		}
		
		return Functions.concat(gradientS, gradientT);
	}
	
	@Override
	public double logProb(DGraph<L> graph)
	{
		if(check)
		{
			// check if the graph has the correct in and out sequence and return 
			// -infty if not. 
			// TODO
		}
		
		return logProb;
	}
	
	public double bitsUpperBound()
	{
		return bitsUpperBound;
	}
	
	public double bitsLowerBound()
	{
		return bitsLowerBound;
	}
	
	/**
	 * Returns the log_2 probability that is assigned to all graphs with the 
	 * correct degree sequences.
	 *   
	 * @return
	 */
	public double logProb()
	{
		return logProb;
	}
	
	private static int SA_MAX_ITS = 1000000;
	private static double SA_BETA = 2.0;
	
	public double computeWithSimulatedAnnealing()
	{
		List<Double> 
				x = new ArrayList<Double>(2 * n), 
				xNew = new ArrayList<Double>(2 * n);
		
		// * Initial position
		for(int i : series(2 * n))
		{
			x.add(1.0);
			xNew.add(1.0);
		}
		
		double vMin = Double.POSITIVE_INFINITY; 
		
		double vX = objective(x), vXNew;
		
		for(int i : series(SA_MAX_ITS))
		{
			if(i % (50000) == 0) System.out.print('.');
			
			double temp = i / (double) SA_MAX_ITS;
			
			// * generate a random step
			double maxStep = Math.pow((1.0 - temp), SA_BETA);
			for(int j : series(x.size()))
				xNew.set(j, x.get(j) + (Global.random().nextDouble() - 0.5 ) * maxStep);
			vXNew = objective(xNew);
			
			if(vXNew < vMin)
				vMin = vXNew;
			
			if(vXNew < vX  || exp((vX - vXNew)/temp) > Global.random().nextDouble())
			{
				List<Double> t = x;
				
				x = xNew;
				vX = vXNew;
				
				xNew = t;
				vXNew = Double.NaN;
			}
		}
		
		System.out.println();
		
		return vMin;
	}
}
