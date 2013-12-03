package org.nodes.draw;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a function from a Euclidean space onto itself.
 * 
 * @author Peter
 *
 */
public interface Map extends Serializable {
	
	public Point map(Point in);
	
	/**
	 * Returns whether this map has an inverse map 
	 */
	public boolean invertible();
	
	/**
	 * Returns the inverse of this map
	 * 
	 * @throws RuntimeException if !isInvertible() 
	 */
	public Map inverse();
	
	public int dimension();
	
	public List<Point> map(List<Point> points);
	
	/**
	 * <p>
	 * Returns a map whose function is equal to applying the other map and then 
	 * this map.
	 * </p><p>
	 * Thus, if functions F(x) and G(x) are represented by Map objects f and g, 
	 * then the function F(G(x)) is represented by f.compose(g).
	 * </p>
	 * 
	 * @param other
	 * @return
	 */
	public Map compose(Map other);
}
