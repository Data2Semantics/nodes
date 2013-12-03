package org.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Measures
{

	/**
	 * <p>
	 * A measure for the extent to which the degree of a node indicate the 
	 * degrees of its neighbours. Basically the correlation between the degrees
	 * of both sides of a random edge.
	 * </p><p>
	 * Implementation as described in "Networks" by Mark Newman (p267) 
	 * </p>
	 * @param graph
	 * @return
	 */
	public static <N> double assortativity(Graph<N> graph)
	{
		double mi;
		double a = 0.0, b = 0.0, c = 0.0;
		
		mi = 1.0 / graph.numLinks();
		
		for(Link<N> link: graph.links())
		{
			Node<N> nj = link.first();
			Node<N> nk = link.second();
			
			int j = nj.degree(), 
			    k = nk.degree();
			
			a += j * k;
			b += 0.5 * (j + k);
			c += 0.5 * (j * j + k * k);
		}
		
		return (mi*a - (mi*mi * b*b)) / (mi*c - (mi*mi * b*b));
	}
	
	public static <L> double clusteringCoefficient(Graph<L> graph)
	{
		long numPaths = 0, numClosed = 0;
		
		for(Node<L> one : graph.nodes())
			for(Node<L> two : one.neighbors())
				for(Node<L> three : two.neighbors())
					if(!three.equals(one))
					{
						numPaths ++;
						if(one.connected(three))
							numClosed++;
					}
	
		return numClosed / (double) numPaths;
	}	
}
