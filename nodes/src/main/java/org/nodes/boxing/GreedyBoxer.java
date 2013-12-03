package org.nodes.boxing;

import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.algorithms.FloydWarshall;
import org.nodes.util.Pair;
import org.nodes.util.Series;
import org.nodes.util.Distance;

public class GreedyBoxer<L> implements BoxingAlgorithm<L>
{
	private int[][] colors;
	private int lMax; // The maximal (horizontal) index to the colors array
	private short[][] distanceCache = null;
	private FloydWarshall<L> fw;
	
	private Graph<L> graph;
	
	public GreedyBoxer(Graph<L> graph, int lm)
	{
		super();
		this.graph = graph;
		fw = new FloydWarshall<L>(graph);
		
		setMatrix(lm-1);
	}	


	@Override
	public Boxing<L> box(int l)
	{
		int lIndex = l-1;
		if(lIndex > lMax)
			setMatrix(lIndex);
		
		Map<Integer, Set<Node<L>>> boxes = new HashMap<Integer, Set<Node<L>>>();
		for(int i : series(graph.size()))
		{
			Node<L> node = graph.nodes().get(i);
			int color = colors[i][lIndex];
			
			if(! boxes.containsKey(color))
				boxes.put(color, new HashSet<Node<L>>());
			
			boxes.get(color).add(node);
		}
		
		return new Boxing<L>(
				new ArrayList<Set<Node<L>>>(boxes.values()), 
				graph);
		
	}
	
	private int distance(int i, int j)
	{
		if(i == j) 
			return 0;		
		
		if(distanceCache != null)
		{
			int max = Math.max(i,  j), min = Math.max(i,  j);
			return distanceCache[max][min];
		}
		
		Number dNum = fw.distance(i, j);
		int distance = (dNum != null) ? (int)dNum.doubleValue() : Integer.MAX_VALUE;
		
		return distance;
	}

	private void setMatrix(int lMax)
	{
		// * NB the l index represent the distance - 1. So the colors at 
		//   colors[.][0] are those for box size 1
		
		this.lMax = lMax;
		int n = graph.size();
		
		colors = new int[n][];
		
		// * Set the color of node 0 to 0 for all l's
		colors[0] = new int[lMax+1];
		for (int l : series(lMax))
			colors[0][l] = 0;
		
		List<Integer> nColors = new ArrayList<Integer>();
		List<Pair<Integer, Integer>> neighbours = new ArrayList<Pair<Integer,Integer>>(n);
		
		for(int i : series(1, n))
		{
			if(i%1000 == 0)
				Global.log().info("At node "+i);
			colors[i] = new int[lMax+1];

			// * this node's neighbours in the dual graph (nodes that are farther than l away)
			//   we will shrink this as we increment l
			neighbours.clear();
			for(int j : series(i))
				neighbours.add(new Pair<Integer, Integer>(j, distance(i, j)));
			
			for(int l : series(lMax+1))
			{
				Iterator<Pair<Integer, Integer>> it = neighbours.iterator();
				while(it.hasNext())
					if(it.next().second() < l + 1)
						it.remove();
								
				nColors.clear();
				for(Pair<Integer, Integer> pair : neighbours)
					nColors.add(colors[pair.first()][l]);
				
				colors[i][l] = smallestException(nColors);
			}
		}

	}
	
	/**
	 * Returns the smallest positive integer not contained in a given list of
	 * illegal integers.
	 * 
	 * This method modifies the list it is passed.
	 * 
	 * @param illegal
	 * @return
	 */
	public static int smallestException(List<Integer> illegal)
	{
		Collections.sort(illegal);
		int res = 0;
		for(int i : illegal)
			if(i > res)
				return res;
			else
				res = i+1;
		return res;
	}
}
