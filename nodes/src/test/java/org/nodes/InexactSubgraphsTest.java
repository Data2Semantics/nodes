package org.nodes;
//package org.lilian.graphs;
//
//import static org.junit.Assert.*;
//
//import org.junit.Test;
//import org.lilian.graphs.data.Dot;
//import org.lilian.graphs.subdue.CostFunctions;
//import org.lilian.graphs.subdue.GraphMDL;
//import org.lilian.graphs.subdue.InexactCost;
//import org.lilian.graphs.subdue.InexactSubgraphs;
//
//
//public class InexactSubgraphsTest
//{
//
//	@Test
//	public void test()
//	{
//		UTGraph<String, String> line = Graphs.line(2, "x");
//		UTGraph<String, String> ladder = Graphs.ladder(2, "x");
//		
//		System.out.println(line);
//		System.out.println(ladder);
//		
//		InexactCost<String> cost = 
//				CostFunctions.transformationCost(1, ladder.size(), ladder.numLinks());
//		InexactSubgraphs<String, String> is =
//				new InexactSubgraphs<String, String>(
//						ladder, line, cost, 10.0, false);
//		
//		System.out.println(is.numMatches());
//		System.out.println(is.numLinks());
//		System.out.println(is.silhouette());
//	}
//	
//	@Test
//	public void memoryTest()
//	{
//		UTGraph<String, String> graph = Dot.read("graph {x_0 -- x_8 [label=null]; x_0 -- x_9 [label=null]; x_1 -- x_8 [label=null]; x_1 -- x_9 [label=null]; x_2 -- x_3 [label=null]; x_2 -- x_6 [label=null]; x_2 -- x_9 [label=null]; x_3 -- x_4 [label=null]; x_3 -- x_9 [label=null]; x_4 -- x_8 [label=null]; x_4 -- x_9 [label=null]; x_5 -- x_7 [label=null]; x_5 -- x_8 [label=null]; x_6 -- x_9 [label=null]; x_7 -- x_8 [label=null]; x_7 -- x_9 [label=null]; x_8 -- x_9 [label=null]; x_10 -- x_18 [label=null]; x_10 -- x_19 [label=null]; x_11 -- x_18 [label=null]; x_11 -- x_19 [label=null]; x_12 -- x_13 [label=null]; x_12 -- x_16 [label=null]; x_12 -- x_19 [label=null]; x_13 -- x_14 [label=null]; x_13 -- x_19 [label=null]; x_14 -- x_18 [label=null]; x_14 -- x_19 [label=null]; x_15 -- x_17 [label=null]; x_15 -- x_18 [label=null]; x_16 -- x_19 [label=null]; x_17 -- x_18 [label=null]; x_17 -- x_19 [label=null]; x_18 -- x_19 [label=null]}");
//		UTGraph<String, String> sub = Dot.read("graph {x_0 -- x_1 [label=null]; x_0 -- x_2 [label=null]; x_0 -- x_3 [label=null]; x_1 -- x_4 [label=null]; x_2 -- x_4 [label=null]}");
//		
//		InexactCost<String> cost = CostFunctions.uniform();
//		
//		InexactSubgraphs<String, String> isFast = 
//				new InexactSubgraphs<String, String>(graph, sub, cost, 0.0, false, 100000);	
//		System.out.println("_");
//		InexactSubgraphs<String, String> is = 
//				new InexactSubgraphs<String, String>(graph, sub, cost, 0.0, false);
//	}
//
//}
