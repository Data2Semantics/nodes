package org.nodes.util;

import java.io.Serializable;
import java.util.*;

import org.nodes.Global;

/**
 * Class representing a bitstring with simple getters and setters by index.
 * 
 * We use the correspondence 1=true and 0=false between bits and boolean values.
 * 
 * Note that a BitString cannot contain null elements. Adding them will cause a 
 * runtime exception to be thrown.
 * 
 * @author peter
 *
 */
public class BitString extends AbstractList<Boolean> implements Serializable
{

	private static final long serialVersionUID = 5693137870101684172L;
	protected byte[] array;
	protected int maxIndex = -1;
	
	/**
	 * 
	 */
	public BitString()
	{
		this(128);
	}
	
	/**
	 * @param capacity The initial capacity in bits.
	 */
	public BitString(int capacity)
	{
		int byteSize = (int)Math.ceil(capacity/8.0);
		array = new byte[byteSize];
	}
	
	/**
	 * Creates an empty britstring, to be filled by static methods.
	 * 
	 * @param empty
	 */
	private BitString(Object empty)
	{
	}
	
	/**
	 * Ensures that the array is large enough to accommodate a value at the 
	 * given index 
	 * 
	 * @param max
	 */
	private void ensureCapacity(int max)
	{
 		int byteSize = max/8 +1;
		if(byteSize <= array.length) 
			return;
		
		byte[] newArray = new byte[byteSize];
		
		for(int i = 0; i < array.length; i++)
			newArray[i] = array[i];
		
		array = newArray;
	}

	@Override
	public int size() 
	{
		return maxIndex+1;
	}
	
	@Override
	public boolean add(Boolean bit) {
		if(bit == null)
			throw new IllegalArgumentException("BitString cannot contain null elements");
		
		ensureCapacity(maxIndex + 1);
		maxIndex++;		
		set(maxIndex, bit);
	
		return true;
	}

	@Override
	public Boolean get(int index) {
		checkIndex(index);
		
		int whichByte = index/8,
		    whichBit  = index%8;
		
		byte b = array[whichByte];
		
		return bit(whichBit, b);
	}
	
	@Override
	public Boolean set(int index, Boolean bit) 
	{
		if(bit == null)
			throw new IllegalArgumentException("BitString cannot contain null elements");
		
		checkIndex(index);
		
		int whichByte = index/8,
		    whichBit  = index%8;
		
		byte b = array[whichByte];
		byte mask = mask(whichBit);
		
		Boolean old = bit(whichBit, b);
		
		if(bit)
			array[whichByte] = (byte)(b |  mask);
		else
			array[whichByte] = (byte)(b & ~mask);
		
		modCount++;
		return old;
	}
	
	private void checkIndex(int index)
	{
		if(index < 0 || index > maxIndex)
			throw new ArrayIndexOutOfBoundsException("Index ("+index+") must be in interval (0, "+maxIndex+")");
	}

	public String toString()
	{
		char[] ch = new char[this.size()];
		for(int i = 0; i < this.size(); i++) 
			ch[i] = this.get(i) ? '1': '0';
		
		return new String(ch);		
	}
	
	@Override
	public void clear()
	{
		for(int i : Series.series(array.length))
			array[i] = 0;
			
		maxIndex = 0;
	}

	/**
	 * Returns a representation of this bitstring as a byte array, the closest
	 * we can get to a string of actual system bits. 
	 *
	 * The last byte is not part of the bitstring, but indicates (encoded as a 
	 * java int cast to a byte) the number of bits the second-to last byte has 
	 * been padded to make the number of bits a multiple of eight.
	 * 
	 * @return  An array of (floor(this.size()) / 8 + 2) bytes containing the 
	 * 			bitstring, with sufficient additional information to reconstruct
	 * 			it.
	 */
	public byte[] byteArray()
	{
		byte[] out = new byte[array.length + 1];
		System.arraycopy(array, 0, out, 0, array.length);
		
		out[array.length] = (byte) padding();
		
		return out;
	}
	
	/**
	 * Returns the array backing this BitString. Use caution.
	 * 
	 * @return
	 */
	public byte[] rawData()
	{
		return array;
	}
	
	/**
	 * The number of bits required to pad this bitstring out to a multiple of 
	 * eight.
	 * @return
	 */
	protected int padding()
	{
		return (8 - size() % 8) % 8;		
	}
	
	/**
	 * The number of ones in this bitstring
	 * @return
	 */
	public int numOnes()
	{
		int sum = 0;
		
		for(boolean bit : this)
			if(bit) sum++;
		
		return sum;
	}
	
	/**
	 * The number of zeros in this bitstring
	 * @return
	 */
	public int numZeros()
	{
		int sum = 0;
		
		for(boolean bit : this)
			if(! bit) sum++;
		
		return sum;
	}


	/**
	 * Zero-pads this bitstring to a multiple of 16, and returns the result as 
	 * a list of integers
	 * @return
	 */
	public List<Integer> toIntegers()
	{
		int n = size();
		n = n%32 == 0 ? n : ((n/32)+1) * 32;
		
		List<Integer> integers = new ArrayList<Integer>(array.length + 1);
		for(int i = 0; i < array.length; i += 4)
		{
	        int next = (array(i) << 24)
	                + ((array(i+1) & 0xFF) << 16)
	                + ((array(i+2) & 0xFF) << 8)
	                +  (array(i+3) & 0xFF);
	        integers.add(next);
		}
		
		return integers;
	}
	
	private byte array(int i)
	{
		if(i < array.length)
			return array[i];
		return 0;
	}
	
	/**
	 * Increments this bitstring to the next in the canonical ordering. If this 
	 * bitstring consists of all 1s, all elements are set to 0 and one 0 is 
	 * added. The bit at index zero is taken as the least significant bit.
	 * 
	 * @return
	 */
	public void increment()
	{
		next(0);
	}
	
	private void next(int index)
	{
		if(index >= size())
		{
			add(false);
			zeroBack(size() - 1);
		} else if(! get(index))
		{
			set(index, true);
			zeroBack(index - 1);
		} else 
			next(index + 1);
	}
	
	/**
	 * Sets index and everything below to zero.
	 * @param index
	 */
	private void zeroBack(int index)
	{ 
		if(index < 0)
			return;
		
		set(index, false);
		
		zeroBack(index - 1);
	}
	
	public static byte mask(int index)
	{
		switch (index) {
			case 0: return 1;			
			case 1: return 2;
			case 2: return 4;
			case 3: return 8;
			case 4: return 16;
			case 5: return 32;
			case 6: return 64;
			case 7: return -128;
			default: throw new IndexOutOfBoundsException(index + "");
		}
	}
	
	public static boolean bit(int index, byte b)
	{
		byte mask = mask(index);
		
		return (mask & b) == mask; 
	}
	
	/**
	 * Creates a bitstring of the given length with only zero bits as elements
	 * @param size
	 * @return
	 */
	public static BitString zeros(int size)
	{
		BitString out = new BitString(size);
		out.maxIndex = size - 1;
		return out;
	}
	
	/**
	 * Creates a bitstring of the given length with only one-valued bits as elements
	 * @param size
	 * @return
	 */
	public static BitString ones(int size)
	{
		// TODO just make a zero bitstring and invert it. 
		BitString out = new BitString(size); 
		for(int i = 0; i < size; i++)
			out.add(true);
		
		return out;
	}	
	
	/**
	 * Creates a bitstring of the given length with random bits as elements
	 * @param size
	 * @return
	 */
	public static BitString random(int size)
	{
		BitString out = new BitString(size);
		out.maxIndex = size - 1;
		for(int i = 0; i < size; i++)
			out.set(i, Global.random().nextBoolean());
		
		return out;
	}	
	
	/**
	 * Creates a bitstring of the given length with random bits as elements
	 * 
	 * @param size
	 * @param probTrue The probability of a 'true' bit being entered
	 * @return
	 */
	public static BitString random(int size, double probTrue)
	{
		BitString out = new BitString(size);
		out.maxIndex = size - 1;
		for(int i = 0; i < size; i++)
			out.set(i, Global.random().nextDouble() < probTrue ? true : false);
		
		return out;
	}		
	
	public static BitString random(int size, int ones)
	{
		BitString out = BitString.zeros(size);
		
		for(int i : Functions.sample(ones, size))
			out.set(i, true);
		
		return out;
	}		
	
	public static String toString(byte b)
	{
		char[] ch = new char[8];
		for(int i = 0; i < 8; i++)
			ch[0] = bit(i, b) ? '1': '0';
		
		return new String(ch);
	}
	
	/**
	 * Parses a character sequence of 1's and 0's into the corresponding 
	 * bitstring
	 * 
	 * If the char sequence contains any character not 0, it is interpreted 
	 * as 1.
	 * 
	 * @param in
	 * @return
	 */
	public static BitString parse(CharSequence in)
	{
		BitString out = new BitString(in.length());
		for(int i = 0; i < in.length(); i++)
			out.add(in.charAt(i) == '0' ? false : true);
		
		return out;
	}
	
	/**
	 * Returns a collection containing all bitstrings of the given size
	 * (generated on the fly)
	 * 
	 * @param size
	 * @return
	 */
	public static Collection<BitString> all(int size)
	{
		return new BSCollection(size);
	}
	
	
	private static class BSCollection extends AbstractCollection<BitString>
	{
		private int numBits;

		
		public BSCollection(int numBits)
		{
			this.numBits = numBits;
		}

		@Override
		public Iterator<BitString> iterator()
		{
			return new BSIterator();
		}

		@Override
		public int size()
		{
			return (int) Math.pow(2, numBits);
		}
		
		private class BSIterator implements Iterator<BitString>
		{
			BitString next = BitString.zeros(numBits);
			
			@Override
			public boolean hasNext()
			{
				return next.size() == numBits;
			}

			@Override
			public BitString next()
			{
				if(! hasNext())
					throw new NoSuchElementException();
				
				BitString out = copy(next);
				
				next.increment();
				
				return out;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		}
		
	}
	
	public static BitString copy(BitString in)
	{
		BitString copy = new BitString(null);
		copy.array = in.array.clone();
		copy.maxIndex = in.maxIndex;
		
		return copy;
	}
}

