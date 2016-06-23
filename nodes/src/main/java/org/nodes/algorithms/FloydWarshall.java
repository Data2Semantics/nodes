package org.nodes.algorithms;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.util.Distance;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

/**
 * TODO: Check graph modcount
 * @author Peter
 *
 * @param <L>
 */
public class FloydWarshall<L> implements Distance<Node<L>>
{
	private short[][] distances;
	private int n;
	private Graph<L> graph;

	public FloydWarshall(Graph<L> graph)
	{
		this.graph = graph;
		n = graph.size();
		
		distances = new short[n][];
		for(int i : series(n))
			distances[i] = new short[n];
		
		Global.log().info("FW Starting");
		
		// * Init the matrix
		for(int i: series(n))
			for(int j : series(n))
				if(i == j)
					distances[i][j] = 0;
				else
					distances[i][j] = 
					graph.nodes().get(i).connected(graph.nodes().get(j)) 
					? 1 : Short.MAX_VALUE;
			
		for(int k : series(n))
			for(int i: series(n))
				for(int j : series(n))
					distances[i][j] = (short)Math.min(distances[i][j], distances[i][k]+distances[k][j]);
		
		Global.log().info("FW finished");
	}
	
	public int distance(int i, int j)
	{
		return distances[i][j];
	}
	
	public double meanDistance()
	{
		double meanDistance = 0.0;
		int num = 0;
		for(int i : series(n))
			for(int j : series(n))
			{
				meanDistance += distance(i, j);
				num++;
			}
		
		meanDistance /= (double) num;
		return meanDistance;
	}
	
	public double diameter()
	{
		double diameter = Double.NEGATIVE_INFINITY;
		for(int i : series(n))
			for(int j : series(n))
				diameter = Math.max(diameter, distance(i, j));

		return diameter;
	}

	@Override
	public double distance(Node<L> a, Node<L> b)
	{
		int i = a.index();
		int j = b.index();
		
		
		int d = distance(i, j);
		
		if(d >= Short.MAX_VALUE)
			return Double.POSITIVE_INFINITY;
		return d;
	}
	
	/**
	 * Returns the distance matrix
	 *  
	 * @return
	 */
	public RealMatrix matrix()
	{
		RealMatrix mat = new Array2DRowRealMatrix(n, n);
		for(int i : series(n))
			for(int j : series(n))
				mat.setEntry(i, j, distances[i][j]);
	
		return mat;
	}
}
