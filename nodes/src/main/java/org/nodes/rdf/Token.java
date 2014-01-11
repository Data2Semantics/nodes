package org.nodes.rdf;

import org.nodes.DNode;
import org.nodes.DTNode;
import org.nodes.Node;


/**
 * Wrapper class for node and depth
 * @author Peter
 *
 */
public class Token
{
	private DNode<String> node;
	private int depth;
	
	public Token(DNode<String> node, int depth)
	{
		this.node = node;
		this.depth = depth;
	}

	public DNode<String> node()
	{
		return node;
	}

	public int depth()
	{
		return depth;
	}
}