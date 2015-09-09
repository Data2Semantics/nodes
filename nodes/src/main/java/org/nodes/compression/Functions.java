package org.nodes.compression;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Series.series;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.draw.Draw;
import org.nodes.util.BitString;
import org.nodes.util.Pair;
import org.nodes.util.Series;

public class Functions
{

	/**
	 * 2 log
	 * @deprecated Use the one in org.nodes.util.Functions
	 */
	public static double log2(double x)
	{
		return Math.log10(x) / Math.log10(2.0);
	}
	
	public static double prefix(int n)
	{
		double prob = 1.0/ ((n + 1.0)*(n + 2.0));
		return - org.nodes.util.Functions.log2(prob);
	}

	/**
	 * The cost of storing the given value in prefix coding
	 * 
	 * @param bits
	 * @return
	 */
	public static double prefixVit(int value)
	{

		return prefixVit(value, 10);
	}

	public static int prefixVit(int value, int d)
	{
		if (d == 0)
			return 2 * length(value) + 1;

		return length(value) + length(prefixVit(length(value), d - 1));
	}

	/**
	 * The length of the given value in the canonical bitstring representation
	 * of integers. (0 = "", 1="0", 2="1", 3="00", etc).
	 * 
	 * @param in
	 * @return
	 */
	public static int length(int in)
	{
		if (in == 0)
			return 0;

		return (int) Math.ceil(log2(in + 1));
	}

	public static <L> BitString toBits(UGraph<L> graph)
	{
		return toBits(graph, Series.series(graph.size()));
	}

	public static <L> BitString toBits(UGraph<L> graph, List<Integer> order)
	{
		int n = graph.size();
		BitString string = BitString.zeros((n * n + n) / 2);

		for (ULink<L> link : graph.links())
		{
			int i = order.get(link.first().index());
			int j = order.get(link.second().index());

			if (j > i)
			{
				int t = j;
				j = i;
				i = t;
			}

			int rowStart = (i * (i + 1)) / 2;
			int index = rowStart + j;

			string.set(index, true);
		}

		return string;
	}

	public static <L> BitString toBits(DGraph<L> graph)
	{
		return toBits(graph, Series.series(graph.size()));
	}

	public static <L> BitString toBits(DGraph<L> graph, List<Integer> order)
	{
		int n = graph.size();
		BitString string = BitString.zeros(n * n);

		for (DLink<L> link : graph.links())
		{
			int i = order.get(link.first().index());
			int j = order.get(link.second().index());

			int rowStart = i * n;
			int index = rowStart + j;
			try
			{
				string.set(index, true);
			} catch (ArrayIndexOutOfBoundsException e)
			{
				System.out.println(n + " " + i + " " + j);
				throw e;
			}
		}

		return string;
	}

	public static <L> void toBits(OutputStream stream, UGraph<L> graph)
			throws IOException
	{
		toBits(stream, graph, Series.series(graph.size()));
	}

	public static <L> void toBits(OutputStream stream, UGraph<L> graph,
			List<Integer> order) throws IOException
	{
		int BUFFER_SIZE = 128;

		List<Integer> inv = Draw.inverse(order);

		BitString buffer = new BitString(BUFFER_SIZE);

		int n = graph.size();
		for (int i : series(n))
			for (int j : series(i + 1, n))
			{
				int ii = inv.get(i), jj = inv.get(j);

				boolean bit = graph.nodes().get(ii)
						.connected(graph.nodes().get(jj));
				buffer.add(bit);

				if (buffer.size() == BUFFER_SIZE)
				{
					stream.write(buffer.rawData());
					buffer.clear();
				}
			}

		stream.write(buffer.byteArray());
		stream.flush();
	}

	public static <L> void toBits(OutputStream stream, DGraph<L> graph)
			throws IOException
	{
		toBits(stream, graph, Series.series(graph.size()));
	}

	public static <L> void toBits(OutputStream stream, DGraph<L> graph,
			List<Integer> order) throws IOException
	{
		int BUFFER_SIZE = 128;

		List<Integer> inv = Draw.inverse(order);

		BitString buffer = new BitString(BUFFER_SIZE);

		int n = graph.size();
		for (int i : series(n))
			for (int j : series(n))
			{
				int ii = inv.get(i), jj = inv.get(j);

				boolean bit = graph.nodes().get(ii)
						.connected(graph.nodes().get(jj));
				buffer.add(bit);

				if (buffer.size() == BUFFER_SIZE)
				{
					stream.write(buffer.rawData());
					buffer.clear();
				}
			}

		stream.write(buffer.byteArray());
		stream.flush();
	}

	public static int toIndexUndirected(int i, int j, boolean self)
	{
		int rowStart = self ? (i * (i + 1)) / 2 : (i * (i - 1)) / 2;
		return rowStart + j;

	}

	public static Pair<Integer, Integer> toPairUndirected(int index,
			boolean self)
	{
		double iDouble;
		int i, j;

		if (self)
		{
			iDouble = -0.5 + 0.5 * Math.sqrt(1.0 + 8.0 * index);
			i = (int) Math.floor(iDouble);
			j = index - ((i * (i + 1)) / 2);
		} else
		{
			iDouble = 0.5 + 0.5 * Math.sqrt(1.0 + 8.0 * index);
			i = (int) Math.floor(iDouble);
			j = index - ((i * (i - 1)) / 2);
		}

		return new Pair<Integer, Integer>(i, j);
	}

	public static class NaturalComparator<T extends Comparable<? super T>>
			implements Comparator<T>
	{

		@Override
		public int compare(T first, T second)
		{
			return first.compareTo(second);
		}
	}
	
	/**
	 * Calculates the set overlap between two sets.
	 * 
	 * @param a the first set
	 * @param b the second set
	 * @return The the number of elements present in both sets. 
	 */
	public static int overlap(Collection<?> a, Collection<?> b)
	{
		Collection<?> smallest;
		Collection<?> largest;
		if(a.size() >= b.size())
		{
			largest = a;
			smallest = b;
		}else
		{
			largest = b;
			smallest = a;
		}
		
		int overlap = 0;
		Iterator<?> it = smallest.iterator();
		while(it.hasNext())
		{
			if(largest.contains(it.next()))
				overlap++;			
		}
		
		return overlap;
	}
	
	private static long ticTime = -1;
	public static void tic()
	{
		ticTime = System.currentTimeMillis();
	}
	
	/** 
	 * Returns the number of seconds since tic was last called. <br/>
	 * <br/>
	 * Not thread-safe (at all).
	 * 
	 * @return A double representing the number of seconds since the last call 
	 *         to tic(). 
	 */
	public static double toc()
	{
		if(ticTime  < 0)
			throw new IllegalStateException("Tic has not been called yet");
		return (System.currentTimeMillis() - ticTime)/1000.0;
	}

}
