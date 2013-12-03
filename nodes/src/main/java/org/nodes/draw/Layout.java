package org.nodes.draw;

import org.nodes.Node;

public interface Layout<L>
{

	/**
	 * Returns a 2D point representing the coordinates for the given point.
	 * the layout algorithm should strive, where possible, to place nodes in the 
	 * bi-unit square (within the range -1 to 1 along each dimension).
	 * 
	 * @param node
	 * @return
	 */
	public Point point(Node<L> node);
}
