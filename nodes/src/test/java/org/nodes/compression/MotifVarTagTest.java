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
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

public class MotifVarTagTest
{

	@Test
	public void test()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		DTNode<String, String> n0 = graph.add("a"), 
		              n1 = graph.add("b"),
		              n2 = graph.add("b"),
		              n3 = graph.add("a"),
				      n4 = graph.add("d"),
		              n5 = graph.add("c");
			
		n0.connect(n1, "beta");
		n1.connect(n5, "beta");
		n5.connect(n0, "alpha");
		n5.connect(n0, "beta");
		n5.connect(n0, "alpha");

		n1.connect(n2, "beta");		
		n4.connect(n5, "beta");

		n2.connect(n3, "beta");
		n2.connect(n4, "beta");
		n4.connect(n3, "alpha");
		n4.connect(n3, "alpha");
		n4.connect(n3, "alpha");

		
		              
		DTGraph<String, String> motif = new MapDTGraph<String, String>();
		
		DTNode<String, String> a = motif.add("a"),
		              b = motif.add("b"),
		              v = motif.add(MotifVarTags.VARIABLE_SYMBOL);
		
		a.connect(b, "beta");
		b.connect(v, "beta");
		v.connect(a, "alpha");
		v.connect(a, MotifVarTags.VARIABLE_SYMBOL);
		v.connect(a, MotifVarTags.VARIABLE_SYMBOL);
		
		@SuppressWarnings("unchecked")
		List<List<Integer>> occurrences = asList(
			asList(0, 1, 5),
			asList(3, 2, 4));
		
		MotifVarTags mv = new MotifVarTags(graph, motif, occurrences, true);
		
		double motifExpected = 0.0;
		
		// structure
		motifExpected += prefix(3) + prefix(5) + 2.0 * (- log2(15.0) + log2(10395.0)) - log2(120.0);
		// labels
		motifExpected += log2(315.0);
		// tags
		motifExpected += log2(10395.0) - log2(9.0);
		
		assertEquals(motifExpected, mv.motif(), 0.0001);
		
		assertEquals(2.0 * prefix(2) + 2.0 * - log2((1/2.0)*(1/4.0)) - log2(2.0), mv.silhouetteStructure(), 0.0);
		assertEquals(- log2(1.0/5.0) - log2(3.0/7.0) + log2(8.0) - log2(3.0), mv.silhouetteLabels(), 0.0);
		
		assertEquals(prefix(2) + 2.0 * log2(4.0) + log2(8.0), mv.labelSubstitutions(), 0.0);
		assertEquals(prefix(1) + 1.0 + prefix(2) + 2.0 + log2(1.0) + log2(8.0), mv.tagSubstitutions(), 0.0);
		
		assertEquals(- log2(9.0) + log2(945.0), mv.wiring(), 0.00001);		
		
		System.out.println("labelset: " + mv.labelSets());

	}

}
