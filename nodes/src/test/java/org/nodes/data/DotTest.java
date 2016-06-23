package org.nodes.data;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;
import org.nodes.UTGraph;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Global;

public class DotTest {

	@Test
	public void testRead() 
	{
		Global.randomSeed();
		
		UTGraph<String, String> graph = RandomGraphs.preferentialAttachment(30, 2);
		
		UTGraph<String, String> out = Dot.readUT(graph.toString());
		
		System.out.println(graph);
		System.out.println(out);
		
		assertEquals(graph.size(), out.size());
		assertEquals(graph.numLinks(), out.numLinks());
	}

}
