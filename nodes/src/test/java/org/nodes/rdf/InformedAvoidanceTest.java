package org.nodes.rdf;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import org.junit.Test;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import nl.peterbloem.kit.Series;
import nl.peterbloem.kit.data.classification.Classification;
import nl.peterbloem.kit.data.classification.Classified;

public class InformedAvoidanceTest
{

	@Test
	public void test()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		Node<String> a = graph.add("a"),
		             b = graph.add("b"),
		             c = graph.add("c"),
		             d = graph.add("d"),
		             e = graph.add("e"),
		             f = graph.add("f"),
		             g = graph.add("g");
		
		a.connect(b);
		b.connect(c);
		b.connect(f);
		c.connect(d);
		c.connect(g);
		d.connect(e);
		f.connect(g);
		
		Classified<Node<String>> instances = Classification.empty();
		
		instances.add(c, 0);
		instances.add(f, 0);
		instances.add(e, 1);
		
		InformedAvoidance ia = new InformedAvoidance(graph, instances, 1);

		// * class priors
		assertEquals(2.0/3.0, ia.p(0), 0.0);
		assertEquals(1.0/3.0, ia.p(1), 0.0);
		
		// * marginals
		assertEquals(0.0,     ia.p(a, 1), 0.0);
		assertEquals(2.0/3.0, ia.p(b, 1), 0.0);
		assertEquals(1.0/3.0, ia.p(c, 1), 0.0);
		assertEquals(2.0/3.0, ia.p(d, 1), 0.0);
		assertEquals(1.0/3.0, ia.p(e, 1), 0.0);
		assertEquals(1.0/3.0, ia.p(f, 1), 0.0);
		assertEquals(2.0/3.0, ia.p(g, 1), 0.0);
	
		// * class 0
		assertEquals(0.0,     ia.p(a, 0, 1), 0.0);
		assertEquals(2.0/2.0, ia.p(b, 0, 1), 0.0);
		assertEquals(1.0/2.0, ia.p(c, 0, 1), 0.0);
		assertEquals(1.0/2.0, ia.p(d, 0, 1), 0.0);
		assertEquals(0.0    , ia.p(e, 0, 1), 0.0);
		assertEquals(1.0/2.0, ia.p(f, 0, 1), 0.0);
		assertEquals(2.0/2.0, ia.p(g, 0, 1), 0.0);
		
		// * class 1
		assertEquals(0.0,     ia.p(a, 1, 1), 0.0);
		assertEquals(0.0,     ia.p(b, 1, 1), 0.0);
		assertEquals(0.0,     ia.p(c, 1, 1), 0.0);
		assertEquals(1.0,     ia.p(d, 1, 1), 0.0);
		assertEquals(1.0,     ia.p(e, 1, 1), 0.0);
		assertEquals(0.0,     ia.p(f, 1, 1), 0.0);
		assertEquals(0.0,     ia.p(g, 1, 1), 0.0);
		
		// * summed marginals
		assertEquals(0.0,     sum(ia, a, instances), 0.0);
		assertEquals(2.0/3.0, sum(ia, b, instances), 0.0);
		assertEquals(1.0/3.0, sum(ia, c, instances), 0.0);
		assertEquals(2.0/3.0, sum(ia, d, instances), 0.0);
		assertEquals(1.0/3.0, sum(ia, e, instances), 0.0);
		assertEquals(1.0/3.0, sum(ia, f, instances), 0.0);
		assertEquals(2.0/3.0, sum(ia, g, instances), 0.0);
	}

	private double sum(InformedAvoidance ia, Node<String> node, Classified<Node<String>> instances)
	{
		double sum = 0.0;
		
		for(int cls : series(instances.numClasses()))
			sum += ia.p(cls) * ia.p(node, cls, 1);
		
		return sum;
	}
	
	@Test
	public void testSearch()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		Node<String> a = graph.add("a"),
		             b = graph.add("b"),
		             c = graph.add("c"),
		             d = graph.add("d"),
		             e = graph.add("e"),
		             f = graph.add("f"),
		             g = graph.add("g");
		
		a.connect(b);
		b.connect(c);
		b.connect(f);
		c.connect(d);
		c.connect(g);
		d.connect(e);
		f.connect(g);
		
		Classified<Node<String>> instances = Classification.empty();
		
		instances.add(c, 0);
		instances.add(f, 0);
		instances.add(e, 1);
		
		InformedAvoidance ia = new InformedAvoidance(graph, instances, 1);
		
		SearchInstances si = new SearchInstances(graph, 1000, 25, 1, ia);
		
		for(Node<String> node : graph.nodes())
			System.out.println(node + " " + c(node, ia, instances) + ia.classEntropy(node, 1));
		
		System.out.println(si.instance((DNode<String>)c));
	}
	
	public String c(Node<String> node, InformedAvoidance ia, Classified<Node<String>> instances)
	{
		String out = "[";
				
		for(int cls : series(instances.numClasses()))
			out += " " + f(ia.pClass(cls, node, 1)) + " ";
		
		return out + "]";
	}

	public String f(double x)
	{
		return String.format("%.3f", x);
	}
	
}
