package org.nodes.random;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;
import org.nodes.Graphs;
import org.nodes.UTGraph;

import nl.peterbloem.kit.Global;

public class RandomGraphsTest {

	@Test
	public void testRandomSelfLoops() {
		Global.randomSeed();
		
		UTGraph<String, String> graph = RandomGraphs.random(30, 16);
		assertFalse(Graphs.hasSelfLoops(graph));
		
		System.out.println(graph);
		System.out.println(Graphs.shuffle(graph));		
	}

	@Test
	public void testBA() {
		UTGraph<String, String> graph = RandomGraphs.preferentialAttachment(30, 2);
		System.out.println(graph);
		System.out.println(Graphs.shuffle(graph));
	}	
	
	@Test
	public void testBig() {
		RandomGraphs.randomFast(100000, 2000000);
	}	
	
}
