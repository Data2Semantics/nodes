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
	 * returned list represents a (weakly) connected subgraph, and all such 
	 * subgraphs are returned once.  
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
					buffer.addAll(0, next.children());
				
				if(buffer.isEmpty())
					return;
			}
			
			assert(buffer.isEmpty() || buffer.peek().isLeaf());
		}
		
		private class State
		{
			Set<Integer> current = new LinkedHashSet<Integer>();
			Set<Integer> neighbors = new LinkedHashSet<Integer>();
			
			private State()
			{
				
			}
			
			public State(int start)
			{
				current.add(start);
				initNeighbors();
			}
			
			private void initNeighbors()
			{
				if(isLeaf()) 
					return;
				
				int max = Functions.max(current);
				
				for(int index : current)
					for(Node<?> neighbor : data.get(index).neighbors())
						if(neighbor.index() > max && ! current.contains(neighbor.index()))
							neighbors.add(neighbor.index());
			}
			
			/**
			 * Adds all children of the state to the given list.
			 * 
			 * @param list
			 */
			public List<State> children()
			{
				// * TODO: we can generate this list on the fly
				List<State> children = new ArrayList<State>(neighbors.size());
				
				for(int neighbor : neighbors)
				{
					State state = new State();
					
					state.current.addAll(this.current);
					state.current.add(neighbor);
					
					state.initNeighbors();
					
					children.add(state);
				}
				
				return children;
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
	
}
