package org.nodes.boxing;

import static org.nodes.util.Series.series;

import java.util.AbstractList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nodes.Graph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.algorithms.FloydWarshall;
import org.nodes.util.Series;

/**
 * A clustering of a graph into boxes. 
 * 
 * @author Peter
 *
 */
public class Boxing<L> extends AbstractList<Set<Node<L>>>
{
	private List<Set<Node<L>>> base;
	private Graph<L> graph;
	
	// private UnweightedShortestPath<V, E> usp;
	
	public Boxing(List<Set<Node<L>>> base, Graph<L> graph)
	{
		this.base = base;
		this.graph = graph;
		
		// usp = new UnweightedShortestPath<V, E>(graph);
	}

	/**
	 * Returns all vertices that have not been assigned some box.
	 * @param boxing
	 * @param graph
	 * @return
	 */
	public Set<Node<L>> uncovered()
	{
		Set<Node<L>> vertices = new HashSet<Node<L>>(graph.nodes());
		
		Set<Node<L>> covered = new HashSet<Node<L>>();
		for(Set<Node<L>> box : this)
			covered.addAll(box);
		
		vertices.removeAll(covered);
		return vertices;
	}
	
	/**
	 * Returns all vertices that have been assigned multiple boxes.
	 * @param boxing
	 * @param graph
	 * @return
	 */
	public Set<Node<L>> overCovered()
	{
		Set<Node<L>> vertices = new HashSet<Node<L>>();
		
		int n = size();
		for(int i : series(n))
			for(int j : series(i + 1, n))
			{
				Set<Node<L>> copy = new HashSet<Node<L>>();
				copy.addAll(get(i));
				
				copy.retainAll(get(j));
				vertices.addAll(copy);
			}
				
		return vertices;
	}
	
	public int maxSize()
	{
		int max = Integer.MIN_VALUE;
		for(Set<Node<L>> box : base)
			max = Math.max(max,  box.size());
		
		return max;
	}
	
	public int maxDistance()
	{
		int max = Integer.MIN_VALUE;
		for(int i : Series.series(base.size()) )
		{
			int boxMax = boxMax(i);
			max = Math.max(max, boxMax);
		}
		
		return max;
	}
	
	public int boxMax(int i)
	{ 
		Graph<L> sub = Subgraph.subgraph(graph, base.get(i));
		FloydWarshall<L> fw = new FloydWarshall<L>(sub);
		
		return (int)fw.diameter();
	}

	@Override
	public Set<Node<L>> get(int index)
	{
		return base.get(index);
	}

	@Override
	public int size()
	{	
		return base.size();
	}
	
	/**
	 * <p>
	 * Constructs the graph with boxes as vertices, and an edge between two 
	 * vertices if there is at least one edge between member from both boxes in
	 * the original graph.  
	 * </p><p>
	 * The integer labels of the vertices of the new graph match the indices of 
	 * the boxes in this boxing. The edge labels have no particular meaning.
	 * </p> 
	 * @return
	 */
	public Graph<Integer> postGraph()
	{
		Graph<Integer> post = new MapUTGraph<Integer, Integer>();
		
		for(int i : series(size()))
			post.add(i);
		
		for(int i : series(size()))
			for(int j : series(i+1, size()))
				if(connected(i, j))
					post.nodes().get(i).connect(post.nodes().get(j));
		
		return post;
	}
		
	/**
	 * Whether box i and box j are connected. Ie. there is at least one link 
	 * between a member of i and a member of j.
	 *  
	 * @param i
	 * @param j
	 * @return
	 */
	public boolean connected(int i, int j)
	{
		for(Node<L> nodeI : get(i))
			for(Node<L> nodeJ : get(j))
				if(nodeI.connected(nodeJ))
					return true;
		
		return false;
	}
	
}
