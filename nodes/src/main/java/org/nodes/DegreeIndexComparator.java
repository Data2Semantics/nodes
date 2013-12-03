package org.nodes;

import java.util.Comparator;

public class DegreeIndexComparator implements Comparator<Integer>
{
	Graph<?> graph;
	
	public DegreeIndexComparator(Graph<?> graph)
	{
		this.graph = graph;
	}
	
	@Override
	public int compare(Integer o1, Integer o2)
	{
		int d1 = graph.nodes().get(o1).degree(), d2 = graph.nodes().get(o2).degree();
		
		return Double.compare(d1, d2);
	}
}
