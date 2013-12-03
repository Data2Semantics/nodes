package org.nodes.util;

/**
 * A class is metrizable, when there is a natural distance function over pairs
 * of instances of the class.
 * 
 * If no single distance function is natural enough to be used for the 
 * implementation of this function, a class implementing the Distance <T>
 * interface can be created and passed to any function that requires a 
 * definition of distance 
 * 
 * This structure is intended to be analogous to Java's own notions of 
 * Comparable and Comparator 
 */

public interface Metrizable<T>
{
	/**
	 * Returns the distance between this object and another.
	 * 
	 * @param other The object to which to return the distance
	 */
	public double distance(T other);
}
