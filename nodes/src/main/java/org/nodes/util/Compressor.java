package org.nodes.util;

/**
 * A compressor defines a compression of a class of objects.
 * 
 * @author peter
 *
 * @param <T>
 */
public interface Compressor<T> {
	
	/**
	 * The absolute length in bits of the compressed size
	 * @param object
	 * @return
	 */
	public double compressedSize(Object... object);
	
	/**
	 * The ratio of the compressed sequence relative to the uncompressed 
	 * sequence. How the uncompressed sequence is defined is left up to the
	 * specific compressor implementation. 
	 * 
	 * @param object
	 * @return
	 */
	public double ratio(Object... object);
}
