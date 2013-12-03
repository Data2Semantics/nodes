package org.nodes.boxing;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * A boxing algorithm divides the nodes of a graph into small clusters such that
 * the diameter of each cluster is less than a parameter l. The objective of the 
 * algorithm is to minimize the number of boxes.
 * </p>
 * 
 * @author Peter
 *
 * @param <V>
 */
public interface BoxingAlgorithm<L>
{
	/**
	 * Generates a boxing of the graph. Depending on the algorithm, successive
	 * calls to this method may yield different boxings (with different numbers
	 * of boxes). 
	 * @return
	 */
	public Boxing<L> box(int l);

}
