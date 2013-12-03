package org.nodes.algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.nodes.Global;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;
import org.nodes.algorithms.UTVF2;

public class DVF2Test
{

	@Test
	public void test()
	{
		DTGraph<String, String> a = new MapDTGraph<String, String>(),
		                  		b = new MapDTGraph<String, String>();
		
		DTNode<String, String>a0 = a.add("a0");
		DTNode<String, String>a1 = a.add("a1");
		DTNode<String, String>a2 = a.add("a2");

		DTNode<String, String>b0 = b.add("b0");
		DTNode<String, String>b1 = b.add("b1");
		DTNode<String, String>b2 = b.add("b2");
		
		a0.connect(a1);
		a1.connect(a2);
		
		b0.connect(b2);
		b2.connect(b1);
		
		System.out.println(a);
		System.out.println(b);
		
		DVF2<String> vfs;
		
		vfs = new DVF2<String>(a, b, true);
		assertFalse(vfs.matches());
		
		vfs = new DVF2<String>(a, b, false);
		assertTrue(vfs.matches());
	
	}
	
	@Test
	public void test2()
	{
		DTGraph<String, String> a = new MapDTGraph<String, String>(),
		                		b = new MapDTGraph<String, String>();
		              
		DTNode<String, String> aa = a.add("0");
		DTNode<String, String> ab = a.add("1");
		DTNode<String, String> ac = a.add("2");
		DTNode<String, String> ad = a.add("3");
		DTNode<String, String> ag = a.add("4");
		DTNode<String, String> ah = a.add("5");
		DTNode<String, String> ai = a.add("6");
		DTNode<String, String> aj = a.add("7");
	    
		DTNode<String, String> bb = b.add("1");
		DTNode<String, String> ba = b.add("0");
		DTNode<String, String> bc = b.add("2");
		DTNode<String, String> bg = b.add("4");		
		DTNode<String, String> bd = b.add("3");
		DTNode<String, String> bj = b.add("7");
		DTNode<String, String> bh = b.add("5");
		DTNode<String, String> bi = b.add("6");		


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
		
		DVF2<String> vfs;
		
//		vfs = new UndirectedVF2<String, BaseGraph<String>.Node>(a, b, true);
//		assertTrue(vfs.matches());
		
		vfs = new DVF2<String>(a, b, false);
		assertTrue(vfs.matches());
	
	}
	
	@Test
	public void test3()
	{
		Global.randomSeed();
		
		DTGraph<String, String> a = new MapDTGraph<String, String>(),
		                        b = new MapDTGraph<String, String>();
		
		DTNode<String, String> aa = a.add("0");
		DTNode<String, String> ab = a.add("1");
		DTNode<String, String> ac = a.add("2");
		DTNode<String, String> ad = a.add("3");
		DTNode<String, String> ag = a.add("4");
		DTNode<String, String> ah = a.add("5");
		DTNode<String, String> ai = a.add("6");
		DTNode<String, String> aj = a.add("7");
				
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
		
		DVF2<String> vfs;
		
//		vfs = new UndirectedVF2<String, BaseGraph<String>.Node>(a, b, true);
//		assertTrue(vfs.matches());
		
		vfs = new DVF2<String>(a, b, false);
		assertTrue(vfs.matches());
		
		
		aa.disconnect(ag);
		vfs = new DVF2<String>(a, b, false);
		assertFalse(vfs.matches());
	
	}
	

}
