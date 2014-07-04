package org.nodes.compression;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.prefix;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DNode;
import org.nodes.MapDTGraph;

public class MotifVarTest
{

	@Test
	public void test()
	{
		DGraph<String> graph = new MapDTGraph<String, String>();
		
		DNode<String> n0 = graph.add("a"), 
		              n1 = graph.add("b"),
		              n2 = graph.add("b"),
		              n3 = graph.add("a"),
				      n4 = graph.add("d"),
		              n5 = graph.add("c");
			
		n0.connect(n1);
		n1.connect(n5);
		n2.connect(n1);
		n2.connect(n4);
		n3.connect(n2);
		n4.connect(n3);
		n5.connect(n4);
		n5.connect(n0);
		              
		DGraph<String> motif = new MapDTGraph<String, String>();
		
		DNode<String> a = motif.add("a"),
		              b = motif.add("b"),
		              v = motif.add(MotifVarTags.VARIABLE_SYMBOL);
		
		a.connect(b);
		b.connect(v);
		v.connect(a);
		
		@SuppressWarnings("unchecked")
		List<List<Integer>> occurrences = asList(
			asList(0, 1, 5),
			asList(3, 2, 4));
		
		MotifVar mv = new MotifVar(graph, motif, occurrences);
		
		double motifExpected = 0.0;
		
		motifExpected += 2.0 * prefix(3) + 2.0 * log2(105.0) - log2(6.0); 
		motifExpected += log2(315.0);
		
		assertEquals(motifExpected, mv.motif(), 0.0001);
		
		assertEquals(2.0 * prefix(2) + 2.0 * - log2((1/2.0)*(1/4.0)) - log2(2.0), mv.silhouetteStructure(), 0.0);
		assertEquals(- log2(1.0/5.0) - log2(3.0/7.0), mv.silhouetteLabels(), 0.0);
		
		assertEquals(prefix(2) + 2.0 * log2(4.0) + log2(8.0), mv.substitutions(), 0.0);
		
		assertEquals(- log2(9.0) + log2(945.0), mv.wiring(), 0.00001);		
		
		System.out.println("tagsset: " + mv.tags());
		System.out.println("labelset: " + mv.labelSets());

	}

}
