package org.nodes.compression;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;
import org.nodes.Global;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.random.RandomGraphs;
import org.nodes.util.BitString;
import org.nodes.util.Pair;
import org.nodes.util.Series;

public class FunctionsTest
{

	@Test
	public void testToBitsOutputStreamUGraphOfL() throws IOException
	{
		UGraph<String> graph = RandomGraphs.random(25, 50);
		
		BitString string = Functions.toBits(graph);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		GZIPOutputStream out = new GZIPOutputStream(os);
		out.write(string.byteArray());
		out.finish();
		
		// System.out.println(os.size());
		
	}

	@Test
	public void testIndex()
	{
		Global.randomSeed();
		
		for(int t : Series.series(10000000))
		{
			int index = Global.random().nextInt(100);
		
			Pair<Integer, Integer> ij = Functions.toPairUndirected(index, true);
			assertEquals(index, Functions.toIndexUndirected(ij.first(), ij.second(), true));
			
			int i = Global.random().nextInt(100), j = Global.random().nextInt(i + 1);
			
			index = Functions.toIndexUndirected(i, j, true);
			ij = Functions.toPairUndirected(index, true);
			
			assertEquals(i, (int)ij.first());
			assertEquals(j, (int)ij.second());
		}
	}
}
