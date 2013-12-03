package org.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

public class GraphsTest
{

	@Test
	public void testSingle()
	{
		
		
		UTGraph<String, String> single = Graphs.single("x");
		
		System.out.println(single);
		assertEquals(1, single.size());
	}
	
	@Test
	public void testLine()
	{
		UTGraph<String, String> line = Graphs.line(3, "x");
		
		System.out.println(line);
		assertEquals(3, line.size());
		
		UTNode<String, String> node = line.node("x");
		System.out.println(line.nodes().get(1).links(line.nodes().get(2)));
	}
	
	@Test
	public void testLadder()
	{
		int n = 3;
		Graph<String> ladder = Graphs.ladder(3, "x");
		
		System.out.println(ladder);
		assertEquals(n + 2 * (n - 1), ladder.numLinks());
		assertEquals(n*2, ladder.size());
	}
	
	@Test
	public void testAdd()
	{
		UTGraph<String, String> empty = new MapUTGraph<String, String>();
		UTGraph<String, String> k3 = Graphs.k(3, "x");
		
		Graphs.add(empty, k3);
		
		System.out.println(k3);
		System.out.println(empty);
				
		Graphs.add(empty, k3);
		
		System.out.println(empty);
		
		
	}

//	@Test
//	public void testJBC()
//	{
//		BaseGraph<String> jbc = Graphs.jbc();
//		
//		System.out.println(jbc);
//	}

}
