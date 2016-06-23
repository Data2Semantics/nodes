package org.nodes;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cern.colt.list.AbstractList;
import nl.peterbloem.kit.Series;

/**
 * @author Peter
 *
 */
public class Subgraph
{
	/**
	 * Generates a subgraph containing the given nodes, 
	 * and any links existing between them in the original graph.
	 * 
	 * @param graph
	 * @param nodes
	 * @return
	 */
	public static <L, T> UTGraph<L, T> utSubgraph(UTGraph<L, T> graph, Collection<UTNode<L, T>> nodes)
	{
		List<UTNode<L, T>> list = new ArrayList<UTNode<L,T>>(nodes);
		
		UTGraph<L, T> out = new MapUTGraph<L, T>();
		for(UTNode<L, T> node : list)
			out.add(node.label());
		
		for(int i : series(nodes.size()))
			for(int j : series(i, nodes.size()))
				for(TLink<L, T> link : list.get(i).links(list.get(j)))
					out.get(i).connect(out.get(j), link.tag());
		
		return out;
	}
	
	/**
	 * Generates a subgraph containing the nodes at the provided indices, 
	 * and any links existing between them in the original graph.
	 * 
	 * @param graph
	 * @param nodes
	 * @return
	 */
	public static <L, T> UTGraph<L, T> utSubgraphIndices(UTGraph<L, T> graph, Collection<Integer> nodes)
	{
		List<UTNode<L, T>> list = new ArrayList<UTNode<L,T>>();
		for(int i : nodes)
			list.add(graph.nodes().get(i));
		
		return utSubgraph(graph, list);
	}
	
	/**
	 * Generates a subgraph containing the given nodes, 
	 * and any links existing between them in the original graph.
	 * 
	 * @param graph
	 * @param nodes
	 * @return
	 */
	public static <L, T> DTGraph<L, T> dtSubgraph(DTGraph<L, T> graph, Collection<DTNode<L, T>> nodes)
	{
		List<DTNode<L, T>> list = new ArrayList<DTNode<L,T>>(nodes);
		
		DTGraph<L, T> out = new MapDTGraph<L, T>();
		for(DTNode<L, T> node : list)
			out.add(node.label());
		
		for(int i : series(nodes.size()))
			for(int j : series(nodes.size()))
				for(DTLink<L, T> link : list.get(i).linksOut(list.get(j)))
					out.get(i).connect(out.get(j), link.tag());

		return out;
	}
	
	/**
	 * Generates a subgraph containing the nodes at the provided indices, 
	 * and any links existing between them in the original graph.
	 * 
	 * @param graph
	 * @param nodes
	 * @return
	 */
	public static <L, T> DTGraph<L, T> dtSubgraphIndices(DTGraph<L, T> graph, Collection<Integer> nodes)
	{
		List<DTNode<L, T>> list = new ArrayList<DTNode<L,T>>();
		for(int i : nodes)
			list.add(graph.get(i));
		
		return dtSubgraph(graph, list);
	}
	
	public static <L> DGraph<L> dSubgraph(DGraph<L> graph, Collection<DNode<L>> nodes)
	{
		List<DNode<L>> list = new ArrayList<DNode<L>>(nodes);
		
		DGraph<L> out = new MapDTGraph<L, String>();
		for(Node<L> node : list)
			out.add(node.label());
		
		for(int i : series(nodes.size()))
			for(int j : series(nodes.size()))
				for(DLink<L> link : list.get(i).linksOut(list.get(j)))
					out.get(i).connect(out.get(j));
		
		return out;
	}
	
	public static <L> DGraph<L> dSubgraphIndices(DGraph<L> graph, Collection<Integer> nodes)
	{
		List<DNode<L>> list = new ArrayList<DNode<L>>();
		for(int i : nodes)
			list.add(graph.nodes().get(i));
		
		return dSubgraph(graph, list);
	}

	public static <L> Graph<L> subgraph(Graph<L> graph, Collection<Node<L>> nodes)
	{
		final List<Node<L>> nodeList =
				nodes instanceof List
				? (List<Node<L>>) nodes 
				: new ArrayList<Node<L>>(nodes);
		
		List<Integer> indices = new java.util.AbstractList<Integer>()
		{
			@Override
			public Integer get(int index)
			{
				return nodeList.get(index).index();
			}

			@Override
			public int size()
			{
				return nodeList.size(); 
			}
		};
		
		return subgraphIndices(graph, indices); 
	}
	
	
	public static <L> Graph<L> subgraphIndices(Graph<L> graph, Collection<Integer> nodes)
	{
		if(graph instanceof DGraph)
			return dSubgraphIndices((DGraph<L>) graph,  nodes);
		if(graph instanceof UGraph)
			return uSubgraphIndices((UGraph<L>) graph, nodes);
		if(graph instanceof DTGraph)
			return dtSubgraphIndices((DTGraph<L, ?>) graph, nodes);
		if(graph instanceof UTGraph)
			return utSubgraphIndices((UTGraph<L, ?>) graph, nodes);

		throw new IllegalArgumentException("Graph type ("+graph.getClass()+") not recognized.");
	}
	
	public static <L> UGraph<L> uSubgraph(UGraph<L> graph, Collection<UNode<L>> nodes)
	{
		List<UNode<L>> list = new ArrayList<UNode<L>>(nodes);
		
		UGraph<L> out = new MapUTGraph<L, String>();
		for(UNode<L> node : list)
			out.add(node.label());
			
		for(int i : series(nodes.size()))
			for(int j : series(i, nodes.size()))
				for(ULink<L> link : list.get(i).links(list.get(j)))
					out.get(i).connect(out.get(j));
		
		return out;
	}
	
	public static <L> UGraph<L> uSubgraphIndices(UGraph<L> graph, Collection<Integer> nodes)
	{
		List<UNode<L>> list = new ArrayList<UNode<L>>();
		for(int i : nodes)
			list.add(graph.nodes().get(i));
		
		return uSubgraph(graph, list);
	}


}
