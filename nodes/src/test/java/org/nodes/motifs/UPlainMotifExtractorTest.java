package org.nodes.motifs;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.nodes.MapUTGraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.random.RandomGraphs;
import org.nodes.util.FrequencyModel;

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
		
		UPlainMotifExtractor<String> ex = new UPlainMotifExtractor<String>(data, 10000, 3, 7);
		
		for(UGraph<String> sub : ex.subgraphs())
		{
			FrequencyModel<Integer> fm = new FrequencyModel<Integer>();
			for(List<Integer> occurrence : ex.occurrences(sub))
				fm.add(occurrence);
			
			assertTrue(fm.frequency(fm.maxToken()) <= 1.0);
		}
	}
	
}
