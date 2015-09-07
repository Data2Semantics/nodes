package org.nodes;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.nodes.data.Data;

public class LightUGraphTest
{

	@Test
	public void testMapDTGraph()
	{
		UGraph<String> graph = new LightUGraph<String>();
		assertEquals(0, graph.size());
		assertEquals(0, graph.numLinks());
	}

	@Test
	public void testToString()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b");
		graph.add("c");
	
		a.connect(b);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c"),
		              d = graph.add("d"),
		              e = graph.add("e");
	
		b.connect(a);
		c.connect(a);
		d.connect(a);
		e.connect(a);
		
		System.out.println(graph);
		
		e.disconnect(a);
		
		System.out.println(graph);
		
		System.out.println(a.index());
		a.remove();
		
		System.out.println(graph);	
	}
	
	@Test
	public void testRemove()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c"),
		              d = graph.add("d"),
		              e = graph.add("e");
	
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
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add("a"),
		              b = graph.add("b"),
		              c = graph.add("c");
	
		a.connect(b);
		a.connect(c);
		
		assertTrue(a.connected(b));
		
		assertFalse(a.connected(a));

		assertTrue(b.connected(a));
		
		assertTrue(a.connected(c));

		assertTrue(c.connected(a));
		
		assertFalse(b.connected(c));
		
		assertFalse(c.connected(b));
	}
	
	@Test
	public void testLinks()
	{
		UGraph<String> graph = new LightUGraph<String>();
		
		UNode<String> a = graph.add(null),
		              b = graph.add(null),
		              c = graph.add(null);
	
		a.connect(b);
		
		a.connect(c);
		a.connect(c);
		
		assertEquals(0, a.links(a).size());
		assertEquals(1, a.links(b).size());
		assertEquals(1, b.links(a).size());
		assertEquals(2, a.links(c).size());
		assertEquals(2, c.links(a).size());
	}	
	
	@Test
	public void testEquals()
	{
		UGraph<String> g1 = new LightUGraph<String>();
		g1.add("a");
		g1.add("b");
		g1.add("c");
		
		g1.node("a").connect(g1.node("b"));
		g1.node("b").connect(g1.node("c"));
		
		UGraph<String> g2 = new LightUGraph<String>();
		g2.add("a");
		g2.add("b");
		g2.add("c");

		g2.node("b").connect(g2.node("c"));		
		g2.node("a").connect(g2.node("b"));                    
         
		assertTrue(g1.equals(g2));
		
		g2.node("a").connect(g2.node("c"));
	
		assertFalse(g1.equals(g2));
	}
	
	@Test
	public void testNotEquals()
	{
		UGraph<String> g1 = new LightUGraph<String>();
		
		UTGraph<String, String> g2 = new MapUTGraph<String, String>();
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
	
	@Test
	public void copy()
		throws IOException
	{
		UGraph<String> data = 
				Data.edgeList(new File("/Users/Peter/Documents/datasets/graphs/neural/celegans.txt"), false);
		
		data = Graphs.blank(data, "");
		data = Graphs.toSimpleUGraph(data);

		Data.writeEdgeList(data, new File("/Users/Peter/Documents/datasets/graphs/neural/simple.txt"));
		
	}
}
