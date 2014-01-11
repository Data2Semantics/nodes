package org.nodes.rdf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.nodes.DNode;
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
	private Instances iaSearchDirected, haSearchDirected, depthSearchDirected;

	/**
	 * 
	 * @param graph The graph. Note that this graph should have the target relations removed.
	 * @param instances The training set
	 * @param instanceSize
	 * @param maxDepth
	 */
	public InstanceHelper(DTGraph<String, String> graph, Classified<? extends Node<String>> instances, int instanceSize, int maxDepth)
	{
		this.graph = graph;
		this.instanceSize = instanceSize;
		this.maxDepth = maxDepth;	
		
		ia = new InformedAvoidance(graph, instances, maxDepth);
		ha = new HubAvoidance(graph, instances, maxDepth);
		
		iaSearch = new FlatInstances(graph, instanceSize, maxDepth, ia);
		haSearch = new FlatInstances(graph, instanceSize, maxDepth, ha);
		depthSearch = new FlatInstances(graph, instanceSize, maxDepth, new DepthScorer());
		
		iaSearchDirected = new FlatInstances(graph, instanceSize, maxDepth, ia, true);
		haSearchDirected = new FlatInstances(graph, instanceSize, maxDepth, ha, true);
		depthSearchDirected = new FlatInstances(graph, instanceSize, maxDepth, new DepthScorer(), true);
	}
	
	public List<DTNode<String, String>> instanceByDepth(DNode<String> instanceNode)
	{
		return instanceByDepth(instanceNode, false);
	}
	
	public List<DTNode<String, String>> instanceInformed(DNode<String> instanceNode)
	{
		return instanceInformed(instanceNode, false);
	}
	
	public List<DTNode<String, String>> instanceUninformed(DNode<String> instanceNode)
	{
		return instanceUninformed(instanceNode, false);
	}
	
	public List<DTNode<String, String>> instanceByDepth(DNode<String> instanceNode, boolean directed)
	{
		return directed ? depthSearchDirected.instance(instanceNode) : depthSearch.instance(instanceNode);
	}
	
	public List<DTNode<String, String>> instanceInformed(DNode<String> instanceNode, boolean directed)
	{
		return directed ? iaSearchDirected.instance(instanceNode) : iaSearch.instance(instanceNode);
	}
	
	public List<DTNode<String, String>> instanceUninformed(DNode<String> instanceNode, boolean directed)
	{
		return directed ? haSearchDirected.instance(instanceNode) : haSearch.instance(instanceNode);
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
		return getInstances(dataset, instanceNodes, classes, method, instanceSize, maxDepth);
		
	}
	
	public static Classified<DTGraph<String, String>> getInstances(DTGraph<String, String> dataset, List<DTNode<String, String>> instanceNodes, List<? extends Number> classes, Method method, int instanceSize, int maxDepth, boolean directed)
	{
		List<Integer> clss = new ArrayList<Integer>(classes.size());
		for(Number number : classes)
			clss.add((int)(number.doubleValue()));
		
		InstanceHelper helper = new InstanceHelper(dataset, Classification.combine(instanceNodes, clss), instanceSize, maxDepth);
		
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
