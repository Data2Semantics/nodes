package org.nodes.random;

/**
 * A source is an object that generates a sequence of objects of the specificed 
 * type. 
 * </p><p>
 * Sources can be used, for example, in algorithms that generate random graphs, 
 * to provide a labeling for the nodes. 
 * </p>
 * 
 * @author Peter
 *
 * @param <T>
 */
public interface Source<T>
{
	/**
	 * Produce the next element in the sequence defined by this source.
	 * @return
	 */
	public T next();
}
