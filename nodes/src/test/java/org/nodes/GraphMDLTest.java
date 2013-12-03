package org.nodes;
//package org.lilian.graphs;
//
//import static org.junit.Assert.*;
//
//import org.junit.Test;
//import org.lilian.graphs.compression.SubdueCompressor;
//import org.lilian.graphs.data.Dot;
//import org.lilian.graphs.random.RandomGraphs;
//import org.lilian.graphs.subdue.GraphMDL;
//import org.lilian.util.Compressor;
//import org.lilian.util.Series;
//import org.lilian.util.distance.CompressionDistance;
//import org.lilian.util.distance.Distance;
//
//public class GraphMDLTest
//{
//	@Test
//	public void simple()
//	{
//		UTGraph<String, String> edge  = Graphs.line(2, "x");
//
//		System.out.println(GraphMDL.mdl(edge));
//	}
//	
//	@Test
//	public void test()
//	{
//		UTGraph<String, String> graph = RandomGraphs.preferentialAttachment(20, 1);
//		UTGraph<String, String> edge  = Graphs.line(4, "x");
//		UTGraph<String, String> star  = Graphs.star(3, "x");
//
//		
//		// System.out.println(GraphMDL.mdl(graph));
//		// for(int threshold : Series.series(20))
//		System.out.println(GraphMDL.mdl(graph, edge, 0.0));
//		System.out.println(GraphMDL.mdl(graph, star, 0.0));
//	}
//	
//	@Test
//	public void testApprox()
//	{
//		UTGraph<String, String> pa = RandomGraphs.preferentialAttachment(100, 1);
//		UTGraph<String, String> er = RandomGraphs.random(pa.size(), pa.numLinks());
//
//		UTGraph<String, String> edge  = Graphs.line(2, "x");
//		UTGraph<String, String> star  = Graphs.line(3, "x");
//
//		
//		// System.out.println(GraphMDL.mdl(graph));
//		// for(int threshold : Series.series(20))
//		System.out.println(GraphMDL.mdl(pa));
//		System.out.println(GraphMDL.mdl(pa, edge, 0.0, 100));
//		System.out.println(GraphMDL.mdl(pa, star, 0.0, 100));
//		
//		System.out.println(GraphMDL.mdl(er));
//		System.out.println(GraphMDL.mdl(er, edge, 0.0, 100));
//		System.out.println(GraphMDL.mdl(er, star, 0.0, 100));
//	}
//	
//	@Test
//	public void test2()
//	{
//		UTGraph<String, String> graph = RandomGraphs.random(20, 0.5);
//		UTGraph<String, String> substructure = Graphs.line(1, "");
//		
//		System.out.println(GraphMDL.mdl(graph));
//		System.out.println(GraphMDL.mdl(graph, substructure, 7));
//	}
//	
//	@Test
//	public void small()
//	{	
//		System.out.println(GraphMDL.mdl(Graphs.line(0, "x")));
//		System.out.println(GraphMDL.mdl(Graphs.line(1, "x")));
//		System.out.println(GraphMDL.mdl(Graphs.line(2, "x")));
//	}
//	
//	@Test
//	public void jbc()
//	{	
//		UTGraph<String, String> substructure = new MapUTGraph<String, String>();
//		// * square 1
//		UTNode<String, String> s1x = substructure.add("x"),
//                               s1y = substructure.add("y"),
//                               s1z = substructure.add("z"),
//                               s1q = substructure.add("q");
//		
//		s1x.connect(s1y);
//		s1y.connect(s1q);
//		s1q.connect(s1z);
//		s1z.connect(s1x);
//		
//		UTGraph<String, String> substructure3 = new MapUTGraph<String, String>();
//
//		UTNode<String, String> 	s3x = substructure3.add("x"),
//                				s3y = substructure3.add("y"),
//				                s3z = substructure3.add("z"),
//				                s3q = substructure3.add("q");
//
//		s3x.connect(s3y);
//		s3q.connect(s3z);
//		s3z.connect(s3x);		
//		
//		UTGraph<String, String> substructure2 = new MapUTGraph<String, String>();
//		// * square 1
//		UTNode<String, String> 	c0 = substructure2.add("c"),
//                               	c1 = substructure2.add("a");
//
//		c0.connect(c1);
//		
//		System.out.println("**" + GraphMDL.mdl(Graphs.jbc()));
//		System.out.println("--" + GraphMDL.mdl(Graphs.jbc(), Graphs.star(3, "c"), 0.0));
//		System.out.println("--" + GraphMDL.mdl(Graphs.jbc(), substructure, 0.0, 100));
//		System.out.println("--" + GraphMDL.mdl(Graphs.jbc(), substructure3, 0.0));
//		System.out.println("--" + GraphMDL.mdl(Graphs.jbc(), substructure2, 0.0, -1));
//	}
//	
//	@Test
//	public void jbc2()
//	{	
//		UTGraph<String, String> substructure = new MapUTGraph<String, String>();
//		// * square 1
//		UTNode<String, String> s1x = substructure.add("x"),
//                               s1y = substructure.add("y"),
//                               s1z = substructure.add("z"),
//                               s1q = substructure.add("q");
//		
//		s1x.connect(s1y);
//		s1y.connect(s1q);
//		s1q.connect(s1z);
//		s1z.connect(s1x);
//		
//		System.out.println("**" + GraphMDL.mdl(Graphs.jbc()));
//		System.out.println("**" + GraphMDL.mdl(Graphs.jbc(), substructure, 0.0));
//	}	
//	
//	@Test
//	public void memoryTest()
//	{
//		UTGraph<String, String> graph = Dot.read("graph {x_1 -- x_2 [label=null]; x_1 -- x_4 [label=null]; x_2 -- x_7 [label=null]; x_2 -- x_9 [label=null]; x_3 -- x_7 [label=null]; x_3 -- x_9 [label=null]; x_4 -- x_5 [label=null]; x_4 -- x_7 [label=null]; x_4 -- x_9 [label=null]; x_5 -- x_6 [label=null]; x_5 -- x_8 [label=null]; x_5 -- x_9 [label=null]; x_6 -- x_9 [label=null]; x_7 -- x_8 [label=null]; x_10 -- x_11 [label=null]; x_10 -- x_12 [label=null]; x_10 -- x_18 [label=null]; x_11 -- x_12 [label=null]; x_11 -- x_14 [label=null]; x_11 -- x_19 [label=null]; x_12 -- x_13 [label=null]; x_12 -- x_18 [label=null]; x_12 -- x_19 [label=null]; x_13 -- x_19 [label=null]; x_14 -- x_15 [label=null]; x_15 -- x_18 [label=null]; x_16 -- x_19 [label=null]; x_17 -- x_18 [label=null]; x_0}");
//		UTGraph<String, String> sub = Dot.read("graph {x_0 -- x_1 [label=null]; x_0 -- x_2 [label=null]; x_0 -- x_3 [label=null]; x_0 -- x_4 [label=null]}");
//		
//		GraphMDL.mdl(graph, sub, 0.0);
//	}
//	
//	@Test
//	public void distanceTest()
//	{
//		for(int i : Series.series(3))
//		{
//			UTGraph<String, String> pa1 = RandomGraphs.preferentialAttachment(100, 1);
//			UTGraph<String, String> pa2 = RandomGraphs.preferentialAttachment(100, 1);
//			UTGraph<String, String> er = RandomGraphs.random(pa1.size(), pa1.numLinks());
//	
//			Compressor<UTGraph<String, String>> comp =
//				new SubdueCompressor<String, String>(
//						5, 1, 5, 10, 0.0, false, 10);
//			Distance<UTGraph<String, String>> distance = 
//					new CompressionDistance<UTGraph<String, String>>(comp);
//			
//			System.out.println(distance.distance(pa1, er));
//			System.out.println(distance.distance(pa1, pa2));
//		}
//		
//	}
//	
//}
