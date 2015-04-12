package org.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

public class LightDTGraphTest
{


	@Test
	public void testMapDTGraph()
	{
		DTGraph<String,String> graph = new LightDTGraph<String,String>();
	}

	@Test
	public void testToString()
	{
		DTGraph<String,String> graph = new LightDTGraph<String,String>();
		
		DTNode<String,String> a = graph.add("a"),
		              		  b = graph.add("b");
		graph.add("c");
	
		a.connect(b, "1");
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		DTGraph<String,String> graph = new LightDTGraph<String,String>();
		
		DTNode<String,String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null),
		              d = graph.add(null),
		              e = graph.add(null);
	
		b.connect(a, "1");
		c.connect(a, "1");
		d.connect(a, "1");
		e.connect(a, "1");
		
		System.out.println(graph);
		
		e.disconnect(a);
		
		System.out.println(graph);
		
		a.remove();
		
		System.out.println(graph);	
	}
	
	@Test
	public void testRemove()
	{
		DTGraph<String, Double> graph = new LightDTGraph<String, Double>();
		
		DTNode<String, Double> a = graph.add(null),
		                       b = graph.add(null),
		                       c = graph.add(null),
		                       d = graph.add(null),
		                       e = graph.add(null);
	
		b.connect(a, 1.0);
		c.connect(a, 2.0);
		d.connect(a, 3.0);
		e.connect(a, 4.0);
		
		//System.out.println(graph.numLinks() + " " + graph.size());
		
		assertEquals(4, graph.numLinks());
		assertEquals(5, graph.size());
		
		a.remove();
		
		assertEquals(0, graph.numLinks());
		assertEquals(4, graph.size());
	}
	
	@Test
	public void testConnected()
	{
		DTGraph<String,String> graph = new LightDTGraph<String,String>();
		
		DTNode<String,String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b, null);
		a.connect(c, null);
		
		assertTrue(a.connected(b));
		assertTrue(a.connectedTo(b));
		
		assertFalse(a.connected(a));
		assertFalse(a.connectedTo(a));

		assertTrue(b.connected(a));
		assertFalse(b.connectedTo(a));
		
		assertTrue(a.connected(c));
		assertTrue(a.connectedTo(c));

		assertTrue(c.connected(a));
		assertFalse(c.connectedTo(a));
		
		assertFalse(b.connected(c));
		assertFalse(b.connectedTo(c));
		
		assertFalse(c.connected(b));
		assertFalse(c.connectedTo(b));
	}
	
	@Test
	public void testLinks()
	{
		DTGraph<String,String> graph = new LightDTGraph<String,String>();
		
		DTNode<String,String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b, null);
		
		a.connect(c, null);
		a.connect(c, null);
		
		assertEquals(0, a.links(a).size());
		assertEquals(1, a.links(b).size());
		assertEquals(1, b.links(a).size());
		assertEquals(2, a.links(c).size());
		assertEquals(2, c.links(a).size());
	}	
	
	@Test
	public void testEquals()
	{
		DTGraph<String,String> g1 = new LightDTGraph<String,String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"), "1");
		g1.node("b").connect(g1.node("c"), "2");
		
		DTGraph<String,String> g2 = new LightDTGraph<String,String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");
		 
		g2.node("a").connect(g2.node("b"), "1");                    
		g2.node("b").connect(g2.node("c"), "2");		
         
		assertTrue(g1.equals(g2));
		
		g2.node("a").connect(g2.node("c"));
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void testNotEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		
		DTGraph<String, String> g2 = new LightDTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
}
