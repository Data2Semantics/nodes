package org.nodes.random;

/**
 * Some basic sources to fit most uses.
 * @author Peter
 *
 */
public class Sources
{
	
	public static <T> Source<T> nulls()
	{
		return new NullSource();
	}
	
	public static class NullSource<T> implements Source<T>
	{
		@Override
		public T next()
		{
			return null;
		}	
	}
	
	public static Source<Integer> integers()
	{
		return new IntegerSource();
	}
	
	public static class IntegerSource implements Source<Integer>
	{
		private int last = -1;
		
		@Override
		public Integer next()
		{
			return last ++;
		}
	}
	
	public static Source<String> strings()
	{
		return new StringSource();
	}

	public static class StringSource implements Source<String>
	{
		private int last = -1;
		
		@Override
		public String next()
		{
			return " " + (last ++);
		}
	}
	
}
