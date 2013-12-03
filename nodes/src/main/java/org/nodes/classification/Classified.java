package org.nodes.classification;

import java.util.Collection;
import java.util.List;

/**<p>
 * Represents a list of items with associated class </p><p>
 * 
 * The normal functions for adding items become unsupported and are replaced with 
 * versions that include a class parameter.</p><p>
 * 
 * See {Classification.combine()} for the standard way of creating this type of data.</p>
 * 
 * @author Peter
 *
 * @param <P>
 */
public interface Classified<P> extends List<P>
{

	public int cls(int i);
	
	public List<Integer> classes();
	
	public int numClasses();	
	
	/**
	 * Optional operation 
	 * 
	 * @param item
	 * @param cls
	 */
	public boolean add(P item, int cls);
	
	public boolean add(int index, P item, int cls);

	
	public boolean addAll(Collection<? extends P> c, int cls);
	
	public boolean addAll(int index, Collection<? extends P> c, int cls);
	
	/**
	 * 
	 * @param data
	 * @param classes
	 * @return
	 */
	public P set(int i, P item, int cls);
	
	public Classified<P> subClassified(int from, int to);
	
	/**
	 * Returns all Ps in this classified list with the given class
	 * 
	 * @param cls
	 * @return
	 */
	public List<P> points(int cls);
	
	/**
	 * Sets the maximum class to the given value. This function is useful when 
	 * a classified set is required which contains no occurences of a class that
	 * should nevertheless be considered.
	 * 
	 * For instance, when a dataset with few instance for a given class is split 
	 * into a test and training set, it's conceivable that one contains no 
	 * instances of the class.
	 * 
	 * If the given value is lower than the current max class, an exception is 
	 * thrown.
	 * 
	 * @param max
	 */
	public void setMaxClass(int max);

	/**
	 * Sets the given class to the given data item.
	 * @param i
	 * @param cls
	 */
	public void setClass(int i, int cls);
}
