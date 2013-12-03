package org.nodes;

import java.util.Comparator;

public class DegreeComparator<N> implements Comparator<Node<N>>
{
	@Override
	public int compare(Node<N> o1, Node<N> o2)
	{
		int d1 = o1.degree(), d2 = o2.degree();
		
		return Double.compare(d1, d2);
	}
}
