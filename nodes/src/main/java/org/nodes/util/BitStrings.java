package org.nodes.util;

public class BitStrings
{

	/**
	 * Returns a 64 bit string representing the given double in standard 
	 * floating point format 
	 * @param in
	 * @return
	 */
	public static BitString fromDouble(double in)
	{
		long lng = Double.doubleToRawLongBits(in);
		return fromLong(lng);
	}
	
	public static BitString fromLong(long in)
	{
		BitString res = new BitString(64);
		
		String str = Long.toBinaryString(in);
		for(int i = 0; i < 64 - str.length(); i++)
			res.add(false);
		for(int i = 0; i < str.length(); i++)
			res.add(str.charAt(i) == '0' ? false : true);
		
		return res;
	}
		
	
	/**
	 * Takes the first 64 bits of the given bitstring and interprets them as a 
	 * double value.
	 * 
	 * @param in
	 * @return
	 */
	public static double toDouble(BitString in)
	{
		return Double.longBitsToDouble(toLong(in));
	}	
	
	public static long toLong(BitString in)
	{
		return Long.parseLong(in.toString(), 2);
	}
}
