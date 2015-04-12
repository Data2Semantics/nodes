package org.nodes;

import static org.nodes.util.Series.series;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.nodes.util.FrequencyModel;
import org.nodes.util.Functions;
import org.nodes.util.Pair;
import org.nodes.util.Series;



/**
 * <p>
 * DTGraph implementation based on hashmaps. Each node stores its neighbours in a
 * hashmap. 
 * </p><p>
 * Graph traversal by labels is close to linear (in the same sense that retrieval
 * from a hasmap is close to constant). Memory usage is relatively inefficient.  
 * </p><p>
 * Because MapDTGraph inherits from Graph, it is possible to connect two nodes 
 * without specifying a tag for the resulting link. In this case the tag gets 
 * the value null.
 * </p><p>
 * Self loops and multiple edges are allowed.
 * </p>
 * 
 * @author peter
 *
 * @param <L>
 */
public class MapDTGraph<L, T> implements DTGraph<L, T>
{
	protected List<MapDTNode> nodeList = new ArrayList<MapDTNode>();
	protected Map<L, Set<MapDTNode>> nodes = new LinkedHashMap<L, Set<MapDTNode>>();

	protected int numEdges = 0;
	protected long modCount = 0;
	
	protected int hash;
	protected Long hashMod;
	
	public MapDTGraph()
	{
	}
	
	/**
	 * Returns a graph with the same structure and labels as that in the 
	 * argument.
	 * 
	 * @param graph
	 * @return
	 */
	public static <L, T> MapDTGraph<L, T> copy(DTGraph<L, T> graph)
	{	
		MapDTGraph<L, T> copy = new MapDTGraph<L, T>();
		for(DTNode<L, T> node : graph.nodes())
			copy.add(node.label());

		for(DTLink<L, T> link : graph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.nodes().get(i).connect(copy.nodes().get(j), link.tag());
		}
		
		return copy;
	}
	
	/**
	 * Returns a copy of the input. All tags are null.
	 * @param graph
	 * @return
	 */
	public static <L> MapDTGraph<L, Object> copy(DGraph<L> graph)
	{	
		MapDTGraph<L, Object> copy = new MapDTGraph<L, Object>();
		for(DNode<L> node : graph.nodes())
			copy.add(node.label());

		for(DLink<L> link : graph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.nodes().get(i).connect(copy.nodes().get(j), null);
		}
		
		return copy;
	}
	
	private class MapDTNode implements DTNode<L, T>
	{
		private Map<T, List<MapDTLink>> linksOut = new LinkedHashMap<T, List<MapDTLink>>();
		private Map<T, List<MapDTLink>> linksIn = new LinkedHashMap<T, List<MapDTLink>>();
		
		private Set<MapDTNode> neighborsTo = new LinkedHashSet<MapDTNode>();
		private Set<MapDTNode> neighborsFrom = new LinkedHashSet<MapDTNode>();
		
		private L label;
		
		// * A node is dead when it is removed from the graph. Since there is no
		//   way to ensure that clients don't maintain copies of node objects we 
		//   keep check of nodes that are no longer part of the graph. 
		private boolean dead = false;
		
		private Integer labelId = null;
		private Long labelIdMod;
		private int index;
				
		public MapDTNode(L label)
		{
			this.label = label;
			
			index = nodeList.size();
			
			// * The node adds itself to the graph's data structures
			nodeList.add(this);
			
			if(! nodes.containsKey(label))
				nodes.put(label, new LinkedHashSet<MapDTNode>());
			nodes.get(label).add(this);
		}

		@Override
		public Collection<MapDTNode> neighbors()
		{
			Set<MapDTNode> set = new LinkedHashSet<MapDTNode>();
			set.addAll(neighborsTo);
			set.addAll(neighborsFrom);
			
			return set;
		}

		@Override
		public MapDTNode neighbor(L label)
		{
			for(MapDTNode node : neighborsTo)
				if((label == null && node.label == null)
						|| (label != null && node.label().equals(label)))
					return node;

			for(MapDTNode node : neighborsFrom)
				if((label == null && node.label == null) 
						|| (label != null && node.label().equals(label)))
					return node;
			
			return null;
		}

		@Override
		public L label()
		{
			return label;
		}

		@Override
		public Set<MapDTNode> neighbors(L label)
		{
			Set<MapDTNode> result = new LinkedHashSet<MapDTNode>();
			for(MapDTNode node : neighborsTo)
				if((label == null && node.label == null)
						|| (label != null && label.equals(node.label)))
					result.add(node);
			for(MapDTNode node : neighborsFrom)
				if((label == null && node.label == null) 
						|| (label != null && label.equals(node.label)))
					result.add(node);
			
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public MapDTLink connect(Node<L> other)
		{
			if(MapDTGraph.this != other.graph())
				throw new IllegalArgumentException("Can only connect nodes that belong to the same graph.");
			
			// - This cast is safe because we know the node belongs to this 
			//   graph, so it was made by this graph, so it's a MapDTNode
			return connect((TNode<L, T>)other, null);
		}

		@Override
		public MapDTLink connect(TNode<L, T> other, T tag)
		{
			if(this.graph().hashCode() != other.graph().hashCode())
				throw new IllegalArgumentException("Can only connect to nodes from the same graph (arguments: this="+this+", other="+other+")");
			
			// * This graph can only contain MapDTNodes, so this is a safe cast
			MapDTNode mdtOther = (MapDTNode)other;
			
//			if(connected(mdtOther, tag))
//				return;
			
			MapDTLink link = new MapDTLink(tag, this, mdtOther);
			
			// * Add other as a 'to' node in this node's neighbors
			if(! linksOut.containsKey(tag))
				linksOut.put(tag, new LinkedList<MapDTLink>());
			linksOut.get(tag).add(link);
			
			// * Add this as a 'from' node in other's neighbors
			if(! mdtOther.linksIn.containsKey(tag))
				mdtOther.linksIn.put(tag, new LinkedList<MapDTLink>());
			mdtOther.linksIn.get(tag).add(link);
			
			neighborsTo.add(mdtOther);
			mdtOther.neighborsFrom.add(this);
			
			numEdges++;
			modCount++;
			
			return link;
		}

		@Override
		public void disconnect(Node<L> other)
		{	
			if(MapDTGraph.this != other.graph())
				throw new IllegalArgumentException("Can only disconnect nodes that belong to the same graph.");

			if(!connected(other))
				return;
			
			MapDTNode mdtOther = (MapDTNode)other;
			
			int removed = 0;
			
			List<MapDTLink> toRemove = new LinkedList<MapDTLink>();
			for(T tag : linksOut.keySet())
			{
				for(MapDTLink link : linksOut.get(tag))
					if(link.second().equals(mdtOther))
						toRemove.add(link);
			}
			/*
			for(T tag : linksIn.keySet())
			{
				for(MapDTLink link : linksIn.get(tag))
					if(link.first().equals(mdtOther))
						toRemove.add(link);
			}
			*/
			
			for(MapDTLink link : toRemove) {
				link.remove();
				removed++;
			}
						
			if(removed > 0)
				modCount++;	
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
		public boolean connectedTo(DNode<L> other)
		{
			return neighborsTo.contains(other);
		}

		@Override
		public DTGraph<L, T> graph()
		{
			return MapDTGraph.this;
		}
		
		public int id()
		{
			return ((Object) this).hashCode();
		}
		
		/** 
		 * An id to identify this node among nodes with the same label.
		 * @return
		 */
		public int labelId()
		{
			if(labelIdMod == null || labelIdMod != modCount)
			{
				Collection<MapDTNode> others = nodes.get(label);
				
				int i = 0;
				for(MapDTNode other : others)
				{
					if(other.equals(this))
					{
						labelId = i;
						break;
					}
					i++;
				}
				labelIdMod = modCount;
			}	
			return labelId;
		
		}
		
		public String toString()
		{
			boolean unique = nodes.get(label).size() <= 1;

			return label + (unique ? "" : "_" + labelId());
		}
		
		/**
		 * Since clients can maintain links to nodes that have been removed 
		 * from the graph, there is a danger of these nodes being used and 
		 * causing mayhem. 
		 * 
		 * To prevent such situations we will explicitly give such nodes a state 
		 * of 'dead'. Using dead nodes in any way (except calling this method) 
		 * can result in an IllegalStateException
		 * 
		 * @return
		 */
		public boolean dead()
		{
			return dead;
		}
		
		/**
		 * Removes this node from the graph
		 */
		public void remove()
		{	
			// * Disconnect from the graph
			// ** Copy the nodes over to avoid a concurrentmodificationexception
			List<MapDTNode> ns = new ArrayList<MapDTNode>(neighborsTo);
			for(MapDTNode to: ns)
				disconnect(to);
			
			ns = new ArrayList<MapDTNode>(neighborsFrom);
			for(MapDTNode from : ns)
				from.disconnect(this);
			
			nodeList.remove(this);
			nodes.get(label).remove(this);
			
			updateIndices();
			
			// * kill the node
			dead = true;	
			
			// * disconnect() won't increment the modcount if this node was 
			//   already orphaned  
			modCount ++;
		}

		@Override
		public Set<MapDTNode> out()
		{
			return Collections.unmodifiableSet(neighborsTo);
		}

		@Override
		public Set<MapDTNode> out(L label)
		{
			Set<MapDTNode> set = new LinkedHashSet<MapDTNode>();
			for(MapDTNode node : neighborsTo)
				if( (label == null && node.label() == null) 
						|| (label != null && label.equals(node.label)))
					set.add(node);
					
			return set; 
		}

		@Override
		public Set<? extends DTNode<L, T>> in()
		{
			return Collections.unmodifiableSet(neighborsFrom);
		}

		@Override
		public Set<? extends DTNode<L, T>> in(L label)
		{
			Set<MapDTNode> set = new LinkedHashSet<MapDTNode>();
			for(MapDTNode node : neighborsFrom)
				if( (label == null && node.label() == null) || (label != null && label.equals(node.label)))
					set.add(node);
					
			return set; 
		}

		@Override
		public int index()
		{
			return index;
		}

		@Override
		public DTLink<L, T> link(TNode<L, T> other)
		{			
			MapDTNode o = (MapDTNode) other;
			
			if(!connected(o))
				return null;
			
			for(T tag : linksOut.keySet())
				for(MapDTLink link : linksOut.get(tag))
					if(link.second().equals(o))
						return link;
			return null;
		}

		@Override
		public boolean connected(TNode<L, T> other, T tag)
		{
			if(!(other instanceof MapDTGraph<?, ?>.MapDTNode))
				return false;
			
			MapDTNode o = (MapDTNode) other;
			
			return this.connectedTo(o, tag) || o.connectedTo(this, tag);
		}
		
		@Override
		public boolean connectedTo(TNode<L, T> other, T tag)
		{
			if(! linksOut.containsKey(tag))
				return false;
			
			for(MapDTLink link : linksOut.get(tag))
				if(link.to().equals(other))
					return true;
				
			return false;
		}

		@Override
		public Collection<MapDTNode> toTag(T tag)
		{
			List<MapDTNode> nodes = new LinkedList<MapDTNode>();
			
			if(! linksOut.containsKey(tag))
				return nodes;
			
			for(MapDTLink link : linksOut.get(tag))
				nodes.add((MapDTNode)link.second());
				
			return nodes;
		}

		@Override
		public Collection<MapDTNode> fromTag(T tag)
		{
			List<MapDTNode> nodes = new LinkedList<MapDTNode>();
			
			if(! linksIn.containsKey(tag))
				return nodes;
			
			for(MapDTLink link : linksIn.get(tag))
				nodes.add((MapDTNode)link.first());
				
			return nodes;
		}

		@Override
		public int inDegree()
		{
			int n = 0;
			for(T tag : linksIn.keySet())
				n += linksIn.get(tag).size();
			
			return n;
		}

		@Override
		public int outDegree()
		{
			int n = 0;
			for(T tag : linksOut.keySet())
				n += linksOut.get(tag).size();
			
			return n;
		}

		@Override
		public int degree()
		{
			return inDegree() + outDegree();
		}

		@Override
		public List<DTLink<L, T>> links()
		{
			List<DTLink<L, T>> list = new ArrayList<DTLink<L,T>>(degree());
			for(T tag : linksOut.keySet())
				list.addAll(linksOut.get(tag));
			
			for(T tag : linksIn.keySet())
				list.addAll(linksIn.get(tag));
			
			return list;
		}

		@Override
		public List<DTLink<L, T>> linksOut()
		{
			List<DTLink<L, T>> list = new ArrayList<DTLink<L,T>>(outDegree());
			for(T tag : linksOut.keySet())
				list.addAll(linksOut.get(tag));
			
			return list;
		}

		@Override
		public List<DTLink<L, T>> linksIn()
		{
			List<DTLink<L, T>> list = new ArrayList<DTLink<L,T>>(inDegree());
			for(T tag : linksIn.keySet())
				list.addAll(linksIn.get(tag));
			
			return list;
		}

		@Override
		public Collection<T> tags()
		{
			HashSet<T> tags = new HashSet<T>();
			tags.addAll(linksOut.keySet());
			tags.addAll(linksIn.keySet());
			
			return tags;
		}

		@Override
		public Collection<? extends DTLink<L, T>> linksOut(DNode<L> other)
		{
			if(! (other instanceof DTNode<?, ?>))
				return Collections.emptyList(); 	
			
			MapDTNode o = (MapDTNode) other;
			
			if(!connected(o))
				return Collections.emptyList();
			
			List<MapDTLink> links = new LinkedList<MapDTLink>();
			for(T tag : linksOut.keySet())
				for(MapDTLink link : linksOut.get(tag))
					if(link.to().equals(o))
						links.add(link);
			
			return links;
		}

		@Override
		public Collection<? extends DTLink<L, T>> linksIn(DNode<L> other)
		{
			if(! (other instanceof DTNode<?, ?>))
				return Collections.emptyList();
			
			MapDTNode o = (MapDTNode) other;
			
			if(!connected(o))
				return Collections.emptyList();
			
			List<MapDTLink> links = new LinkedList<MapDTLink>();
			for(T tag : linksIn.keySet())
				for(MapDTLink link : linksIn.get(tag))
					if(link.from().equals(o))
						links.add(link);
			
			return links;
		}
		
		@Override
		public int hashCode()
		{
			// NOTE: We do not include the links in the calculation of the 
			// hashcode. We want the hashcode to remain invariant to 
			// modifications of the graph. 
			int hash = 1;
			
			hash = 31 * hash + (label == null ? 0 : label.hashCode());
			
//			for(T tag : linksOut.keySet())
//				hash = 31 * hash + (tag == null ? 0 : tag.hashCode());
//			
			return hash;
		}

		@Override
		public Collection<? extends DTLink<L, T>> links(Node<L> other)
		{
			if(! (other instanceof DTNode<?, ?>))
				return Collections.emptyList(); 		
			
			List<DTLink<L, T>> links = new ArrayList<DTLink<L, T>>(degree());
			
			links.addAll(linksOut( (DTNode<L, T>) other));
			links.addAll(linksIn(  (DTNode<L, T>) other));
			
			return links;
		}
	}

	private final class MapDTLink implements DTLink<L, T>
	{
		private T tag;
		private MapDTNode first, second;
		private boolean dead = false;
		
		public MapDTLink(T tag, MapDTNode first, MapDTNode second)
		{
			this.tag = tag;
			this.first = first;
			this.second = second;
		}
	
		@Override
		public T tag()
		{
			return tag;
		}
	
		@Override
		public DTNode<L, T> first()
		{
			return first;
		}
	
		@Override
		public DTNode<L, T> second()
		{
			return second;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Collection<? extends DTNode<L, T>> nodes()
		{
			return Arrays.asList(first, second);
		}
	
		@Override
		public DTGraph<L, T> graph()
		{
			return MapDTGraph.this;
		}
	
		@Override
		public void remove()
		{
			first.linksOut.get(tag).remove(this);
			second.linksIn.get(tag).remove(this);
			
			// * check whether second should be removed from first.neighborsTo
			// * Speed this up by maintaining a count in neighborsTo
			boolean occurs = false;
			for(T tag : first.linksOut.keySet())
				for(MapDTLink link : first.linksOut.get(tag))
					if(link.second().equals(second))
						occurs = true;
			if(! occurs)
				first.neighborsTo.remove(second);
			
			// * check whether first should be removed from second.neighborsFrom
			// * Speed this up by maintaining a count in neighborsFrom
			occurs = false;
			for(T tag : second.linksIn.keySet())
				for(MapDTLink link : second.linksIn.get(tag))
					if(link.first().equals(first))
						occurs = true;
			if(! occurs)
				second.neighborsFrom.remove(first);
			
			dead = true;
			
			numEdges--;
			modCount++;
		}

		@Override
		public boolean dead()
		{
			return dead;
		}
		
		public String toString()
		{
			return first + " -> " + second + (tag == null ? "" : " [label="+tag+"]");
		}

		@Override
		public DTNode<L, T> from()
		{
			return first();
		}

		@Override
		public DTNode<L, T> to()
		{
			return second();
		}

		@Override
		public DTNode<L, T> other(Node<L> current)
		{
			if(first != current)
				return first;
			return second;
		}
	}

	@Override
	public DTNode<L, T> node(L label)
	{
		Set<MapDTNode> n = nodes.get(label);
		if(n == null)
			return null;
	
		return n.iterator().next();
	}

	public int size()
	{
		return nodeList.size();
	}

	@Override
	public Set<? extends DTNode<L, T>> nodes(L label)
	{
		Set<MapDTNode> n = nodes.get(label);
		if(n == null)
			return Collections.emptySet();
		
		return Collections.unmodifiableSet(n); 
	}

	@Override
	public DTNode<L, T> add(L label)
	{
		// * Create the new node. It will add itself to the nodes map and list
		DTNode<L, T> node = new MapDTNode(label);
		
		modCount++;
		
		// This isn't necessary (right?)
		// updateIndices();

		return node;
	}

	/**
	 * Returns true if each label currently describes a unique node. 
	 * 
	 * @return
	 */
	public boolean uniqueLabels()
	{
		for(L label : nodes.keySet())
			if(nodes.get(label).size() > 1)
				return false;
		
		return true;
	}
	
	/**
	 * Returns a representation of the graph in Dot language format.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("digraph {");
		
		Set<MapDTNode> nodes = new HashSet<MapDTNode>(nodeList);
		
		boolean first = true;
		for(MapDTLink link : links())
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}
		
		for(MapDTNode node : nodes)
			sb.append("; " + node);
		
		sb.append("}");
		
		return sb.toString();
	}

	@Override
	public boolean connected(L first, L second)
	{
		for(MapDTNode f : nodes.get(first))
			for(MapDTNode s : nodes.get(second))
				if(f.connected(s))
					return true;
		return false;
	}
	
	@Override
	public Set<L> labels()
	{
		return Collections.unmodifiableSet(nodes.keySet());
	}

	@Override
	public int numLinks()
	{
		return numEdges;
	}

	@Override
	public long state()
	{
		return modCount;
	}

	@Override
	public List<? extends DTNode<L, T>> nodes()
	{
		return Collections.unmodifiableList(nodeList);
	}

	@Override
	public Collection<MapDTLink> links()
	{
		return new LinkCollection();
	}
	
	private class LinkCollection extends AbstractCollection<MapDTLink>
	{
		@Override
		public Iterator<MapDTLink> iterator()
		{
			return new LCIterator();
		}

		@Override
		public int size()
		{
			return numLinks();
		}
		
		private class LCIterator implements Iterator<MapDTLink>
		{
			
			long graphModCount = MapDTGraph.this.modCount;

			private void check()
			{
				if(MapDTGraph.this.modCount != graphModCount)
					throw new ConcurrentModificationException("The graph was modified since this link iterator was created.");
			}
			
			private static final int BUFFER_SIZE = 5;
			private LinkedList<MapDTLink> buffer = new LinkedList<MapDTLink>();
			private MapDTLink last = null;
			
			private MapDTNode current = null;
			private Iterator<MapDTNode> nodeIt = nodeList.iterator();
			private Iterator<MapDTNode> neighborIt;

			LCIterator()
			{
				
			}
			
			@Override
			public boolean hasNext()
			{
				check();
				buffer();
				
				return ! buffer.isEmpty();
			}

			@Override
			public MapDTLink next()
			{
				check();
				buffer();
				
				last = buffer.poll();
				return last;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Call remove on the link object to remove the link from the graph");
			}
			
			private void buffer()
			{
				while(buffer.size() < BUFFER_SIZE)
				{
					if(! nodeIt.hasNext())
						break;
					
					current = nodeIt.next();
					
					for(T tag : current.linksOut.keySet())
						for(MapDTLink link : current.linksOut.get(tag))
							buffer.add(link);
				}
			}
		}
	}
	
	/** 
	 * Resets the indices of all nodes
	 */
	protected void updateIndices()
	{
		
		for(int i : series(nodeList.size()))
			nodeList.get(i).index = i;
	}
	
	@Override
	public Set<T> tags()
 	{
		Set<T> tags = new HashSet<T>();
		for(DTLink<L, T> link : links())
			tags.add(link.tag());
		
		return tags;
	}

	@Override
	public DTNode<L, T> get(int i)
	{
		return nodes().get(i);
	}
	
	@Override 
	public int hashCode()
	{
		if(hashMod != null && hashMod == modCount)
			return hash;
		
		hash = 1;
		
		for(DTNode<L, T> node : nodes())
		    hash = 31 * hash + (node == null ? 0 : node.hashCode());
		
		return hash;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof DTGraph<?, ?>))
			return false;
		
		DTGraph<Object, Object> otherGraph = (DTGraph<Object, Object>) other;
		
		if(! otherGraph.level().equals(level()))
			return false;
		
		if(size() != otherGraph.size())
			return false;
		
		if(numLinks() != otherGraph.numLinks())
			return false;
		
		if(labels().size() != otherGraph.labels().size())
			return false;
		
		// * for all connected nodes
		for(DTNode<L, T> node : nodes())
		{
			if(! Functions.equals(node.label(), otherGraph.get(node.index()).label()))
				return false;
			
			for(DTNode<L, T> neighbor : node.neighbors())
			{
				Collection<? extends DTLink<L, T>> links = node.linksOut(neighbor);
				Collection<? extends DTLink<Object, Object>> otherLinks = 
						otherGraph.get(node.index())
							.linksOut(otherGraph.get(neighbor.index()));

				if(links.size() != otherLinks.size())
					return false;
				
				if(links.size() == 1)
				{
					// ** If there is only one link, check that there is a single 
					//    similar link in the other graph and that it has the same tag
					T tag = links.iterator().next().tag();
					Object otherTag = otherLinks.iterator().next().tag();
					
					if(! Functions.equals(tag, otherTag))
						return false;
				} else {
					// ** If there are multiple links between these two nodes,
					//    count the occurrences of each tag and check that the 
					//    frequencies match between graphs
					FrequencyModel<T> 
							model = new FrequencyModel<T>();
					FrequencyModel<Object>
							otherModel = new FrequencyModel<Object>();
					
					for(DTLink<L, T> link : links)
						model.add(link.tag());
					
					for(DTLink<Object, Object> otherLink : otherLinks)
						otherModel.add(otherLink.tag());
					
					for(T token : model.tokens())
						if(otherModel.frequency(token) != model.frequency(token))
							return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public Class<? extends DTGraph<L, T>> level()
	{
		Object obj = DTGraph.class;
		return (Class<? extends DTGraph<L, T>>) obj;
	}
}
