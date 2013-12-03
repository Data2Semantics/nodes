package org.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

public class LightDGraphTest
{


	@Test
	public void testMapDTGraph()
	{
		DGraph<String> graph = new LightDGraph<String>();
	}

	@Test
	public void testToString()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add("a"),
		              b = graph.add("b");
		graph.add("c");
	
		a.connect(b);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null),
		              e = graph.add(null);
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph);
		
		e.disconnect(a);
		
		System.out.println(graph);
		
		a.remove();
		
		System.out.println(graph);	
	}
	
	@Test
	public void testRemove()
	{
		DTGraph<String, Double> graph = new MapDTGraph<String, Double>();
		
		DTNode<String, Double> a = graph.add(null),
		                       b = graph.add(null),
		                       c = graph.add(null),
		                       d = graph.add(null),
		                       e = graph.add(null);
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph.numLinks() + " " + graph.size());
		
		assertEquals(4, graph.numLinks());
		assertEquals(5, graph.size());
		
		a.remove();
		
		assertEquals(0, graph.numLinks());
		assertEquals(4, graph.size());
	}
	
	@Test
	public void testConnected()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b);
		a.connect(c);
		
		assertTrue(a.connected(b));
		assertFalse(a.connected(a));
		assertFalse(b.connected(a));
		assertTrue(a.connected(c));
		assertFalse(c.connected(a));
		assertFalse(b.connected(c));
		assertFalse(c.connected(b));
	}
	
	@Test
	public void testLinks()
	{
		DGraph<String> graph = new LightDGraph<String>();
		
		DNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b);
		
		a.connect(c);
		a.connect(c);
		
		assertEquals(0, a.links(a).size());
		assertEquals(1, a.links(b).size());
		assertEquals(0, b.links(a).size());
		assertEquals(2, a.links(c).size());
		assertEquals(0, c.links(a).size());
	}	
	
	@Test
	public void testEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"));
		g1.node("b").connect(g1.node("c"));
		
		DGraph<String> g2 = new LightDGraph<String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");
		 
		g2.node("a").connect(g2.node("b"));                    
		g2.node("b").connect(g2.node("c"));		
         
		assertTrue(g1.equals(g2));
		
		g2.node("a").connect(g2.node("c"));
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void testNotEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		
		DTGraph<String, String> g2 = new MapDTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
}
