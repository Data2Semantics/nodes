package org.nodes.compression;

import static nl.peterbloem.kit.Functions.log2Choose;
import static org.junit.Assert.*;

import org.junit.Test;

public class BinomialCompressorTest
{

	@Test
	public void test()
	{
		int n = 1000000;
		int m = 30000000;
		
		int np = 5, mp = 15;
		
		int nn = n - np - 1;
		int mn = m - mp;
		
		double diff = log2Choose(m, n*(double)n ) - log2Choose(mn, nn*(double)nn); 
		System.out.println(diff);
		
	}

}
