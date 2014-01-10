package org.nodes.rdf;

import org.nodes.DTNode;
import org.nodes.Node;


/**
 * Wrapper class for node and depth
 * @author Peter
 *
 */
public class Token
{
	private Node<String> node;
	private int depth;
	
	public Token(Node<String> node, int depth)
	{
		this.node = node;
		this.depth = depth;
	}

	public Node<String> node()
	{
		return node;
	}

	public int depth()
	{
		return depth;
	}
}