package org.nodes;

import static java.lang.Math.max;
import static nl.peterbloem.kit.Functions.concat;
import static nl.peterbloem.kit.Series.series;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Pump;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.mapdb.serializer.SerializerUtils;

import com.google.code.externalsorting.ExternalSort;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * A version of the LightDGraph which stores all data on disk. It's slower, 
 * but can store much bigger graphs.
 * 
 * 
 * The db file can be stored and re-used later. Make sure to close the graph with 
 * close().
 * 
 * The graph supports more than Integer.MAX_VALUE links in total, however, the 
 * in- and outdegrees of each node need to be below that value.
 * 
 * @author Peter
 *
 * @param <L>
 */
public class DiskDGraph implements DGraph<String>, FastWalkable<String, DNode<String>>
{
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;
	
	private DB db;
	
	private boolean nullLabels = false;
	private List<String> labels;
	
	private List<List<Integer>> in;
	private List<List<Integer>> out;
	
	private long numLinks = 0;
	private long modCount = 0;
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;
	
	private boolean sorted = false;
		
	public DiskDGraph(File dbFile)
	{
		this(dbFile, false);
	}	
	
	/**
	 * 
	 * @param dbFile The file containing the graph structure. If the file doesn't exist, 
	 *   it will be created. If it does exist, the graph it contains will be loaded.
	 * @param nullLabels If true, all labels will be null (saving some space). 
	 *    Adding a node with a nonnull label will result in an exception.
	 */
	public DiskDGraph(File dbFile, boolean nullLabels)
	{
		this.nullLabels = nullLabels;
		
		db = DBMaker.fileDB(dbFile).make();
		
		labels = nullLabels ? null : db.indexTreeList("labels", Serializer.STRING).createOrOpen();
		 
		in  = db.indexTreeList("in",  new SerializerIntList()).createOrOpen();
		out = db.indexTreeList("out", new SerializerIntList()).createOrOpen();
		
		if(!nullLabels && labels.size() != in.size())
			throw new IllegalStateException("labels list has size "+ labels.size() + ", should be " + in.size() + ".");
		
		if(db.exists("numLinks"))
			numLinks = db.atomicLong("numLinks").createOrOpen().get();
		else
			for(List<Integer> list : in)
				numLinks += list.size();
	}
	
	@Override
	public int size()
	{
		return in.size();
	}

	@Override
	public long numLinks()
	{
		return numLinks;
	}

	@Override
	public DNode<String> node(String label)
	{
		int i = labels.indexOf(label);
		if(i == -1)
			return null;
			
		return new DiskDNode(i);
	}
	
	private class DiskDNode implements DNode<String>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;

		public DiskDNode(int index)
		{
			this.index = index;
		}
		
		@Override
		public String label()
		{
			check();
			if(nullLabels)
				return null;
			
			return labels.get(index);
		}

		@Override
		public void remove()
		{
			check();

			int linksRemoved = inDegree() + outDegree();
			linksRemoved -= linksOut(this).size();
			
			numLinks -= linksRemoved;
						
			for(int i : series(in.size()))
			{
				List<Integer> neighbors = new ArrayList<Integer>(in.get(i));
				
				Iterator<Integer> it = neighbors.iterator();
				while(it.hasNext())
					if(index == (int)it.next())
						it.remove();

				in.set(i, neighbors);
			}
			for(int i : series(out.size()))
			{
				List<Integer> neighbors = new ArrayList<Integer>(out.get(i));
								
				Iterator<Integer> it = neighbors.iterator();
				while(it.hasNext())
					if(index == (int)it.next())
						it.remove();
				
				out.set(i, neighbors);
			}
			
			// * move through all neighbor lists and decrement every index that 
			//   is higher than the one we just removed.  
			for(int i : series(in.size()))
			{
				List<Integer> neighbors = new ArrayList<Integer>(in.get(i));

				for(int j : series(neighbors.size()))
				{
					Integer value = neighbors.get(j);
					if(value > index)
						neighbors.set(j, value - 1);
				}
				
				in.set(i, neighbors);
			}
			for(int i : series(out.size()))
			{
				List<Integer> neighbors = new ArrayList<Integer>(out.get(i));

				for(int j : series(neighbors.size()))
				{
					Integer value = neighbors.get(j);
					if(value > index)
						neighbors.set(j, value - 1);
				}
				
				out.set(i, neighbors);
			}			
		
			in.remove((int)index);
			out.remove((int)index);
			if(! nullLabels)
				labels.remove((int)index);

			dead = true;
			modCount++;
			nodeModCount++;
			
			sorted = false;
		}

		private void check()
		{
			if(dead)
				throw new IllegalStateException("Node is dead (index was "+index+").");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation. The node objects should be re-requested from the graph object.");
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
		public Collection<? extends DNode<String>> neighbors()
		{
			check();
			
			Set<Integer> indices = new LinkedHashSet<Integer>();
			
			indices.addAll(in.get(this.index));
			indices.addAll(out.get(this.index));
			
			return new NodeList(new ArrayList<Integer>(indices));
		}

		@Override
		public DNode<String> neighbor(String label)
		{
			check();
			if(nullLabels)
			{
				if(degree() == 0)
					return null;
				else
					return neighbors().iterator().next();
			}	
			
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					return new DiskDNode(index);
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					return new DiskDNode(index);
			
			return null;
		}

		@Override
		public Collection<? extends DNode<String>> neighbors(String label)
		{
			check();
			if(nullLabels)
					return neighbors();
			
			Set<Integer> indices = new LinkedHashSet<Integer>();
	
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(new ArrayList<Integer>(indices));
		}

		@Override
		public Collection<? extends DNode<String>> out()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());
			
			for(int i : out.get(this.index))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<String>> out(String label)
		{
			check();
			if(nullLabels)
				return out();
			
			List<Integer> indices = new ArrayList<Integer>(outDegree());
			
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<String>> in()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());
			
			for(int index : in.get(this.index))
				indices.add(index);
			
			return new NodeList(indices);
		}

		@Override
		public Collection<? extends DNode<String>> in(String label)
		{
			check();
			
			if(nullLabels)
				return in();
			
			List<Integer> indices = new ArrayList<Integer>(inDegree());
			
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public DLink<String> connect(Node<String> to)
		{
			check();
			int fromIndex = index, toIndex = to.index();
			
			List<Integer> neighbors;
			neighbors = new ArrayList<Integer>(out.get(fromIndex));
			neighbors.add(toIndex);
			out.set(fromIndex, neighbors);
			
			neighbors = new ArrayList<Integer>(in.get(toIndex));
			neighbors.add(fromIndex);
			in.set(toIndex, neighbors);
			
			modCount++;			
			numLinks++;
			
			sorted = false;
			
			return new DiskDLink(index(), to.index());
		}

		@Override
		public void disconnect(Node<String> other)
		{
			check();
			
			int mine = index, his = other.index();
			
			int links = 0;
			
			List<Integer> myOut = new ArrayList<Integer>(out.get(mine));
			while(myOut.remove((Integer)his))
				links++;
			out.set(mine, myOut);
			
			List<Integer> hisOut = new ArrayList<Integer>(out.get(his));
			while(hisOut.remove((Integer)mine))
				links++;
			out.set(his, hisOut);
			
			List<Integer> myIn = new ArrayList<Integer>(in.get(mine));
			while(myIn.remove((Integer)his));
			in.set(mine, myIn);
				
			List<Integer> hisIn = new ArrayList<Integer>(in.get(his));
			while(hisIn.remove((Integer)mine));
			in.set(his, hisIn);

			numLinks -= links;			
			modCount++;
			
			sorted = false;
		}

		@Override
		public boolean connected(Node<String> other)
		{
			check();
			
			if(!(other instanceof DNode<?>))
				return false;
			
			DNode<String> o = (DNode<String>) other;
			
			return this.connectedTo(o) || o.connectedTo(this);
		}
		
		@Override
		public boolean connectedTo(DNode<String> to)
		{
			check();
			
			int mine = index, his = to.index();
			
			if(out.get(mine).contains(his))
				return true;
			
			return false;
		}

		@Override
		public DGraph<String> graph()
		{
			check();
			
			return DiskDGraph.this;
		}

		@Override
		public int index()
		{
			check();
			
			return index;
		}

		@Override
		public int inDegree()
		{
			check();
			
			return in.get(index).size();
		}

		@Override
		public int outDegree()
		{
			check();
			
			return out.get(index).size();
		}

		@Override
		public int hashCode()
		{
			check();
			
			final int prime = 31;
			int result = 1;
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((index == null) ? 0 : index.hashCode());
			if(! nullLabels)
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
			
			DiskDNode other = (DiskDNode) obj;
			
			if (graph() != other.graph())
				return false;
			if (dead != other.dead)
				return false;
			
			return index == other.index();
		}
		
		public String toString()
		{
			check();
			
			return label() == null ? ("n"+index()) : label().toString() + "_" +index();
		}

		@Override
		public List<DLink<String>> links()
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>(degree());
			
			for(int neighbor : out.get((int)index))
				list.add(new DiskDLink(index, neighbor));
			
			for(int neighbor : in.get((int)index))
				if(neighbor != ((int)index)) // no double reflexive links
					list.add(new DiskDLink(neighbor, index));	
			
			return list;
		}

		@Override
		public List<DLink<String>> linksOut()
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>(outDegree());
			for(int neighbor : out.get((int)index))
				list.add(new DiskDLink(index, neighbor));
			
			return list;
		}

		@Override
		public List<DLink<String>> linksIn()
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>(inDegree());
			for(int neighbor : in.get((int)index))
				list.add(new DiskDLink(neighbor, index));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<String>> links(Node<String> other)
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>();
			
			int o = other.index();
			for(int neighbor : out.get((int)index))
				if(neighbor == o)
					list.add(new DiskDLink(index, neighbor));
			
			if(index != o)
				for(int neighbor : in.get((int)index))
					if(neighbor == o)
						list.add(new DiskDLink(neighbor, index));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<String>> linksOut(DNode<String> other)
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>(outDegree());
			
			int o = other.index();
			for(int neighbor : out.get((int)index))
				if(((int)neighbor) == ((int)o))
					list.add(new DiskDLink(index, neighbor));
			
			return list;
		}

		@Override
		public Collection<? extends DLink<String>> linksIn(DNode<String> other)
		{
			check();
			
			List<DLink<String>> list = new ArrayList<DLink<String>>(inDegree());
			
			int o = other.index();
			for(int neighbor : in.get((int)index))
				if(neighbor == o)
					list.add(new DiskDLink(neighbor, index));
			
			return list;
		}
	}
	
	private class DiskDLink implements DLink<String>
	{
		private DNode<String> from, to;
		
		private long nodeModState = nodeModCount;
		
		private boolean dead = false;
		
		public DiskDLink(int from, int to)
		{
			this.from = new DiskDNode(from);
			this.to = new DiskDNode(to);
		}
		
		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		
		
		@Override
		public Collection<? extends Node<String>> nodes()
		{
			check();
			return Arrays.asList(from, to);
		}

		@Override
		public Graph<String> graph()
		{
			check();
			return DiskDGraph.this;
		}

		@Override
		public void remove()
		{
			check();
			
			List<Integer> list = new ArrayList<Integer>(in.get(to.index()));
			list.remove((Integer)from.index());
			in.set(to.index(), list);
			
			list = new ArrayList<Integer>(out.get(from.index()));
			list.remove((Integer)to.index());
			out.set(from.index(), list);
			
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
		public DNode<String> first()
		{
			check();
			return from;
		}

		@Override
		public DNode<String> second()
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
			DiskDLink other = (DiskDLink) obj;
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

		private DiskDGraph getOuterType()
		{
			return DiskDGraph.this;
		}
		
		public String toString()
		{
			check();
			return from + " -> " + to;
		}

		@Override
		public DNode<String> from()
		{
			return from;
		}

		@Override
		public DNode<String> to()
		{
			return to;
		}

		@Override
		public DNode<String> other(Node<String> current)
		{
			if(! first().equals(current))
				return first();
			return second();
		}
	}

	private class NodeList extends AbstractList<DNode<String>>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public DiskDNode get(int index)
		{
			return new DiskDNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}
	
	@Override
	public Collection<? extends DNode<String>> nodes(String label)
	{
		if(nullLabels)
		{
			if(label == null)
				return nodes();
			else
				return Collections.emptyList();
		}
		
		// * count the occurrences so that we can set the ArrayList's capacity 
		//   accurately
		int frq = 0;
		for(String l : labels)
			if(eq(l, label))
				frq++;
			
		List<Integer> indices = new ArrayList<Integer>(frq);
		
		for(int i : Series.series(size()))
			if(eq(labels.get(i), label))
				indices.add(i);
		
		return new NodeList(indices);
	}

	@Override
	public List<? extends DNode<String>> nodes()
	{
		return new NodeList(Series.series(size()));
	}
	
	@Override
	public Iterable<? extends DLink<String>> links()
	{
		return new LinkCollection();
	}
	
	/**
	 * A collection of all links in this graph.	 
	 * @author Peter
	 *
	 */
	private class LinkCollection extends AbstractCollection<DLink<String>>
	{
		@Override
		public Iterator<DLink<String>> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return (int)numLinks();
		}
		
		private class LLIterator implements Iterator<DLink<String>>
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
			public DLink<String> next()
			{
				check();
				read();
				
				if(buffer.isEmpty())
					throw new NoSuchElementException();
				
				Pair<Integer, Integer> pair = buffer.pop();
				
				return new DiskDLink(pair.first(), pair.second());
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}
			
			private void read()
			{
				if(next >= DiskDGraph.this.size())
					return;
				
				while(buffer.size() < BUFFER_LIMIT && next < DiskDGraph.this.size())
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
	public DNode<String> add(String label)
	{

		if(nullLabels && label != null)
			throw new IllegalArgumentException("Graph is set to null labels only.");
		
		if(! nullLabels)
			labels.add(label);
		
		in.add(new ArrayList<Integer>(2));
		out.add(new ArrayList<Integer>(2));
		
		sorted = false;
		return new DiskDNode(in.size() - 1);
	}

	@Override
	public Set<String> labels()
	{
		if(nullLabels)
		{
			Set<String> set = new HashSet<String>();
			set.add(null);
			return set;			
		}
		return new HashSet<String>(labels);
	}

	@Override
	public boolean connected(String from, String to)
	{
		if(nullLabels)
		{
			return numLinks() > 1;
		}
		
		for(DNode<String> a : nodes(from))
			for(DNode<String> b : nodes(to))
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
		
		Set<DNode<String>> nodes = new HashSet<DNode<String>>(nodes());
		
		for(DLink<String> link : links())
		{
			if(sb.length() != 9) 
				sb.append("; ");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}
		
		for(DNode<String> node : nodes)
			sb.append("; " + node);
		
		sb.append("}");
		
		return sb.toString();
	}
	
//	/**
//	 * Resets all neighbour list to their current capacity, plus the 
//	 * given margin. 
//	 * 
//	 * @param margin
//	 */
//	public void compact(int margin)
//	{
//		// Not necessary,
//	}
	
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
		{
			List<Integer> old = in.get(i);
			Collections.sort(old);
			
			in.set(i, old);
		}
		
		for(int i : Series.series(out.size()))
		{
			List<Integer> old = out.get(i);
			Collections.sort(old);
			
			out.set(i, old);
		}
		
		sorted = true;
	}
	
	
	
	/**
	 * Creates a copy of the given graph as a LightDGraph object. 
	 * 
	 * If the argument is undirectional, the link direction will be arbitrary.
	 * 
	 * @param graph
	 * @return
	 */
	public static DiskDGraph copy(Graph<String> graph, File db)
	{
		DiskDGraph copy = new DiskDGraph(db);
		for(Node<String> node : graph.nodes())
			copy.add(node.label());
		
		for(Link<String> link : graph.links())
			copy.get(link.first().index()).connect(copy.get(link.second().index()));
				
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
		    hash = 31 * hash + (get(i).label() == null ? 0 : get(i).label().hashCode());
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
		
		for(DNode<String> node : nodes())
		{
			DNode<Object> othNode = oth.get(node.index());
			
			if(! Functions.equals(node.label(), othNode.label()))
				return false;
			
			FrequencyModel<Integer> outs = new FrequencyModel<Integer>(),
			                        othOuts = new FrequencyModel<Integer>();
			for(DNode<String> neighbor : node.out())
				outs.add(neighbor.index());
			
			for(DNode<Object> othNeighbor : othNode.out())
				othOuts.add(othNeighbor.index());
			
			if(! outs.equals(othOuts))
				return false;
		}
		
		return true;
		
	}

	@Override
	public DNode<String> get(int i)
	{
		return nodes().get(i);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends DGraph<String>> level()
	{
		Object obj = DGraph.class;
		return (Class<? extends DGraph<String>>) obj;
	}

	@Override
	public List<DNode<String>> neighborsFast(Node<String> node)
	{
		if(node.graph() != this)
			throw new IllegalArgumentException("Cannot call with node from another graph.");
		
		int index = node.index();
		
		List<Integer> indices = concat(in.get(index), out.get(index));
		
		return new NodeList(indices);
	}
	
	/**
	 * Loads a previous converted graph.
	 * 
	 * @param dbFile
	 * @return
	 * @throws IOException
	 */
	public static DiskDGraph fromDB(File dbFile)
			throws IOException
	{
		DB db = DBMaker.fileDB(dbFile).make();

		if(db.exists("labels"))
		{
			db.close();
			return new DiskDGraph(dbFile, false);
		}
		
		db.close();
		return new DiskDGraph(dbFile, true);
	}
	
	/**
	 * Reads a (large) edgelist-encoded file into a DiskDGraph. 
	 *  
	 * @param file
	 * @param dir Where to put temporary files, used in reading the graph
	 * @return
	 */
	public static DiskDGraph fromFile(File file, File dir)
		throws IOException
	{
		int id = (new Random()).nextInt(10000000);

		return fromFile(file, dir, new File("graph."+id+".db"));	
	}
	
	public static DiskDGraph fromFile(File file, File tmpDir, File dbFile)
			throws IOException
		{
		DiskDGraph graph = new DiskDGraph(dbFile, true);

		// * sort the input file by first element
        File forward = new File(tmpDir, "forward.edgelist");
        
        
        List<File> files = ExternalSort.sortInBatch(
        		file, 
        		new LComp(true), ExternalSort.DEFAULTMAXTEMPFILES, 
        		Charset.defaultCharset(), tmpDir, false);
        ExternalSort.mergeSortedFiles(files, forward, new LComp(true), Charset.defaultCharset());
        
        System.out.println("Forward sort finished");
        
        readSorted(graph.out, forward, true);
        
        System.out.println("Forward list read");

        forward.delete();
        File backward = new File(tmpDir, "backward.edgelist");
        
        files = ExternalSort.sortInBatch(
        		file, 
        		new LComp(false), ExternalSort.DEFAULTMAXTEMPFILES, 
        		Charset.defaultCharset(), tmpDir, false);
        ExternalSort.mergeSortedFiles(files, backward, new LComp(false), Charset.defaultCharset());
        
        System.out.println("Backward sort finished");
        
        long links = readSorted(graph.in, backward, false);
        
        System.out.println("Backward list read");
        
        backward.delete();
        
        int max = Math.max(graph.in.size(), graph.out.size());
        while(graph.in.size() < max)
        	graph.in.add(Collections.EMPTY_LIST);
        while(graph.out.size() < max)
        	graph.out.add(Collections.EMPTY_LIST);

        graph.numLinks = links;
        graph.nullLabels = true;
        
		Global.log().info("Graph loaded and sorted.");

		return graph;
	}
	
	public void close()
	{
		db.atomicLong("numLinks").createOrOpen().set(numLinks);
		
		db.close();
	}
	
	private static long readSorted(List<List<Integer>> list, File file, boolean forward)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line;
		int i = 0;
		
		long links = 0;
		
		Integer current = 0;
		List<Integer> neighbors = new ArrayList<Integer>();
		
		do
		 {
			line = reader.readLine();
			i++;
		
			if(line == null)
				continue;
			if(line.trim().isEmpty())
				continue;
			if(line.trim().startsWith("#"))
				continue;
			if(line.trim().startsWith("%"))
				continue;
			
			String[] split = line.split("\\s");
			if(split.length < 2)
				throw new IllegalArgumentException("Line "+i+" does not split into two elements.");
			
			Integer a, b = null;
			try {
				a = Integer.parseInt(split[0]);
			} catch(NumberFormatException e)
			{
				throw new RuntimeException("The first element on line "+i+" ("+split[0]+") cannot be parsed into an integer.", e);
			}
			
			try {
				b = Integer.parseInt(split[1]);
			} catch(NumberFormatException e)
			{
				throw new RuntimeException("The second element on line "+i+" ("+split[1]+") cannot be parsed into an integer.", e);
			}
			
			if(! forward) // * Switch the two integers
			{
				int t = a;
				a = b;
				b = t;
			}
				
			if(current == null)
				current = a;
			
			if(a != (int) current)
			{
				try {
					list.add(neighbors);
				} catch(AssertionError e)
				{
					throw new AssertionError("Failed to add list to IndexTreeList. current list size: "+list.size()+", list to be added "+neighbors);
				}
				neighbors.clear();
				
				if(a < list.size())
					throw new IllegalStateException("Next index is "+a+", while list size is already " + list.size() + ". It seems like the sorting ot the file went wrong.");
				
				while(list.size() < a)
					list.add(Collections.EMPTY_LIST);
				
				current = a;
			}
			
			neighbors.add(b);
			links++;
			
			if(links % 50000 == 0)
				Global.log().info((forward? "Forward ":"Backward ")+": loaded " + links + " links.");
			
		} while(line != null);
		
		list.add(neighbors);
		
		Global.log().info((forward? "Forward ":"Backward ")+": loaded " + links + " links.");

		
		return links;
	}
	
	private static class LComp implements Comparator<String>
	{

		private boolean forward;
		
		public LComp(boolean forward) 
		{
			this.forward = forward;
		}

		@Override
		public int compare(String s1, String s2) 
		{
			boolean s1c = comment(s1), s2c = comment(s2);
			
			if(s1c && s2c) return 0;
			if(s1c && !s2c) return -1;
			if(s2c) return 1;
			
			int ind = forward ? 0 :1;
			int i1 = Integer.parseInt(s1.split("\\s")[ind]);
			int i2 = Integer.parseInt(s2.split("\\s")[ind]);
			
			return Integer.compare(i1, i2);
		}
	
		private boolean comment(String s)
		{
			return s.trim().startsWith("#") || s.trim().startsWith("%");
		}
	}
	
	/** 
	 * Copy of the INT_ARRAY serializer that takes an integer list as argument instead
	 */
	public static class SerializerIntList extends GroupSerializerObjectArray<List<Integer>> {

	    @Override
	    public void serialize(DataOutput2 out, List<Integer> value) 
	    		throws IOException 
	    {
	        out.packInt(value.size());
	        for (int c : value)
	            out.writeInt(c);
	    }

	    @Override
	    public List<Integer> deserialize(DataInput2 in, int available)
	    		throws IOException 
	    {
	        final int size = in.unpackInt();
	        
	        final int[] ret = new int[size];
	        for (int i = 0; i < size; i++) 
	            ret[i] = in.readInt();
	        
	        return list(ret);
	    }
	    
	    @Override
	    public boolean isTrusted() 
	    {
	        return true;
	    }

	    @Override
	    public boolean equals(List<Integer> a1, List<Integer> a2) 
	    {
	        return a1.equals(a2);
	    }

	    @Override
	    public int hashCode(List<Integer> ints, int seed) {
	        for (int i : ints) {
	            seed = (-1640531527) * seed + i;
	        }
	        return seed;
	    }

	    @Override
	    public int compare(List<Integer> o1, List<Integer> o2) {
	    	
	        if (o1 == o2) return 0;
	        
	        final int len = Math.min(o1.size(), o2.size());
	        
	        for (int i = 0; i < len; i++) {
	            if (o1.get(i) == o2.get(i))
	                continue;
	            if (o1.get(i) > o2.get(i))
	                return 1;
	            return -1;
	        }
	        return SerializerUtils.compareInt(o1.size(), o2.size());
	    }

	    @Override
	    public List<Integer> nextValue(List<Integer> in) {
	        final int[] value = array(in);

	        for (int i = value.length-1; ;i--) 
	        {
	            int b1 = value[i];
	            if(b1 == Integer.MAX_VALUE)
	            {
	                if(i==0)
	                    return null;
	                
	                value[i]=Integer.MIN_VALUE;
	                continue;
	            }
	            
	            value[i] = b1 + 1;
	            
	            return list(value);
	        }			
	    }
	    
		private static int[] array(List<Integer> in)
		{
			int[] res = new int[in.size()];
			for(int i : series(in.size()))
				res[i] = in.get(i);
			return res;
		}
	    
	    private static List<Integer> list(final int[] in)
	    {
	        return new AbstractList<Integer>() {
				@Override
				public Integer get(int index) {
					return in[index];
				}

				@Override
				public int size() {
					return in.length;
				}
			};
	    }

	}
}
