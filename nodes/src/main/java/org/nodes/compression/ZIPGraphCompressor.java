package org.nodes.compression;

import static nl.peterbloem.kit.Series.series;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

import org.nodes.Graph;
import org.nodes.UTGraph;
import org.nodes.UTLink;
import org.nodes.UTNode;
import org.nodes.util.Compressor;
import org.nodes.util.GZIPCompressor;

import nl.peterbloem.kit.BitString;
import nl.peterbloem.kit.Series;

/**
 * 
 * @author Peter
 *
 * @param <L>
 * @param <T>
 */
public class ZIPGraphCompressor<L, T> implements Compressor<UTGraph<L, T>>
{
	private int bufferSize = 512;
	@Override
	public double compressedSize(Object... objects)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream goz = new GZIPOutputStream(baos, bufferSize);
			ObjectOutputStream oos = new ObjectOutputStream(goz);

			for(Object object : objects)
			{
				if(! (object instanceof UTGraph<?, ?>))
					oos.writeObject(object);
				else {
					UTGraph<?, ?> graph = (UTGraph<?, ?>)object;
					
					oos.writeObject(Functions.toBits(graph));
					for(UTNode<?, ?> node : graph.nodes())
						oos.writeObject(node.label());
					for(UTLink<?, ?> link : graph.links())
						oos.writeObject(link.tag());
				}
			}
			
			oos.close();
			goz.finish();
			goz.close();
			
			return baos.size();
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}		
	}

	@Override
	public double ratio(Object... object)
	{
		throw new UnsupportedOperationException();
	}
}
