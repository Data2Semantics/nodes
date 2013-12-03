package org.nodes.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.UTGraph;
import org.nodes.UTLink;
import org.nodes.UTNode;

public class MatrixZIPCompressor<N> extends AbstractGraphCompressor<N>
{

	@Override
	public double structureBits(Graph<N> graph, List<Integer> order)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream goz = new GZIPOutputStream(baos, 4096);
			
			if(graph instanceof UGraph<?>)
				Functions.toBits(goz, (UGraph<N>)graph, order);
			else if(graph instanceof DGraph<?>)
				Functions.toBits(goz, (DGraph<N>)graph, order);
			else
				throw new IllegalArgumentException("Can only handle graphs of type UGraph or DGraph");
			
			goz.finish();
			goz.close();
			
			return baos.size() * 8 + 1; // the +1 is there to distinguish between U and D
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}		
	}

}
