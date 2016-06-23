package org.nodes.motifs;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.MapUTGraph;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.FrequencyModel;

public class UPlainMotifExtractorTest
{

	@Test
	public void test()
	{
		UGraph<String> data = new MapUTGraph<String, String>();
		
		UNode<String> a = data.add(""),
		              b = data.add(""),
		              c = data.add(""),
		              d = data.add(""),
		              e = data.add(""),
		              f = data.add(""),
		              g = data.add(""),
		              h = data.add(""),
		              i = data.add(""),
		              j = data.add(""),
		              k = data.add(""),
		              l = data.add(""),
		              m = data.add("");
		
		a.connect(b);
		b.connect(c);
		c.connect(a);
		
		d.connect(e);
		e.connect(f);
		f.connect(d);

		g.connect(h);
		h.connect(i);
		i.connect(g);
		
		j.connect(k);
		k.connect(l);
		l.connect(j);
		
		m.connect(a);
		m.connect(d);
		m.connect(g);
		m.connect(j);
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 10000, 3, 7);
		
		for(UGraph<String> sub : ex.subgraphs())
		{
			System.out.println("subgraph (f=" + ex.frequency(sub) + ") : " + sub);
			
			System.out.println("instances:");
			
			FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
			for(List<Integer> occurrence : ex.occurrences(sub))
			{
				fm.add(occurrence);
				System.out.println("  - " + occurrence);
			}
			
			assertTrue(fm.frequency(fm.maxToken()) <= 1.0);
				
			System.out.println();
		}
	}

	@Test
	public void testRandom()
	{
		UGraph<String> data = RandomGraphs.random(100, 0.5);
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 500, 3, 7);
		
		for(UGraph<String> sub : ex.subgraphs())
		{
			FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
			for(List<Integer> occurrence : ex.occurrences(sub))
				fm.add(occurrence);
			
			assertTrue(fm.frequency(fm.maxToken()) <= 1.0);
		}
	}
	
	@Test
	public void testOverlaps()
	{
		UGraph<String> data = RandomGraphs.random(1000, 2000);
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 3000, 3, 6);
		
		for(UGraph<String> sub : ex.subgraphs())
		{
			int total = 0;
			Set<Integer> nodes = new HashSet<Integer>();
			for(List<Integer> i : ex.occurrences(sub))
			{
				nodes.addAll(i);
				total += i.size();
			}
			
			assertEquals(total, nodes.size());
		}
	}
	
	@Test
	public void testuniqueNodes()
	{		
		UGraph<String> data = RandomGraphs.random(100, 400);

		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 50000, 3, 6);
		for(UGraph<String> sub : ex.subgraphs())
			for(List<Integer> occ : ex.occurrences(sub))
			{
				Set<Integer> set = new LinkedHashSet<Integer>(occ);
				assertEquals(set.size(), occ.size());
			}
	}
	
	@Test
	public void testMotifs()
	{
		UGraph<String> data = RandomGraphs.random(1000, 2000);
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 3000, 3, 6);
		
		for(UGraph<String> sub : ex.subgraphs())
			for(List<Integer> occ : ex.occurrences(sub))
			{
				UGraph<String> ext = Subgraph.uSubgraphIndices(data, occ);
				
				assertEquals(sub, ext);
			}
		}

	
}
