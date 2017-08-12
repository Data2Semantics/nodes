package org.nodes;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * NOTE: Buggy implementation. Do not use until tested more thoroughly.
 * 
 * @param <L>
 * @param <T>
 */
public class LightDTGraph<L,T> implements DTGraph<L, T> {
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;

	private List<L> labels;

	private List<List<Integer>> out;
	private List<List<Integer>> in;	

	private List<List<T>> outTags;
	private List<List<T>> inTags;

	private long numLinks = 0;
	private long modCount = 0;

	// * changes for any edit which causes the node indices to change 
	//   (currently just removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;

	private boolean sorted = false;

	public LightDTGraph()
	{
		this(16);
	}

	public LightDTGraph(int capacity)
	{
		out = new ArrayList<List<Integer>>(capacity);
		in = new ArrayList<List<Integer>>(capacity); 

		outTags = new ArrayList<List<T>>(capacity);
		inTags = new ArrayList<List<T>>(capacity); 

		labels = new ArrayList<L>(capacity);
	}


	/**
	 * returns all the tags in a set, slow, since it iterates over all links
	 * 
	 */
	@Override
	public Set<T> tags() {
		Set<T> tags = new HashSet<T>();

		for (List<T> tList : outTags) {
			tags.addAll(tList);
		}
		for (List<T> tList : inTags) {
			tags.addAll(tList);
		}
		return tags;
	}

	@Override
	public int size() {
		return labels.size();
	}

	@Override
	public DTNode<L, T> node(L label) {
		int i = labels.indexOf(label);
		if(i == -1)
			return null;

		return new LightDTNode(i);
	}

	@Override
	public Collection<? extends DTNode<L, T>> nodes(L label) {
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
	public List<? extends DTNode<L, T>> nodes() {
		return new NodeList(Series.series(size()));
	}

	@Override
	public DTNode<L, T> get(int i) {
		return nodes().get(i);
	}

	@Override
	public Iterable<? extends DTLink<L, T>> links() {
		return new LinkCollection();
	}

	@Override
	public DTNode<L, T> add(L label) {
		labels.add(label);

		in.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		out.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));

		inTags.add(new ArrayList<T>(NEIGHBOR_CAPACITY));
		outTags.add(new ArrayList<T>(NEIGHBOR_CAPACITY));

		sorted = false;
		return new LightDTNode(in.size() - 1);
	}

	@Override
	public long numLinks() {
		return numLinks;
	}

	@Override
	public Set<L> labels() {
		return new HashSet<L>(labels);
	}

	@Override
	public boolean connected(L from, L to) {
		for(DTNode<L,T> a : nodes(from))
			for(DTNode<L,T> b : nodes(to))
				if(a.connected(b))
					return true;

		return false;
	}

	@Override
	public long state() {
		return modCount;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends DTGraph<L, T>> level() {
		Object obj = DTGraph.class;
		return (Class<? extends DTGraph<L,T>>) obj;
	}


	private class LightDTNode implements DTNode<L,T>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;
		private int theHashCode;
		
		public LightDTNode(int index)
		{
			this.index = index;
			this.theHashCode = hashDude();
		}

		@Override
		public L label()
		{
			check();
			return labels.get(index);
		}


		@Override @Deprecated 
		/**
		 * remove this node form the graph. This is an expensive operation.
		 * 
		 * BUG IN IMPLEMENTATION, BEWARE
		 */
		public void remove()
		{
			check();

			for (int j = 0; j < out.size(); j++) {
				List<Integer> nb = out.get(j);
				List<T> nbT = outTags.get(j);
				for (int i = 0; i < nb.size(); i++) {
					if (nb.get(i).equals(index)) {
						nbT.remove(i);
						nb.remove(i);
						numLinks--;
					}
				}
			}

			for (int j = 0; j < in.size(); j++) {
				List<Integer> nb = in.get(j);
				List<T> nbT = inTags.get(j);
				for (int i = 0; i < nb.size(); i++) {
					if (nb.get(i).equals(index)) {
						nbT.remove(i);
						nb.remove(i);
						numLinks--;
					}
				}
			}

			in.remove((int)index);
			out.remove((int)index);

			inTags.remove((int)index);
			outTags.remove((int)index);

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
		public Collection<? extends DTNode<L,T>> neighbors()
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
		public DTNode<L,T> neighbor(L label)
		{
			check();
			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					return new LightDTNode(index);
			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					return new LightDTNode(index);

			return null;
		}

		@Override
		public Collection<? extends DTNode<L,T>> neighbors(L label)
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
		public Collection<? extends DTNode<L,T>> out()
				{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());

			for(int i : out.get(this.index))
				indices.add(i);

			return new NodeList(indices);
				}

		@Override
		public Collection<? extends DTNode<L,T>> out(L label)
				{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());

			for(int i : out.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);

			return new NodeList(indices);
				}

		@Override
		public Collection<? extends DTNode<L,T>> in()
				{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());

			for(int index : in.get(this.index))
				indices.add(index);

			return new NodeList(indices);
				}

		@Override
		public Collection<? extends DTNode<L,T>> in(L label)
				{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());

			for(int i : in.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);

			return new NodeList(indices);
				}


		@Override
		public DTLink<L,T> connect(Node<L> to)
		{
			return connect((TNode<L,T>) to, null);
		}	

		@Override
		public DTLink<L, T> connect(TNode<L, T> other, T tag) {
			check();
			int fromIndex = index, toIndex = other.index();

			out.get(fromIndex).add(toIndex);
			in.get(toIndex).add(fromIndex);

			outTags.get(fromIndex).add(tag);
			inTags.get(toIndex).add(tag);

			// Collections.sort(out.get(fromIndex));
			// Collections.sort(out.get(toIndex));

			modCount++;			
			numLinks++;

			sorted = false;

			return new LightDTLink(fromIndex, toIndex, outTags.get(fromIndex).size()-1, false);
		}

		@Override
		public void disconnect(Node<L> other)
		{
			check();
			
			int mine = index, his = other.index();

			int links = 0;

			List<Integer> nb = out.get(mine);
			List<T> nbT = outTags.get(mine);

			for (int i = 0; i < nb.size(); i++) {
				if (nb.get(i).equals(his)) {
					nbT.remove(i);
					nb.remove(i);
					links++;
				}
			}

			nb = out.get(his);
			nbT = outTags.get(his);

			for (int i = 0; i < nb.size(); i++) {
				if (nb.get(i).equals(mine)) {
					nbT.remove(i);
					nb.remove(i);
					links++;
				}
			}

			nb = in.get(mine);
			nbT = inTags.get(mine);

			for (int i = 0; i < nb.size(); i++) {
				if (nb.get(i).equals(his)) {
					nbT.remove(i);
					nb.remove(i);
				}
			}

			nb = in.get(his);
			nbT = inTags.get(his);

			for (int i = 0; i < nb.size(); i++) {
				if (nb.get(i).equals(mine)) {
					nbT.remove(i);
					nb.remove(i);
				}
			}

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
		public boolean connected(TNode<L, T> other, T tag) {
			if(!(other instanceof DTNode<?,?>))
				return false;

			DTNode<L,T> o = (DTNode<L,T>) other;

			return this.connectedTo(o, tag) || o.connectedTo(this, tag);
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
		public boolean connectedTo(TNode<L, T> other, T tag) {
			int mine = index;
			Integer his = other.index();

			List<Integer> nb = out.get(mine);
			List<T> nbT = outTags.get(mine);

			for (int i = 0; i < nb.size(); i++) {
				if (nb.get(i).equals(his) && nbT.get(i).equals(tag)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public DTGraph<L,T> graph()
		{
			return LightDTGraph.this;
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
			return this.theHashCode;
		}
		
		private int hashDude() {
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
			LightDTNode other = (LightDTNode) obj;
			if (this.theHashCode != other.theHashCode)
				return false;
			if (getOuterType() != other.getOuterType()) // They don't come from the same graph object (checking for graph equality here is really, really, really slow)
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
		

		private LightDTGraph getOuterType()
		{
			return LightDTGraph.this;
		}

		public String toString()
		{
			return label() == null ? ("n"+index()) : label().toString() + "_" +index();
		}

		@Override
		public List<DTLink<L,T>> links()
		{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(degree());
			List<Integer> nb = out.get((int) index);
			for(int i = 0; i < nb.size(); i++)
				list.add(new LightDTLink(index, (int) nb.get(i), i, false));

			nb = in.get((int) index);
			for(int i = 0; i < nb.size(); i++)
				if((int) nb.get(i) != (int) index) // No double reflexive links
					list.add(new LightDTLink((int) nb.get(i), index, i, true));	

			return list;
		}

		@Override
		public List<DTLink<L,T>> linksOut()
		{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(outDegree());
			List<Integer> nb = out.get((int) index);
			for(int i = 0; i < nb.size(); i++)
				list.add(new LightDTLink(index, (int) nb.get(i), i, false));

			return list;
		}

		@Override
		public List<DTLink<L,T>> linksIn()
		{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(inDegree());
			List<Integer> nb = in.get((int) index);
			for(int i = 0; i < nb.size(); i++)
				list.add(new LightDTLink((int) nb.get(i), index, i, true));

			return list;
		}

		@Override
		public Collection<? extends DTLink<L,T>> links(Node<L> other)
				{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(degree());

			int o = other.index();
			List<Integer> nb = out.get(index);

			for (int i = 0; i < nb.size(); i++)  {
				if ((int) nb.get(i) == o) {
					list.add(new LightDTLink(index, (int) nb.get(i), i, false));
				}
			}

			o = other.index();
			nb = in.get(index);

			for (int i = 0; i < nb.size(); i++)  {
				if ((int) nb.get(i) != (int) index && (int) nb.get(i) == o) { // no double reflexive
					list.add(new LightDTLink((int) nb.get(i), index, i, true));
				}
			}
			return list;
				}

		@Override
		public Collection<? extends DTLink<L,T>> linksOut(DNode<L> other)
				{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(outDegree());

			int o = other.index();
			List<Integer> nb = out.get(index);

			for (int i = 0; i < nb.size(); i++)  {
				if ((int) nb.get(i) == o) {
					list.add(new LightDTLink(index, (int) nb.get(i), i, false));
				}
			}		
			return list;
				}

		@Override
		public Collection<? extends DTLink<L,T>> linksIn(DNode<L> other)
				{
			List<DTLink<L,T>> list = new ArrayList<DTLink<L,T>>(inDegree());

			int o = other.index();
			List<Integer> nb = in.get(index);

			for (int i = 0; i < nb.size(); i++)  {
				if ((int) nb.get(i) == o) {
					list.add(new LightDTLink((int) nb.get(i), index, i, true));
				}
			}		
			return list;
				}



		@Override
		public TLink<L, T> link(TNode<L, T> other) {
			if (out.get((int) index).contains(new Integer(other.index()))) {
				return new LightDTLink(index, other.index(), out.get((int) index).indexOf(new Integer(other.index())), false);
			} else if (in.get((int) index).contains(new Integer(other.index()))) {
				return new LightDTLink(other.index(), index, in.get((int) index).indexOf(new Integer(other.index())), true);
			}
			return null;
		}

		@Override
		public Collection<T> tags() {
			List<T> tags = new ArrayList<T>(degree());

			tags.addAll(outTags.get((int) index));
			tags.addAll(inTags.get((int) index));

			return tags;
		}

		@Override
		public Collection<? extends DTNode<L, T>> toTag(T tag) {
			List<DTNode<L,T>> list = new ArrayList<DTNode<L,T>>();
			List<Integer> nb = out.get((int) index);
			List<T> nbT = outTags.get((int) index);

			for (int i = 0; i < nbT.size(); i++) {
				if (nbT.get(i).equals(tag)) {
					list.add(new LightDTNode(nb.get(i)));
				}
			}
			return list;
		}

		@Override
		public Collection<? extends DTNode<L, T>> fromTag(T tag) {
			List<DTNode<L,T>> list = new ArrayList<DTNode<L,T>>();
			List<Integer> nb = in.get((int) index);
			List<T> nbT = inTags.get((int) index);

			for (int i = 0; i < nbT.size(); i++) {
				if (nbT.get(i).equals(tag)) {
					list.add(new LightDTNode(nb.get(i)));
				}
			}
			return list;
		}

	}

	private class LightDTLink implements DTLink<L,T>
	{
		private DTNode<L,T> from, to;
		private int tagIndex;
		private boolean toIndex;
		private int theHashCode;


		private long nodeModState = nodeModCount;

		private boolean dead = false;

		public LightDTLink(int from, int to, int tagIndex, boolean toIndex)
		{
			this.from = new LightDTNode(from);
			this.to = new LightDTNode(to);
			this.tagIndex = tagIndex;
			this.toIndex = toIndex;
			this.theHashCode = hashDude();
		}

		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");

			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		

		@Override
		public Collection<? extends DTNode<L,T>> nodes()
				{
			check();
			return Arrays.asList(from, to);
				}

		@Override
		public DTGraph<L,T> graph()
		{
			check();
			return LightDTGraph.this;
		}


		/**
		 * This is an expensive operation since one direction of the link has to be looked up in a loop
		 * 
		 */
		@Override
		public void remove()
		{
			check();

			// Look up the other index
			if (!toIndex) { 
				int tagIndex2 = 0;
				T tag = outTags.get(from.index()).get(tagIndex);
				List<Integer> nb = in.get(to.index());
				List<T> nbT = inTags.get(to.index());
				for (int i = 0; i < nb.size(); i++) {
					if (nb.get(i).equals(new Integer(to.index())) && nbT.get(i).equals(tag)) {
						tagIndex2 = i;
					}
				}
				inTags.get(to.index()).remove(tagIndex2);
				outTags.get(from.index()).remove(tagIndex);			

				in.get(to.index()).remove(tagIndex2);
				out.get(from.index()).remove(tagIndex);			

			} else {
				int tagIndex2 = 0;
				T tag = inTags.get(to.index()).get(tagIndex);
				List<Integer> nb = out.get(from.index());
				List<T> nbT = outTags.get(from.index());
				for (int i = 0; i < nb.size(); i++) {
					if (nb.get(i).equals(new Integer(to.index())) && nbT.get(i).equals(tag)) {
						tagIndex2 = i;
					}
				}
				inTags.get(to.index()).remove(tagIndex);
				outTags.get(from.index()).remove(tagIndex2);			

				in.get(to.index()).remove(tagIndex);
				out.get(from.index()).remove(tagIndex2);				
			}

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
		public DTNode<L,T> first()
		{
			check();
			return from;
		}

		@Override
		public DTNode<L,T> second()
		{
			check();
			return to;
		}


		@Override public int hashCode() { return this.theHashCode; }
		
		public int hashDude()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			T tagThis = (toIndex) ? inTags.get(to.index()).get(tagIndex) : outTags.get(from.index()).get(tagIndex);
			result = prime * result + ((tagThis == null) ? 0 : tagThis.hashCode());
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
			LightDTLink other = (LightDTLink) obj;		
			if (this.theHashCode != other.theHashCode)
				return false;		
			if (getOuterType() != other.getOuterType()) // They don't come from the same graph object (checking for graph equality here is really, really, really slow)
				return false;
			if (dead != other.dead)
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null)	{
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			T tagThis = (toIndex) ? inTags.get(to.index()).get(tagIndex) : outTags.get(from.index()).get(tagIndex);
			T tagOther = (other.toIndex) ? inTags.get(other.to.index()).get(other.tagIndex) : outTags.get(other.from.index()).get(other.tagIndex);
			if (!tagThis.equals(tagOther)) {
				return false;
			}
			return true;
		}
		

		private LightDTGraph getOuterType()
		{
			return LightDTGraph.this;
		}

		public String toString()
		{
			check();
			return from + " -> " + to + ((tag()==null) ? "" : " [label=" + tag() + "]");
		}

		@Override
		public DTNode<L,T> from()
		{
			return from;
		}

		@Override
		public DTNode<L,T> to()
		{
			return to;
		}

		@Override
		public DTNode<L,T> other(Node<L> current)
		{
			if(first() != current)
				return first();
			return second();
		}

		@Override
		public T tag() {
			check();
			if (!toIndex) {
				return outTags.get(from.index()).get(tagIndex);
			} else {
				return inTags.get(to.index()).get(tagIndex);
			}
		}
	}

	private class NodeList extends AbstractList<DTNode<L,T>>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public LightDTNode get(int index)
		{
			return new LightDTNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}

	private class LinkCollection extends AbstractCollection<DTLink<L,T>>
	{
		@Override
		public Iterator<DTLink<L,T>> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return (int)numLinks;
		}

		private class LLIterator implements Iterator<DTLink<L,T>>
		{
			private static final int BUFFER_LIMIT = 5;
			private long graphState = state();
			private Deque<Pair<Pair<Integer, Integer>,Integer>> buffer = new LinkedList<Pair<Pair<Integer, Integer>,Integer>>();
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
			public DTLink<L,T> next()
			{
				check();
				read();

				if(buffer.isEmpty())
					throw new NoSuchElementException();

				Pair<Pair<Integer, Integer>,Integer> pair = buffer.pop();

				return new LightDTLink(pair.first().first(), pair.first().second(), pair.second(), false);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}

			private void read()
			{
				if(next >= LightDTGraph.this.size())
					return;

				while(buffer.size() < BUFFER_LIMIT && next < LightDTGraph.this.size())
				{
					int from = next;

					List<Integer> tos = out.get(from);

					for (int i = 0; i < tos.size(); i++)
						buffer.add(new Pair<Pair<Integer,Integer>,Integer>(new Pair<Integer, Integer>(from, tos.get(i)), i));

					next++;
				}			
			}
		}	
	}

	private boolean eq(Object a, Object b)
	{
		if(a == null && b == null)
			return true;

		if(a == null || b == null)
			return false;

		return a.equals(b);
	}

	///*
	public boolean equals(Object other)
	{	
		if(!(other instanceof DTGraph<?,?>))
			return false;

		DTGraph<Object,Object> oth = (DTGraph<Object,Object>) other;
		if(! oth.level().equals(level()))
			return false;

		if(size() != oth.size())
			return false;

		if(numLinks() != oth.numLinks())
			return false;

		if(labels().size() != oth.labels().size())
			return false;

		for(DTNode<L,T> node : nodes())
		{
			DTNode<Object,Object> othNode = oth.get(node.index());

			if(! Functions.equals(node.label(), othNode.label()))
				return false;

			
			if (node.tags().size() != othNode.tags().size())
				return false;
			
			for (T tag : node.tags()) {

				FrequencyModel<Integer> outs = new FrequencyModel<Integer>(),
						othOuts = new FrequencyModel<Integer>();
		
				for(DTNode<L,T> neighbor : node.toTag(tag))
					outs.add(neighbor.index());

				for(DTNode<Object,Object> othNeighbor : othNode.toTag(tag))
					othOuts.add(othNeighbor.index());

				if(! outs.equals(othOuts))
					return false;
			}
		}
		return true;
	}
	//*/

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
	
}
