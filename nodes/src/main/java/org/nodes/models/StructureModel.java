package org.nodes.models;

import org.nodes.Graph;

/**
 * A StructureModel models only the structure of the graph. The labels and tags 
 * are ignored.
 * 
 * @author Peter
 *
 */
public interface StructureModel<G extends Graph<?>>
{
	public double codelength(G graph);
}
