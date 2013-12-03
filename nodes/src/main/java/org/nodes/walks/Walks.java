package org.nodes.walks;

import static org.nodes.util.Functions.reverse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nodes.Acyclic;
import org.nodes.Graph;
import org.nodes.Node;
import org.nodes.util.Functions;

/**
 * <p>
 * This class provides several factories for Walks, Iterables over graphs.
 * The point of this class is to make it simple to traverse graphs.
 * 	</p><p>
 * For example:
 * <code><pre>
 * 		// * Follow a given track
		List<String> track = Arrays.asList("a", "b", "c", "d");
		for( : Walks.track(graph, track))
			...
		
		// * Look for a label depth first 
		String target = "z";
		for( String label : Walk.depthFirst(graph, graph.get(0)) )
			if(label.equals(target))
				... 
 * </pre></code>
 *
 * @author peter
 */
public class Walks
{
	/**
	 * Returns a breadth first walk that visits each node in the graph exactly 
	 * once.
	 * 
	 * The walk maintains a history of nodes it has already returned, unless the
	 * graph is guaranteed acyclic (because it implements {@link Acyclic}. 
	 * 
	 * @param <L> The type of node labels in the graph
	 * @param <N> The node-type of the graph
	 * @param graph The grap
	 * @param root The node to start the walk.
	 * @return 
	 */
	public static <L> Walk<L> breadthFirst(Graph<L> graph, Node<L> root)
	{
		return new BFWalk<L>(graph, root);
	}
	
	private static class BFWalk<L> extends AbstractWalk<L>
	{
		protected Graph<L> graph;
		protected Node<L> start;		
		
		public BFWalk(Graph<L> graph, Node<L> start)
		{
			this.graph = graph;
			this.start = start;
		}

		public java.util.Iterator<Node<L>> iterator()
		{
			return new Iterator(); 
		}
		
		private class Iterator implements java.util.Iterator<Node<L>>
		{
			private LinkedList<Node<L>> buffer;
			private int next = 0; // The next node in the buffer to unpack
			
			// * Whether we can assume that the given graph is acyclic
			private boolean acyclic = (graph instanceof Acyclic);  
			// * History of visited nodes
			private Set<Node<L>> history = acyclic ? new HashSet<Node<L>>() : null;
			
			public Iterator() {
				buffer = new LinkedList<Node<L>>();
				buffer.add(start);
			}

			@Override
			public boolean hasNext()
			{
				ensure();
				
				return ! buffer.isEmpty();
				
			}

			@Override
			public Node<L> next()
			{
				ensure();

				next --;
				
				if(acyclic) history.add(buffer.peek());
				return buffer.poll();
			}

			@Override
			public void remove()
			{
				// TODO: implement
				throw new UnsupportedOperationException();
			}
			
			private void ensure()
			{
				if(buffer.size() < 5 && next < buffer.size())
					for(Node<L> node : buffer.get(next).neighbors())
						if(! history.contains(node))
							buffer.add(node);
				
				next++;
			}
		}
	}

	/**
	 * 
	 * @param <T>
	 * @return
	 */
	public static <L> Walk<L> depthFirst(Graph<L> graph, Node<L> root)
	{
		return new DFWalk<L>(graph, root);
	}

	private static class DFWalk<L> extends AbstractWalk<L>
	{
		protected Graph<L> graph;
		protected Node<L> start;		
		
		public DFWalk(Graph<L> graph, Node<L> start)
		{
			this.graph = graph;
			this.start = start;
		}

		public java.util.Iterator<Node<L>> iterator()
		{
			return new Iterator(); 
		}
		
		private class Iterator implements java.util.Iterator<Node<L>>
		{
			private LinkedList<Node<L>> buffer;
			private int next = 0; // The next node in the buffer to unpack
			
			// * Whether we can assume that the given graph is acyclic
			private boolean acyclic = (graph instanceof Acyclic);  
			// * History of visited nodes
			private Set<Node<L>> history = acyclic ? new HashSet<Node<L>>() : null;
			
			public Iterator() {
				buffer = new LinkedList<Node<L>>();
				buffer.add(start);
			}

			@Override
			public boolean hasNext()
			{
				ensure();
				
				return ! buffer.isEmpty();
				
			}

			@Override
			public Node<L> next()
			{
				ensure();

				next --;
				
				if(acyclic) history.add(buffer.peek());
				return buffer.poll();
			}

			@Override
			public void remove()
			{
				// TODO: implement
				throw new UnsupportedOperationException();
			}

			/**
			 * Ensures that the buffer has enough nodes, if they are available
			 */
			private void ensure()
			{

				if(buffer.size() < 5 && next < buffer.size())
					for(Node<L> node : buffer.get(next).neighbors())
						buffer.add(next+1, node);
				
				next++;
			}
			
		}
	}



}
