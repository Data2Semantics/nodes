package org.nodes.rdf;

import java.util.List;

import org.nodes.DTNode;
import org.nodes.Node;

/**
 * Interface for basic instance extraction algorithms
 * 
 * @author Peter
 *
 */
public interface Instances
{
	
	/**
	 * Extracts a set of nodes representing the given instanceNode 
	 * 
	 * @param instanceNode
	 * @return
	 */
	public List<DTNode<String, String>> instance(Node<String> instanceNode);

}
