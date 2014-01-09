package org.nodes.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapUTGraph;
import org.nodes.Node;

public class FloydWarshallTest
{

	@Test
	public void testDistanceIntInt()
	{
		Graph<String> graph = Graphs.k(10, "x");
		
		FloydWarshall<String> fw = new FloydWarshall<String>(graph);
		
		for(Node<String> a : graph.nodes())
			for(Node<String> b : graph.nodes())
				assertEquals(a.equals(b) ? 0 : 1, fw.distance(a.index(), b.index()));
		
	}
	
	@Test
	public void testDistanceLadder()
	{
		Graph<String> graph = new MapUTGraph<String, String>();
		
		Node<String> a = graph.add("a"),
		             b = graph.add("b"),
		             c = graph.add("c"),
		             d = graph.add("d"),
		             e = graph.add("e"),
		             f = graph.add("f");
		
		a.connect(b);
		a.connect(c);
		b.connect(d);
		c.connect(d);
		c.connect(e);
		d.connect(f);
		e.connect(f);
		 
		FloydWarshall<String> fw = new FloydWarshall<String>(graph);
		
		// * test for symmetry
		for(Node<String> x : graph.nodes())
			for(Node<String> y : graph.nodes())
				assertEquals(fw.distance(x.index(), y.index()), fw.distance(y.index(), x.index()));
		
		// * Test the actual values
		assertEquals(1, fw.distance(a.index(), b.index()));
		assertEquals(1, fw.distance(a.index(), c.index()));
		assertEquals(2, fw.distance(a.index(), d.index()));
		assertEquals(2, fw.distance(a.index(), e.index()));
		assertEquals(3, fw.distance(a.index(), f.index()));
		
		assertEquals(2, fw.distance(b.index(), c.index()));
		assertEquals(1, fw.distance(b.index(), d.index()));
		assertEquals(3, fw.distance(b.index(), e.index()));
		assertEquals(2, fw.distance(b.index(), f.index()));
		
		assertEquals(1, fw.distance(c.index(), d.index()));
		assertEquals(1, fw.distance(c.index(), e.index()));
		assertEquals(2, fw.distance(c.index(), f.index()));
		
		assertEquals(2, fw.distance(d.index(), e.index()));
		assertEquals(1, fw.distance(d.index(), f.index()));
		
		assertEquals(1, fw.distance(e.index(), f.index()));

	}

}
