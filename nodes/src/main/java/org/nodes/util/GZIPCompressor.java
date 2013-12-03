package org.nodes.util;

import java.io.*;
import java.util.zip.GZIPOutputStream;


/**
 * 
 * NOTE: We can't check whether the type is Serializable at compile time due to 
 * the use of varargs (this may be fixed in java7).  
 * 
 * @author peter
 *
 * @param <T>
 */
public class GZIPCompressor<T> implements Compressor<T> {
	
	private int bufferSize = 256;
	
	public GZIPCompressor() 
	{
	}
	
	public GZIPCompressor(int bufferSize) 
	{
		this.bufferSize = bufferSize;
	}

	@Override
	public double compressedSize(Object... objects) {
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream goz = new GZIPOutputStream(baos, bufferSize);
			ObjectOutputStream oos = new ObjectOutputStream(goz);

			for(Object object : objects)
				oos.writeObject(object);
			
			oos.close();
			goz.finish();
			goz.close();
			
			return baos.size();
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public double plainSize(Object... objects) {
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			for(Object object : objects)
				oos.writeObject(object);
			oos.close();
			
			return baos.size();
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}	
	
	@Override
	public double ratio(Object... objects) {
		double compressed = compressedSize(objects),
			       plain = plainSize(objects);
		
		return compressed / plain;
	}		
	
	public static double ratio(boolean[] array) {
		return ratio(array, 1);
	}
	
	public static double ratio(boolean[] array, int bufferSize) 
	{
		try
		{
			ByteArrayOutputStream rbaos = new ByteArrayOutputStream();
			ObjectOutputStream roos = new ObjectOutputStream(rbaos);

			for(boolean bool : array)
				roos.writeObject(bool);
			roos.close();
			
			double regSize = rbaos.size();			
					
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream goz = new GZIPOutputStream(baos, bufferSize);
			ObjectOutputStream oos = new ObjectOutputStream(goz);

			for(boolean bool : array)
				oos.writeObject(bool);
			oos.close();
			
			double compSize = baos.size();
			
			return compSize/regSize;
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static double ratio(byte[] array) {
		return ratio(array, 1);
	}	
	
	public static double ratio(byte[] array, int bufferSize) 
	{
		try
		{
			double regSize = array.length;			
					
			ByteArrayOutputStream outer = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(outer, bufferSize);
			
			for(byte b : array)
				gzip.write(b);
			
			gzip.close();
			
			double compSize = outer.size();
			
			return compSize/regSize;
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
