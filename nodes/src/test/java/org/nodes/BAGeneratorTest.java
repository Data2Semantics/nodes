//package org.nodes;
//
//
//-- Restore when powerlaws has a public repo
//
//import static org.junit.Assert.*;
//
//import nl.peterbloem.powerlaws.Discrete;
//
//import org.junit.Test;
//import org.lilian.graphs.random.*;
//
//public class BAGeneratorTest
//{
//
//	@Test
//	public void test()
//	{
//		UTGraph<String, String> graph = RandomGraphs.preferentialAttachment(15, 2);
//		
//		System.out.println(graph.size());
//		System.out.println(graph);
//		
//		Discrete pl = Discrete.fit(Graphs.degrees(graph)).fit();
//		System.out.println(pl.exponent());
//		System.out.println(pl.xMin());
//		
//		System.out.println(pl.significance(Graphs.degrees(graph), 0.1));
//	}
//
//}
