package org.nodes.clustering;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.nodes.classification.Classification;
import org.nodes.classification.Classified;
import org.nodes.draw.Point;
import org.nodes.util.Distance;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class KMedioids<P>
{
	private Distance<P> distance;
	private Classified<P> data;
	private int numClusters;
	
	// * Indices of the medioids
	private List<Integer> medioids;
	
	// * A cache for the distances
	private RealMatrix distances;
	
	public KMedioids(List<P> data, Distance<P> distance, int numClusters)
	{
		int n = data.size();
		
		this.distance = distance;
		this.numClusters = numClusters;
		
		medioids = Functions.sampleInts(numClusters, n);
		
		Global.log().info("Calculating distances");
		
		int total = (n * n + n) / 2;
		int t = 0;
				
		distances = new Array2DRowRealMatrix(n, n);
		for(int i : series(n))
			for(int j : series(i, n))
			{
				distances.setEntry(i, j, 
						distance.distance(data.get(i), data.get(j)));
				// Global.log().info("Calculating distance " + t + " out of " + total);
				t++;
			}
		
		// System.out.println(distances);
		
		List<Integer> classes = new ArrayList<Integer>(n);
		for(int i : series(n))
			classes.add(-1);
		
		this.data = Classification.combine(data, classes);
	}
	
	/**
	 * Assign each point to its closest medioid.
	 */
	public void assign()
	{
		for(int i : series(data.size()))
		{
			int bestCluster = -1;
			double bestDistance = Double.POSITIVE_INFINITY; 
			
			for(int cluster : series(numClusters))
			{
				double distance = distance(i, medioids.get(cluster));
				
				if(distance < bestDistance)
				{
					bestDistance = distance;
					bestCluster = cluster;
				}
			}
			
			data.setClass(i, bestCluster);
		}		
		
		System.out.println(data.classes());
	}
	
	public Classified<P> clustered()
	{
		return data;
	}
	
	/**
	 * Choose the optimal medioids.
	 */
	public void update()
	{
		for(int cluster : series(numClusters))
		{
			int bestMedioid = -1;
			double bestMedioidScore = Double.POSITIVE_INFINITY;
			
			for(int medioid : series(data.size()))
			{
				double score = 0.0;
				for(int other : series(data.size()))
					if(data.cls(other) == cluster)
					{
						double d = distance(medioid, other);
						score += d * d;
					}
				
				if(score < bestMedioidScore)
				{
					bestMedioidScore = score;
					bestMedioid = medioid;
				}
			}
			
			medioids.set(cluster, bestMedioid);
		}
	}
	
	public void iterate(int n)
	{
		for(int i : series(n))
		{
			assign();
			update();
		}
	}
	
	private double distance(int i, int j)
	{
		int small, big;
		if(i < j)
		{
			small = i; 
			big = j;
		} else
		{
			small = j; 
			big = i;
		}
		
		return distances.getEntry(small, big);
	}


}
