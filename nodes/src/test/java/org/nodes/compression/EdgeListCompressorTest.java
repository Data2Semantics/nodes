package org.nodes.compression;

import static nl.peterbloem.kit.Functions.prefix;
import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.log2;
import static org.nodes.compression.Functions.tic;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Series;

public class EdgeListCompressorTest
{

	@Test
	public void test()
	{
		DGraph<String> data = new MapDTGraph<String,String>();
		
		Node<String> a = data.add("a");
		Node<String> b = data.add("b");
		Node<String> c = data.add("c");
		
		a.connect(b);
		b.connect(c);
		c.connect(a);
		
		EdgeListCompressor<String> comp = new EdgeListCompressor<String>();
				
		assertEquals(
				nl.peterbloem.kit.Functions.prefix(3) + nl.peterbloem.kit.Functions.prefix(3)
				- log2(1.0/9) + - log2(1.0/25) - log2(1.0/49)
				- 2.58496250072, 
				comp.directed(data), 0.001);		
	}
	
	public void speed()
	{
		DGraph<String> graph = RandomGraphs.preferentialAttachmentDirected(10000, 3);
		EdgeListCompressor<String> comp = new EdgeListCompressor<String>();
		System.out.println(".");
		
		tic();
		comp.compressedSize(graph);
		System.out.println(Functions.toc());
	}

}
