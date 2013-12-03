package org.nodes.algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.nodes.Global;
import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;
import org.nodes.algorithms.UTVF2;

public class VF2Test
{

	@Test
	public void test()
	{
		UTGraph<String, String> a = new MapUTGraph<String, String>(),
		                  		b = new MapUTGraph<String, String>();
		
		UTNode<String, String>a0 = a.add("a0");
		UTNode<String, String>a1 = a.add("a1");
		UTNode<String, String>a2 = a.add("a2");

		UTNode<String, String>b0 = b.add("b0");
		UTNode<String, String>b1 = b.add("b1");
		UTNode<String, String>b2 = b.add("b2");
		
		a0.connect(a1);
		a1.connect(a2);
		
		b0.connect(b1);
		b0.connect(b2);
		
		System.out.println(a);
		System.out.println(b);
		
		UTVF2<String, String> vfs;
		
		vfs = new UTVF2<String, String>(a, b, true);
		assertFalse(vfs.matches());
		
		vfs = new UTVF2<String, String>(a, b, false);
		assertTrue(vfs.matches());
	
	}
	
	@Test
	public void test2()
	{
		UTGraph<String, String> a = new MapUTGraph<String, String>(),
		                		b = new MapUTGraph<String, String>();
		              
		UTNode<String, String> aa = a.add("0");
		UTNode<String, String> ab = a.add("1");
		UTNode<String, String> ac = a.add("2");
		UTNode<String, String> ad = a.add("3");
		UTNode<String, String> ag = a.add("4");
		UTNode<String, String> ah = a.add("5");
		UTNode<String, String> ai = a.add("6");
		UTNode<String, String> aj = a.add("7");
	
		UTNode<String, String> bb = b.add("1");
		UTNode<String, String> ba = b.add("0");
		UTNode<String, String> bc = b.add("2");
		UTNode<String, String> bg = b.add("4");		
		UTNode<String, String> bd = b.add("3");
		UTNode<String, String> bj = b.add("7");
		UTNode<String, String> bh = b.add("5");
		UTNode<String, String> bi = b.add("6");		


		aa.connect(ag);
		aa.connect(ah);
		aa.connect(ai);
		ab.connect(ag);
		ab.connect(ah);
		ab.connect(aj);
		ac.connect(ag);
		ac.connect(ai);
		ac.connect(aj);
		ad.connect(ah);
		ad.connect(ai);
		ad.connect(aj);

		ba.connect(bg);
		ba.connect(bh);
		ba.connect(bi);
		bb.connect(bg);
		bb.connect(bh);
		bb.connect(bj);
		bc.connect(bg);
		bc.connect(bi);
		bc.connect(bj);
		bd.connect(bh);
		bd.connect(bi);
		bd.connect(bj);
		
		System.out.println(a.size());
		System.out.println(b.size());
		
		UTVF2<String, String> vfs;
		
//		vfs = new UndirectedVF2<String, BaseGraph<String>.Node>(a, b, true);
//		assertTrue(vfs.matches());
		
		vfs = new UTVF2<String, String>(a, b, false);
		assertTrue(vfs.matches());
	
	}
	
	@Test
	public void test3()
	{
		Global.randomSeed();
		
		UTGraph<String, String> a = new MapUTGraph<String, String>(),
		                        b = new  MapUTGraph<String, String>();
		
		UTNode<String, String> aa = a.add("0");
		UTNode<String, String> ab = a.add("1");
		UTNode<String, String> ac = a.add("2");
		UTNode<String, String> ad = a.add("3");
		UTNode<String, String> ag = a.add("4");
		UTNode<String, String> ah = a.add("5");
		UTNode<String, String> ai = a.add("6");
		UTNode<String, String> aj = a.add("7");
				
		aa.connect(ag);
		aa.connect(ah);
		aa.connect(ai);
		ab.connect(ag);
		ab.connect(ah);
		ab.connect(aj);
		ac.connect(ag);
		ac.connect(ai);
		ac.connect(aj);
		ad.connect(ah);
		ad.connect(ai);
		ad.connect(aj);
		
		List<String> labels = new ArrayList<String>(a.labels());
		Collections.shuffle(labels, Global.random());
		
		for(String label : labels)
			b.add(label);
			
		for(String first : labels)
			for(String second : labels)
				if(a.node(first).connected(a.node(second)))
					b.node(first).connect(b.node(second));
				
		System.out.println(a.size());
		System.out.println(b.size());
		
		UTVF2<String, String> vfs;
		
//		vfs = new UndirectedVF2<String, BaseGraph<String>.Node>(a, b, true);
//		assertTrue(vfs.matches());
		
		vfs = new UTVF2<String, String>(a, b, false);
		assertTrue(vfs.matches());
		
		
		aa.disconnect(ag);
		vfs = new UTVF2<String, String>(a, b, false);
		assertFalse(vfs.matches());
	
	}
	

}
