package org.nodes.motifs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nodes.Graph;
import org.nodes.Node;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

/**
 * Enumerates all subgraphs using the FANMOD algorithm.
 * @author Peter
 *
 */
public class AllSubgraphs implements Iterable<Set<Integer>>
{
	private Graph<?> data;
	private int size;
	private List<Double> probs = null;
	
	
	public AllSubgraphs(Graph<?> data, int size) 
	{
		this.data = data;
		this.size = size;
	}
	
	/**
	 * @param data
	 * @param size
	 * @param prob The probability at each node in the search tree, that that
	 *             node will be expanded.
	 */
	public AllSubgraphs(Graph<?> data, int size, double prob) 
	{
		this.data = data;
		this.size = size;
		
		probs = new ArrayList<Double>(size);
		for(int i : Series.series(size))
			probs.add(prob);
	}
	
	/**
	 * Returns an iterator over lists indices of the original graph. Each 
	 * returned list represents a (weakly) connected subgraph, and any such 
	 * subgraph is returned only once.  
	 * 
	 * @return
	 */
	public Iterator<Set<Integer>> iterator()
	{
		return new Enumerator();
	}
 
	private class Enumerator implements Iterator<Set<Integer>>
	{
		private LinkedList<State> buffer = new LinkedList<State>();
		
		public Enumerator()
		{
			for(int index : Series.series(data.size()))
				buffer.add(new State(index));
		}

		@Override
		public boolean hasNext() 
		{
			expandBuffer();
			return ! buffer.isEmpty();
		}

		@Override
		public Set<Integer> next() 
		{
			expandBuffer();
			return buffer.pop().indices();
		}
		
		/**
		 * Expands the buffer until the first element is a leaf state. If that's
		 * not possible, it returns. 
		 */
		private void expandBuffer()
		{
			if(buffer.isEmpty())
				return;
			
			while(! buffer.peek().isLeaf())
			{				
				State next = buffer.pop();
				
				if(probs == null || Global.random().nextDouble() < probs.get(next.indices().size() - 1))
					for(State state : next.children())
						buffer.add(0, state);
				
				if(buffer.isEmpty())
					return;
			}
			
			assert(buffer.isEmpty() || buffer.peek().isLeaf());
		}
		
		private class State
		{
			// * The first integer added
			int start;
			Set<Integer> current = new LinkedHashSet<Integer>();
			Set<Integer> neighbors = new LinkedHashSet<Integer>();
			
			private State()
			{
				
			}
			
			/**
			 * Create an initial node 
			 * @param start
			 */
			public State(int start)
			{
				this.start = start;
				current.add(start);
								
				for(Node<?> neighbor : data.get(start).neighbors())
						if(neighbor.index() > start)
							neighbors.add(neighbor.index());
			}
			
			/**
			 * Returns a list of children of this state
			 * 
			 * @param list
			 */
			public Iterable<State> children()
			{
				return new Iterable<State>() 
				{
					@Override
					public Iterator<State> iterator()
					{
						return new Iterator<State>() 
						{
							LinkedList<Integer> oldNeighbors = new LinkedList<Integer>(neighbors);


							@Override
							public State next()
							{
								int choice = oldNeighbors.pop();
								
								State state = new State();
								
								state.start = start;
								
								state.current.addAll(current);
								
								state.neighbors.addAll(oldNeighbors);
								// * add all neighbors of the new node provided that:
								// - their index is higher than start
								// - they are not neighbors to any nodes in current 
								for(Node<?> node : data.get(choice).neighbors())
									if(node.index() > start && ! inNeighborhood(node, current))
										state.neighbors.add(node.index());
								
								state.current.add(choice);

								return state;
							}
							
							@Override
							public boolean hasNext()
							{
								return ! oldNeighbors.isEmpty();
							}

						
						};
					}
					
				};
			}
			
			/**
			 * Whether this state represents a complete subgraph (ie. it has the
			 * required number of nodes).
			 * @return
			 */
			public boolean isLeaf()
			{
				return current.size() == size;
			}
			
			public Set<Integer> indices()
			{
				return current;
			}
			
			public String toString()
			{
				return current + "_" + neighbors;
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private boolean inNeighborhood(Node<?> node, Set<Integer> current)
	{
		Set<Integer> n = new LinkedHashSet<Integer>();
		for(Node<?> neighb : node.neighbors())
			n.add(neighb.index());

		return Functions.overlap(n, current) > 0;
	}
}
