package org.nodes.util;

import static org.junit.Assert.*;
import static org.nodes.util.BitString.parse;

import org.junit.Before;
import org.junit.Test;
import org.nodes.Global;

import java.util.*;

public class BitStringTest {
	
	public void setup()
	{
		Global.randomSeed();
	}
	
	@Test
	public void bitStringTest1()
	{
		int length = Global.random().nextInt(100);
		String str = "";
		for(int i = 0; i < length; i++)
			str += Global.random().nextBoolean() ? '0' : '1';
		
		BitString bs =  BitString.parse(str);
		
		assertEquals(str, bs.toString());
	}
	
	@Test
	public void bitStringTest2()
	{
		String in  = "101010101010101010101010101010",
		       out = "010010101010010010101000011111";
		BitString bs = BitString.parse(in);
		bs.set(0, false);
		bs.set(1, true);		
		bs.set(2, false);
		
		bs.set(12, false);
		bs.set(13, true);		
		bs.set(14, false);		

		bs.set(22, false);
		bs.set(23, false);
		bs.set(24, false);
		bs.set(25, false);
		bs.set(26, false);		
				
		bs.set(25, true);
		bs.set(26, true);
		bs.set(27, true);
		bs.set(28, true);
		bs.set(29, true);		
		
		assertEquals(out, bs.toString());
	}
	
	@Test
	public void paddingTest()
	{
		BitString bs = random(Global.random().nextInt(100));
		assertEquals(0, (bs.size() + bs.padding()) % 8);		
	}
	
	@Test
	public void zerosTest()
	{
		String exp = "0000000000000000000000000";
		BitString bs = BitString.zeros(exp.length());
		
		assertEquals(exp, bs.toString());
	}
	
	@Test
	public void toIntegerTest()
	{
		BitString string = new BitString(2);
		for(int i : Series.series(129))
			string.add(false);
		
		List<Integer> ints = string.toIntegers();
		assertEquals(5, ints.size());
	}

	@Test
	public void toIntegerTest2()
	{
		BitString string = new BitString(2);
		for(int i : Series.series(1000))
			string.add(Global.random().nextBoolean());
		
		List<Integer> ints = string.toIntegers();
		double sum = 0.0;
		for(int i : ints)
			sum += i;
		
		System.out.println(sum);
	}
	
	/** 
	 * Returns a random string
	 */
	public BitString random(int length)
	{
		String str = "";
		for(int i = 0; i < length; i++)
			str += Global.random().nextBoolean() ? '0' : '1';
		
		return BitString.parse(str);
	}
	
	@Test 
	public void incrementTest()
	{
		BitString in = new BitString();
		in.increment();
		
		assertEquals(parse("0"), in);
		
		in = parse("1111");
		in.increment();
		assertEquals(parse("00000"), in);
		
		in = parse("10101");
		in.increment();
		assertEquals(parse("01101"), in);
		
		in = parse("1111110");
		in.increment();
		assertEquals(parse("0000001"), in);
	}
	
	@Test
	public void collectionTest()
	{
		Set<BitString> actual   = new HashSet<BitString>(BitString.all(5));
		Set<BitString> expected = new HashSet<BitString>(
				Arrays.asList(
					parse("00000"),
					parse("00001"),
					parse("00010"),
					parse("00011"),
					parse("00100"),
					parse("00101"),
					parse("00110"),
					parse("00111"),
					parse("01000"),
					parse("01001"),
					parse("01010"),
					parse("01011"),
					parse("01100"),
					parse("01101"),
					parse("01110"),
					parse("01111"),
					parse("10000"),
					parse("10001"),
					parse("10010"),
					parse("10011"),
					parse("10100"),
					parse("10101"),
					parse("10110"),
					parse("10111"),
					parse("11000"),
					parse("11001"),
					parse("11010"),
					parse("11011"),
					parse("11100"),
					parse("11101"),
					parse("11110"),
					parse("11111")	
					));
		
		assertEquals(expected, actual);
 	}
	
	@Test
	public void collectionTest2()
	{
		for(BitString mask : BitString.all(5))
		{
			System.out.println(mask);
		}
	}
}
