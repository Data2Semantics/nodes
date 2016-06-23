package org.nodes;

import static java.util.Collections.unmodifiableList;
import static nl.peterbloem.kit.Functions.equals;
import static nl.peterbloem.kit.Series.series;

import java.io.File;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.nodes.data.Data;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * A light-weight (ie. low memory) implementation of a directed graph.
 * 
 * This graph uses non-persistent objects for nodes and links. This means that 
 * these objects become invalid if the graph is edited in such a way that its 
 * node indices change. If a node or link object belonging to this graph is 
 * accessed after such a change, an exception will be thrown   
 * 
 * @author Peter
 *
 * @param <L>
 */
public class LightUGraph<L> implements UGraph<L>, FastWalkable<L, UNode<L>>
{
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;
	
	private List<L> labels;
	
	List<List<Integer>> neighbors;
	
	private int numLinks = 0;
	private long modCount = 0;
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just node removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;
	
	private boolean sorted = false;
	
	public LightUGraph()
	{
		this(16);
	}
	
	public LightUGraph(int capacity)
	{
		neighbors = new ArrayList<List<Integer>>(capacity);
		
		labels = new ArrayList<L>(capacity);
	}
	
	@Override
	public int size()
	{
		return labels.size();
	}

	@Override
	public int numLinks()
	{
		return numLinks;
	}

	@Override
	public UNode<L> node(L label)
	{
		int i = labels.indexOf(label);
		if(i == -1)
			throw new NoSuchElementException("Graph does not contain node with label "+label+"");
		
		return new LightUNode(i);
	}
	
	private class LightUNode implements UNode<L>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;

		public LightUNode(int index)
		{
			this.index = index;
		}
		
		@Override
		public L label()
		{
			check();
			
			return labels.get(index);
		}

		@Override
		public void remove()
		{
			check();
			
			int linksRemoved = degree();
			linksRemoved -= links(this).size();
			
			for(List<Integer> neighbor : neighbors)
			{
				Iterator<Integer> it = neighbor.iterator();
				while(it.hasNext())
					if(it.next() == index)
						it.remove();
			}
			
			// * move through all neighbor lists and decrement every index that 
			//   is higher than the one we just removed.  
			for(List<Integer> list : neighbors)
				for(int i : series(list.size()))
				{
					Integer value = list.get(i);
					if(value > index)
						list.set(i, value - 1);
				}		
			
			neighbors.remove((int)index);
			labels.remove((int)index);

			numLinks -= linksRemoved;
			
			dead = true;
			
			modCount++;
			nodeModCount++;
			
			sorted = false;
		}

		private void check()
		{
			if(dead)
				throw new IllegalStateException("Node is dead (index was "+index+")");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation. Node object is out of date.");
		}

		@Override
		public boolean dead()
		{
			return dead;
		}

		@Override
		public int degree()
		{
			check();
			
			return neighbors.get(index).size();
		}

		@Override
		public Collection<? extends UNode<L>> neighbors()
		{
			check();
			
			Set<Integer> set = new LinkedHashSet<Integer>(neighbors.get(index));
			
			return new NodeList(new ArrayList<Integer>(set));
		}

		@Override
		public UNode<L> neighbor(L label)
		{
			check();
			
			for(int i : neighbors.get(this.index))
				if(eq(labels.get(i), label))
					return new LightUNode(index);
			
			return null;
		}

		@Override
		public Collection<? extends UNode<L>> neighbors(L label)
		{
			check();
			
			List<Integer> indices = new ArrayList<Integer>(degree());
	
			for(int i : neighbors.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public ULink<L> connect(Node<L> to)
		{
			check();
			
			int fromIndex = index, toIndex = to.index();
			
			neighbors.get(fromIndex).add(toIndex);
			if(fromIndex != toIndex)
				neighbors.get(toIndex).add(fromIndex);
						
			modCount++;			
			numLinks++;
			
			sorted = false;
			
			return new LightULink(index(), to.index());
		}

		@Override
		public void disconnect(Node<L> other)
		{
			check();
			
			int mine = index, his = other.index();
		
			while(neighbors.get(mine).remove((Integer)his))
				numLinks--;
			while(neighbors.get(his).remove((Integer)mine))
				numLinks--;
			
			modCount++;
		}

		@Override
		public boolean connected(Node<L> other)
		{
			check();
			
			if(this.graph() != other.graph())
				return false;
			
			int mine = index, his = other.index();
			
			if(neighbors.get(mine).contains(his))
				return true;
			
			return false;
		}

		@Override
		public UGraph<L> graph()
		{
			check();
			
			return LightUGraph.this;
		}

		@Override
		public int index()
		{
			check();
			
			return index;
		}

		@Override
		public int hashCode()
		{
			check();
			
			final int prime = 31;
			int result = 1;
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((index == null) ? 0 : index.hashCode());
			result = prime * result + ((labels.get(index) == null) ? 0 : labels.get(index).hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			check();
			
			if (this == obj)
				return true;
			
			if (obj == null)
				return false;
			
			if (getClass() != obj.getClass())
				return false;
			
			LightUNode other = (LightUNode) obj;
			
			if (this.graph() != other.graph())
				return false;
			
			if (dead != other.dead)
				return false;
			
			return index.equals(other.index);
		}
		
		public String toString()
		{
			check();
			
			return label() == null ? ("n"+index()) : label().toString();
		}

		@Override
		public List<ULink<L>> links()
		{
			check();
			
			List<ULink<L>> list = new ArrayList<ULink<L>>(degree());
			for(int neighbor : neighbors.get(index))
				list.add(new LightULink(index, neighbor));
			
			return list;
		}

		@Override
		public Collection<? extends ULink<L>> links(Node<L> other)
		{
			check();
			
			List<ULink<L>> list = new ArrayList<ULink<L>>();
			
			int o = other.index();
			for(int neighbor : neighbors.get(index))
				if(neighbor == o)
					list.add(new LightULink(index, neighbor));
						
			return list;
		}
	}
	
	private class LightULink implements ULink<L>
	{
		private UNode<L> from, to;
		
		private long nodeModState = nodeModCount;
		
		private boolean dead = false;
		
		public LightULink(int from, int to)
		{
			this.from = new LightUNode(from);
			this.to = new LightUNode(to);
		}
		
		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		
		
		@Override
		public Collection<? extends UNode<L>> nodes()
		{
			check();
			return Arrays.asList(from, to);
		}

		@Override
		public Graph<L> graph()
		{
			check();
			return LightUGraph.this;
		}

		@Override
		public void remove()
		{
			check();
			neighbors.get(to.index()).remove((Integer)from.index());
			neighbors.get(from.index()).remove((Integer)to.index());
			
			
			numLinks--;
			modCount++;
			dead = true;
			
			sorted = false;
		}

		@Override
		public boolean dead()
		{
			check();
			return dead;
		}

		@Override
		public UNode<L> first()
		{
			check();
			return from;
		}

		@Override
		public UNode<L> second()
		{
			check();
			return to;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + (from.hashCode());
			result = prime * result + (to.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LightULink other = (LightULink) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dead != other.dead)
				return false;
			
			if(from == other.from && to == other.to)
				return true;
			
			if(from == other.to && to == other.from)
				return true;

			return false;
		}

		private LightUGraph getOuterType()
		{
			return LightUGraph.this;
		}
		
		public String toString()
		{
			check();
			return from + " -- " + to;
		}

		@Override
		public UNode<L> other(Node<L> current)
		{
			if(! first().equals(current))
				return first();
			return second();
		}
	}

	private class NodeList extends AbstractList<UNode<L>>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public LightUNode get(int index)
		{
			return new LightUNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}
	
	@Override
	public Collection<? extends UNode<L>> nodes(L label)
	{
		// * count the occurrences so that we can set the ArrayList's capacity 
		//   accurately
		int frq = 0;
		for(L l : labels)
			if(eq(l, label))
				frq++;
			
		List<Integer> indices = new ArrayList<Integer>(frq);
		
		for(int i : Series.series(size()))
			if(eq(labels.get(i), label))
				indices.add(i);
		
		return new NodeList(indices);
	}

	@Override
	public List<? extends UNode<L>> nodes()
	{
		return new NodeList(Series.series(size()));
	}
	
	@Override
	public Collection<? extends ULink<L>> links()
	{
		return new LinkCollection();
	}
	
	/**
	 * A collection of all links in this graph.
	 * 
	 * @author Peter
	 *
	 */
	private class LinkCollection extends AbstractCollection<ULink<L>>
	{
		@Override
		public Iterator<ULink<L>> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return numLinks;
		}
		
		private class LLIterator implements Iterator<ULink<L>>
		{
			private static final int BUFFER_LIMIT = 5;
			private long graphState = state();
			private Deque<LightULink> buffer = new LinkedList<LightULink>();
			int next = 0;
			
			private void check()
			{
				if(graphState != state())
					throw new ConcurrentModificationException("Graph has been modified.");
			}

			@Override
			public boolean hasNext()
			{
				check();
				read();
				
				return ! buffer.isEmpty();
			}

			@Override
			public ULink<L> next()
			{
				check();
				read();
				
				if(buffer.isEmpty())
					throw new NoSuchElementException();
				
				return buffer.pop();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}
			
			private void read()
			{
				if(next >= LightUGraph.this.size())
					return;
				
				while(buffer.size() < BUFFER_LIMIT && next < LightUGraph.this.size())
				{
					int from = next;
					
					List<Integer> tos = neighbors.get(from);
					for(int to : tos)
						if(to >= from)
							buffer.add(new LightULink(from, to));
					
					next++;
				}
					
			}
		}
		
	}

	@Override
	public UNode<L> add(L label)
	{
		labels.add(label);
		
		neighbors.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		
		sorted = false;
		return new LightUNode(neighbors.size() - 1);
	}

	@Override
	public Set<L> labels()
	{
		return new HashSet<L>(labels);
	}

	@Override
	public boolean connected(L from, L to)
	{
		for(UNode<L> a : nodes(from))
			for(UNode<L> b : nodes(to))
				if(a.connected(b))
					return true;
		
		return false;
	}

	@Override
	public long state()
	{
		return modCount;
	}
	
	private boolean eq(Object a, Object b)
	{
		if(a == null && b == null)
			return true;
		
		if(a == null || b == null)
			return false;
		
		return a.equals(b);
	}
	
	/**
	 * Returns a representation of the graph in Dot language format.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("graph {");
		
		Set<UNode<L>> nodes = new HashSet<UNode<L>>(nodes());

		int i = 0;
		for(ULink<L> link : links())
		{
			if(i++ != 0)
				sb.append(";");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}

		for(UNode<L> node : nodes)
		{
			if(i++ != 0) 
				sb.append(";");
			
			sb.append(node);
		}
		
		sb.append("}");
		
		return sb.toString();
	}
	
	/**
	 * Resets all neighbour list to their current capacity, plus the 
	 * given margin. 
	 * 
	 * @param margin
	 */
	public void compact(int margin)
	{
		for(int i : Series.series(neighbors.size()))
		{
			List<Integer> old = neighbors.get(i);
			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
			nw.addAll(old);
			
			neighbors.set(i, nw);
		}
	}
	
	/**
	 * Sorts all neighbour lists
	 * 
	 * @param margin
	 */
	public void sort()
	{
		if(sorted)
			return;
		
		for(int i : Series.series(neighbors.size()))
			Collections.sort(neighbors.get(i));
		
		sorted = true;
	}
	
	
	
	/**
	 * Creates a copy of the given graph as a LightUGraph object. 
	 * 
	 * @param graph
	 * @return
	 */
	public static <L> LightUGraph<L> copy(Graph<L> graph)
	{
		if(graph instanceof LightUGraph)
		{
			LightUGraph<L> other = (LightUGraph<L>) graph;
			LightUGraph<L> copy = new LightUGraph<L>();
			
			copy.labels = new ArrayList<L>(other.labels);
			copy.neighbors = new ArrayList<List<Integer>>(other.neighbors.size());
			for(List<Integer> nb : other.neighbors)
				copy.neighbors.add(new ArrayList<Integer>(nb));
			
			copy.numLinks = other.numLinks;
			copy.hash = other.hash;
			
			copy.modCount++;
			copy.nodeModCount++;
			
			if(other.hashMod == null)
				copy.hashMod = null;
			else
				copy.hashMod = 
					other.hashMod == other.modCount ? copy.modCount : copy.modCount - 1;
			
			copy.sorted = other.sorted;
			
			return copy;
		}
		
		LightUGraph<L> copy = new LightUGraph<L>(graph.size());
		for(Node<L> node : graph.nodes())
			copy.add(node.label());
		
		for(Link<L> link : graph.links())
			copy.get(link.first().index()).connect(copy.get(link.second().index()));
		
		copy.compact(0);
		
		return copy;
	}
	
	@Override 
	public int hashCode()
	{
		if(hashMod != null && hashMod == modCount)
			return hash;
		
		hash = 1;
		sort();
		
		for(int i : Series.series(size()))
		    hash = 31 * hash + (labels.get(i) == null ? 0 : labels.get(i).hashCode());
		
		// * structure
		for(UNode<L> node : nodes())
		{
			List<Integer> nbIndices = new ArrayList<Integer>(node.degree());
			for(UNode<L> neighbor : node.neighbors())
				nbIndices.add(neighbor.index());
			
			Collections.sort(nbIndices);
			hash = 31 * hash + nbIndices.hashCode();
		}
		
		hashMod = modCount;
		
		return hash;
	}	
	
	public boolean equals(Object other)
	{	
		if(!(other instanceof UGraph<?>))
			return false;
		
		UGraph<Object> oth = (UGraph<Object>) other;
		if(! oth.level().equals(level()))
			return false;
		
		if(size() != oth.size())
			return false;
		
		if(numLinks() != oth.numLinks())
			return false;
		
		if(labels().size() != oth.labels().size())
			return false;
		
		for(UNode<L> node : nodes())
		{
			UNode<Object> othNode = oth.get(node.index());
			
			if(! Functions.equals(node.label(), othNode.label()))
				return false;
			
			FrequencyModel<Integer> myNeighbors = new FrequencyModel<Integer>(),
			                        hisNeighbors = new FrequencyModel<Integer>();
			for(UNode<L> myNeighbor : node.neighbors())
				myNeighbors.add(myNeighbor.index());
			
			for(UNode<Object> hisNeighbor : othNode.neighbors())
				hisNeighbors.add(hisNeighbor.index());
						
			if(! myNeighbors.equals(hisNeighbors))
				return false;
		}
		
		return true;
		
	}

	@Override
	public UNode<L> get(int i)
	{
		return nodes().get(i);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends UGraph<L>> level()
	{
		Object obj = DGraph.class;
		return (Class<? extends UGraph<L>>) obj;
	}

	@Override
	public List<UNode<L>> neighborsFast(Node<L> node)
	{
		if(node.graph() != this)
			throw new IllegalArgumentException("Cannot call with node from another graph.");
		
		List<Integer> indices = neighbors.get(node.index());
		
		return new NodeList(indices);
	}
}
