package org.nodes;

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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Pair;
import org.nodes.util.Series;

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
public class LightDGraph<L> implements DGraph<L>
{
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;
	
	private List<L> labels;
	
	private List<List<Integer>> out;
	private List<List<Integer>> in;	
	
	private int numLinks = 0;
	private long modCount = 0;
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;
	
	private boolean sorted = false;
	
	public LightDGraph()
	{
		this(16);
	}
	
	public LightDGraph(int capacity)
	{
		out = new ArrayList<List<Integer>>(capacity);
		in = new ArrayList<List<Integer>>(capacity); 
		
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
	public DNode<L> node(L label)
	{
		int i = labels.indexOf(label);
		if(i == -1)
			throw new NoSuchElementException("Graph does not contain node with label "+label+"");
		
		return new LightDNode(i);
	}
	
	private class LightDNode implements DNode<L>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;

		public LightDNode(int index)
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

			int linksRemoved = inDegree() + outDegree();
			linksRemoved -= linksOut(this).size();
			
			numLinks -= linksRemoved;
			
			for(List<Integer> neighbors : out)
			{
				Iterator<Integer> it = neighbors.iterator();
				while(it.hasNext())
					if(it.next() == index)
						it.remove();
			}
			for(List<Integer> neighbors : in)
			{
				Iterator<Integer> it = neighbors.iterator();
				while(it.hasNext())
					if(it.next() == index)
						it.remove();
			}
			
			in.remove((int)index);
			out.remove((int)index);
			labels.remove((int)index);

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
				throw new IllegalStateException("Graph was modified since node creation.");
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
			return inDegree() + outDegree();
		}

		@Override
		public Collection<? extends DNode<L>> neighbors()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(degree());
			
			for(int i : in.get(this.index))
				indices.add(i);
			for(int i : out.get(this.index))
				indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public DNode<L> neighbor(L label)
		{
			check();
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					return new LightDNode(index);
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					return new LightDNode(index);
			
			return null;
		}

		@Override
		public Collection<? extends DNode<L>> neighbors(L label)
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(degree());
	
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<L>> out()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());
			
			for(int i : out.get(this.index))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<L>> out(L label)
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());
			
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<L>> in()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());
			
			for(int index : in.get(this.index))
				indices.add(index);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<L>> in(L label)
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());
			
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public DLink<L> connect(Node<L> to)
		{
			check();
			int fromIndex = index, toIndex = to.index();
			
			out.get(fromIndex).add(toIndex);
			in.get(toIndex).add(fromIndex);
			
			// Collections.sort(out.get(fromIndex));
			// Collections.sort(out.get(toIndex));
			
			modCount++;			
			numLinks++;
			
			sorted = false;
			
			return new LightDLink(index(), to.index());
		}

		@Override
		public void disconnect(Node<L> other)
		{
			int mine = index, his = other.index();
			
			int links = 0;
			
			while(out.get(mine).remove((Integer)his))
				links++;
			while(out.get(his).remove((Integer)mine))
				links++;
			
			while(in.get(mine).remove((Integer)his));
			while(in.get(his).remove((Integer)mine));

			numLinks -= links;			
			modCount++;
			
			sorted = false;
		}

		@Override
		public boolean connected(Node<L> other)
		{
			if(!(other instanceof DNode<?>))
				return false;
			
			DNode<L> o = (DNode<L>) other;
			
			return this.connectedTo(o) || o.connectedTo(this);
		}
		
		@Override
		public boolean connectedTo(DNode<L> to)
		{
			int mine = index, his = to.index();
			
			if(out.get(mine).contains(his))
				return true;
			
			return false;
		}

		@Override
		public DGraph<L> graph()
		{
			return LightDGraph.this;
		}

		@Override
		public int index()
		{
			return index;
		}

		@Override
		public int inDegree()
		{
			return in.get(index).size();
		}

		@Override
		public int outDegree()
		{
			return out.get(index).size();
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((index == null) ? 0 : index.hashCode());
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
			LightDNode other = (LightDNode) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dead != other.dead)
				return false;
			if (index == null)
			{
				if (other.index != null)
					return false;
			} else if (!index.equals(other.index))
				return false;
			return true;
		}

		private LightDGraph getOuterType()
		{
			return LightDGraph.this;
		}
		
		public String toString()
		{
			return label() == null ? ("n"+index()) : label().toString() + "_" +index();
		}

		@Override
		public List<DLink<L>> links()
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>(degree());
			for(int neighbor : out.get(index))
				list.add(new LightDLink(index, neighbor));
			
			for(int neighbor : in.get(index))
				if(neighbor != index) // no double reflexive links
					list.add(new LightDLink(neighbor, index));	
			
			return list;
		}

		@Override
		public List<DLink<L>> linksOut()
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>(outDegree());
			for(int neighbor : out.get(index))
				list.add(new LightDLink(index, neighbor));
			
			return list;
		}

		@Override
		public List<DLink<L>> linksIn()
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>(inDegree());
			for(int neighbor : in.get(index))
				list.add(new LightDLink(neighbor, index));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<L>> links(Node<L> other)
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>();
			
			int o = other.index();
			for(int neighbor : out.get(index))
				if(neighbor == o)
					list.add(new LightDLink(index, neighbor));
			
			o = other.index();
			for(int neighbor : in.get(index))
				if(index != o && neighbor == 0) // no double reflexive
					list.add(new LightDLink(neighbor, index));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<L>> linksOut(DNode<L> other)
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>(outDegree());
			
			int o = other.index();
			for(int neighbor : out.get(index))
				if(neighbor == o)
					list.add(new LightDLink(index, neighbor));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<L>> linksIn(DNode<L> other)
		{
			List<DLink<L>> list = new ArrayList<DLink<L>>(inDegree());
			
			int o = other.index();
			for(int neighbor : in.get(index))
				if(neighbor == 0)
					list.add(new LightDLink(neighbor, index));
			
			return list;
		}
	}
	
	private class LightDLink implements DLink<L>
	{
		private DNode<L> from, to;
		
		
		private long nodeModState = nodeModCount;
		
		private boolean dead = false;
		
		public LightDLink(int from, int to)
		{
			this.from = new LightDNode(from);
			this.to = new LightDNode(to);
		}
		
		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		
		
		@Override
		public Collection<? extends Node<L>> nodes()
		{
			check();
			return Arrays.asList(from, to);
		}

		@Override
		public Graph<L> graph()
		{
			check();
			return LightDGraph.this;
		}

		@Override
		public void remove()
		{
			check();
			in.get(to.index()).remove((Integer)from.index());
			out.get(from.index()).remove((Integer)to.index());
			
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
		public DNode<L> first()
		{
			check();
			return from;
		}

		@Override
		public DNode<L> second()
		{
			check();
			return to;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
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
			LightDLink other = (LightDLink) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dead != other.dead)
				return false;
			if (from == null)
			{
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null)
			{
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}

		private LightDGraph getOuterType()
		{
			return LightDGraph.this;
		}
		
		public String toString()
		{
			check();
			return from + " -> " + to;
		}

		@Override
		public DNode<L> from()
		{
			return from;
		}

		@Override
		public DNode<L> to()
		{
			return to;
		}

		@Override
		public DNode<L> other(Node<L> current)
		{
			if(! first().equals(current))
				return first();
			return second();
		}
	}

	private class NodeList extends AbstractList<DNode<L>>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public LightDNode get(int index)
		{
			return new LightDNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}
	
	@Override
	public Collection<? extends DNode<L>> nodes(L label)
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
	public List<? extends DNode<L>> nodes()
	{
		return new NodeList(Series.series(size()));
	}
	
	@Override
	public Collection<? extends DLink<L>> links()
	{
		return new LinkCollection();
	}
	
	/**
	 * A collection of all links in this graph.
	 * 
	 * @author Peter
	 *
	 */
	private class LinkCollection extends AbstractCollection<DLink<L>>
	{
		@Override
		public Iterator<DLink<L>> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return numLinks;
		}
		
		private class LLIterator implements Iterator<DLink<L>>
		{
			private static final int BUFFER_LIMIT = 5;
			private long graphState = state();
			private Deque<Pair<Integer, Integer>> buffer = new LinkedList<Pair<Integer,Integer>>();
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
			public DLink<L> next()
			{
				check();
				read();
				
				if(buffer.isEmpty())
					throw new NoSuchElementException();
				
				Pair<Integer, Integer> pair = buffer.pop();
				
				return new LightDLink(pair.first(), pair.second());
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}
			
			private void read()
			{
				if(next >= LightDGraph.this.size())
					return;
				
				while(buffer.size() < BUFFER_LIMIT && next < LightDGraph.this.size())
				{
					int from = next;
					
					List<Integer> tos = out.get(from);
					for(int to : tos)
						buffer.add(new Pair<Integer, Integer>(from, to));
					
					next++;
				}
					
			}
		}
		
	}

	@Override
	public DNode<L> add(L label)
	{
		labels.add(label);
		
		in.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		out.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		
		sorted = false;
		return new LightDNode(in.size() - 1);
	}

	@Override
	public Set<L> labels()
	{
		return new HashSet<L>(labels);
	}

	@Override
	public boolean connected(L from, L to)
	{
		for(DNode<L> a : nodes(from))
			for(DNode<L> b : nodes(to))
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
		sb.append("digraph {");
		
		Set<DNode<L>> nodes = new HashSet<DNode<L>>(nodes());
		
		for(DLink<L> link : links())
		{
			if(sb.length() != 9) 
				sb.append("; ");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}
		
		for(DNode<L> node : nodes)
			sb.append("; " + node);
		
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
		for(int i : Series.series(in.size()))
		{
			List<Integer> old = in.get(i);
			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
			nw.addAll(old);
			
			in.set(i, nw);
		}
		
		for(int i : Series.series(out.size()))
		{
			List<Integer> old = out.get(i);
			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
			nw.addAll(old);
			
			out.set(i, nw);
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
		
		for(int i : Series.series(in.size()))
			Collections.sort(in.get(i));
		
		for(int i : Series.series(out.size()))
			Collections.sort(out.get(i));
		
		sorted = true;
	}
	
	
	
	/**
	 * Creates a copy of the given graph as a LightDGraph object. If the argument
	 * is directional, the links will be copied, if it is not directional, every
	 * link in the original will be represented by two links in the result.
	 * 
	 * @param graph
	 * @return
	 */
	public static <L> LightDGraph<L> copy(Graph<L> graph)
	{
		LightDGraph<L> copy = new LightDGraph<L>(graph.size());
		for(Node<L> node : graph.nodes())
			copy.add(node.label());
		
		for(int i : Series.series(graph.size()))
			for(int j : Series.series(graph.size()))
			{
				if(graph.nodes().get(i).connected(graph.nodes().get(j)))
					copy.nodes().get(i).connect(copy.nodes().get(j));
			}
		
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
		{
		    hash = 31 * hash + (labels.get(i) == null ? 0 : labels.get(i).hashCode());
		   //  hash = 31 * hash + (in.get(i) == null ? 0 : in.get(i).hashCode());
		}
		
		return hash;
	}	
	
	public boolean equals(Object other)
	{	
		if(!(other instanceof DGraph<?>))
			return false;
		
		DGraph<Object> oth = (DGraph<Object>) other;
		if(! oth.level().equals(level()))
			return false;
		
		if(size() != oth.size())
			return false;
		
		if(numLinks() != oth.numLinks())
			return false;
		
		if(labels().size() != oth.labels().size())
			return false;
		
		for(DNode<L> node : nodes())
		{
			DNode<Object> othNode = oth.get(node.index());
			
			if(! Functions.equals(node.label(), othNode.label()))
				return false;
			
			FrequencyModel<Integer> outs = new FrequencyModel<Integer>(),
			                        othOuts = new FrequencyModel<Integer>();
			for(DNode<L> neighbor : node.out())
				outs.add(neighbor.index());
			
			for(DNode<Object> othNeighbor : othNode.out())
				othOuts.add(othNeighbor.index());
			
			if(! outs.equals(othOuts))
				return false;
		}
		
		return true;
		
	}

	@Override
	public DNode<L> get(int i)
	{
		return nodes().get(i);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends DGraph<L>> level()
	{
		Object obj = DGraph.class;
		return (Class<? extends DGraph<L>>) obj;
	}
}
