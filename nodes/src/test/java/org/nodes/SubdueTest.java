package org.nodes;
//package org.lilian.graphs;
//
//import static org.junit.Assert.*;
//
//import java.util.Collection;
//
//import org.junit.Test;
//import org.lilian.graphs.random.RandomGraphs;
//import org.lilian.graphs.subdue.CostFunctions;
//import org.lilian.graphs.subdue.FindRules;
//import org.lilian.graphs.subdue.GraphMDL;
//import org.lilian.graphs.subdue.InexactCost;
//import org.lilian.graphs.subdue.Subdue;
//
//public class SubdueTest
//{
//
//	@Test
//	public void test()
//	{
//		UTGraph<String, String> pa1 = RandomGraphs.preferentialAttachment(100, 1);
//		UTGraph<String, String> pa2 = RandomGraphs.preferentialAttachment(100, 1);
//		UTGraph<String, String> er = RandomGraphs.random(pa1.size(), pa2.numLinks());
//		
//		UTGraph<String, String> conc = new MapUTGraph<String, String>();
//		
//		Graphs.add(conc, pa1);
//		Graphs.add(conc, pa2);
//		
//		InexactCost<String> costFunction = CostFunctions.uniform();
//		Subdue<String, String> sub = 
//				new Subdue<String, String>(
//						conc, costFunction, 0.0, 100);
//		
//		Collection<Subdue<String, String>.Substructure> subs =
//				sub.search(10, 5, 10, 5);
//		
//		System.out.println("---" + GraphMDL.mdl(conc));
//		for(Subdue<String, String>.Substructure structure : subs)
//			System.out.println(structure);
//	}
//
//	@Test
//	public void findRules()
//	{
//		UTGraph<String, String> in = 
//				Graphs.jbc();
//
//		FindRules.findRules(in, 3);
//	}
//}
