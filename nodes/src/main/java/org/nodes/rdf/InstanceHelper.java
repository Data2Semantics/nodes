package org.nodes.rdf;

import java.util.List;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;
import org.nodes.classification.Classified;

/**
 * Utility class for extracting instances from RDF graphs
 * @author Peter
 *
 */
public class InstanceHelper
{
	private DTGraph<String, String> graph;
	private int instanceSize, maxDepth;
	
	private InformedAvoidance ia;
	private HubAvoidance ha;
	
	private Instances iaSearch, haSearch, depthSearch;
	
	/**
	 * 
	 * @param graph The graph. Note that this graph should have the target relations removed.
	 * @param instances The training set
	 * @param instanceSize
	 * @param maxDepth
	 */
	public InstanceHelper(DTGraph<String, String> graph, Classified<Node<String>> instances, int instanceSize, int maxDepth)
	{
		this.graph = graph;
		this.instanceSize = instanceSize;
		this.maxDepth = maxDepth;	
		
		ia = new InformedAvoidance(graph, instances, maxDepth);
		ha = new HubAvoidance(graph, instances, maxDepth);
		
		iaSearch = new FlatInstances(graph, instanceSize, maxDepth, ia);
		haSearch = new FlatInstances(graph, instanceSize, maxDepth, ha);
		depthSearch = new FlatInstances(graph, instanceSize, maxDepth, new DepthScorer());
	}
	
	public List<DTNode<String, String>> instanceByDepth(Node<String> instanceNode)
	{
		return depthSearch.instance(instanceNode);
	}
	
	public List<DTNode<String, String>> instanceInformed(Node<String> instanceNode)
	{
		return iaSearch.instance(instanceNode);
	}
	
	public List<DTNode<String, String>> instanceUninformed(Node<String> instanceNode)
	{
		return haSearch.instance(instanceNode);
	}	


	
	
}
