package org.nodes.rdf;

import java.util.ArrayList;
import java.util.List;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.classification.Classification;
import org.nodes.classification.Classified;

/**
 * Utility class for extracting instances from RDF graphs
 * @author Peter
 *
 */
public class InstanceHelper
{
	
	public static enum Method {INFORMED, UNINFORMED, DEPTH};
	
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

	/**
	 * 
	 * @param dataset
	 * @param instances
	 * @param classes List of number that represent classes. Casting the result of doubleValue() to int should retain the correct value.
	 * @return
	 */
	public static Classified<DTGraph<String, String>> getInstances(DTGraph<String, String> dataset, List<DTNode<String, String>> instanceNodes, List<? extends Number> classes, Method method, int instanceSize, int maxDepth)
	{
		List<Integer> clss = new ArrayList<Integer>(classes.size());
		for(Number number : classes)
			clss.add((int)(number.doubleValue()));
		
		// Dirty, dirty trick
		Object object = instanceNodes;
		@SuppressWarnings("unchecked")
		List<Node<String>> casted = (List<Node<String>>)object; 
		
		InstanceHelper helper = new InstanceHelper(dataset, Classification.combine(casted, clss), instanceSize, maxDepth);
		
		List<DTGraph<String, String>> instances = new ArrayList<DTGraph<String,String>>();

		for(DTNode<String, String> instanceNode : instanceNodes)
		{
			List<DTNode<String, String>> instance = null;
			switch (method)
			{
			case DEPTH:
				instance = helper.instanceByDepth(instanceNode); 
				break;
			case INFORMED:
				instance = helper.instanceInformed(instanceNode);
				break;
			case UNINFORMED:
				instance = helper.instanceUninformed(instanceNode);
				break;
			}
			
			instances.add(Subgraph.dtSubgraph(dataset, instance));
		}
		
		return Classification.combine(instances, clss);
	}
	
	
}
