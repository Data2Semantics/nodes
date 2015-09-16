package org.nodes;

import static org.nodes.util.Series.series;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
 * UTGraph (undirected tagged graph) implementation based on hashmaps. Each 
 * node stores its neighbours in a hashmap. 
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
public class MapUTGraph<L, T> implements UTGraph<L, T>
{
	protected List<MapUTNode> nodeList = new ArrayList<MapUTNode>();
	protected Map<L, Set<MapUTNode>> nodes = new LinkedHashMap<L, Set<MapUTNode>>();
	protected Set<T> tags = new LinkedHashSet<T>();

	protected int numEdges = 0;
	protected long modCount = 0;
	
	protected int hash;
	protected Long hashMod = null;
	
	public MapUTGraph()
	{
	}
	
	/**
	 * Returns a graph with the same structure and labels as that in the 
	 * argument.
	 * 
	 * @param graph
	 * @return
	 */
	public static <L, T> MapUTGraph<L, T> copy(UTGraph<L, T> graph)
	{	
		MapUTGraph<L, T> copy = new MapUTGraph<L, T>();
		for(UTNode<L, T> node : graph.nodes())
			copy.add(node.label());

		for(UTLink<L, T> link : graph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.nodes().get(i).connect(copy.nodes().get(j), link.tag());
		}
		
		return copy;
	}
	
	public static <L,T> MapUTGraph<L,T> copy(UGraph<L> graph)
	{	
		MapUTGraph<L, T> copy = new MapUTGraph<L, T>();
		for(UNode<L> node : graph.nodes())
			copy.add(node.label());

		for(ULink<L> link : graph.links())
		{
			int i = link.first().index(), 
			    j = link.second().index();
			
			copy.nodes().get(i).connect(copy.nodes().get(j), null);
		}
		
		return copy;
	}
	
	private class MapUTNode implements UTNode<L, T>
	{
		private Map<T, List<MapUTLink>> links = new LinkedHashMap<T, List<MapUTLink>>();
		
		private Set<MapUTNode> neighbors = new LinkedHashSet<MapUTNode>();
		
		private L label;
		
		// * A node is dead when it is removed from the graph. Since there is no
		//   way to ensure that clients don't maintain copies of node objects we 
		//   keep check of nodes that are no longer part of the graph. 
		private boolean dead = false;
		
		private Integer labelId = null;
		private Long labelIdMod;
		private int index;

		public MapUTNode(L label)
		{
			this.label = label;
			
			index = nodeList.size();
			
			// * The node adds itself to the graph's data structures
			nodeList.add(this);
			
			if(! nodes.containsKey(label))
				nodes.put(label, new LinkedHashSet<MapUTNode>());
			nodes.get(label).add(this);
		}

		@Override
		public Collection<MapUTNode> neighbors()
		{
			return Collections.unmodifiableSet(neighbors);
		}

		@Override
		public MapUTNode neighbor(L label)
		{
			for(MapUTNode node : neighbors)
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
		public Set<MapUTNode> neighbors(L label)
		{
			Set<MapUTNode> result = new LinkedHashSet<MapUTNode>();
			for(MapUTNode node : neighbors)
				if((label == null && node.label == null)
						|| (label != null && label.equals(node.label)))
					result.add(node);
			
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public MapUTLink connect(Node<L> other)
		{
			if(MapUTGraph.this != other.graph())
				throw new IllegalArgumentException("Can only connect nodes that belong to the same graph.");
			
			// - This cast is safe because we know the node belongs to this 
			//   graph, so it was made by this graph, so it's a MapDTNode
			return connect((TNode<L, T>)other, null);
		}

		@Override
		public MapUTLink connect(TNode<L, T> other, T tag)
		{
			if(this.graph().hashCode() != other.graph().hashCode())
				throw new IllegalArgumentException("Can only connect to nodes from the same graph (arguments: this="+this+", other="+other+")");
			
			// * This graph can only contain MapDTNodes, so this is a safe cast
			MapUTNode mdtOther = (MapUTNode) other;
			
//			if(connected(mdtOther, tag))
//				return;
			
			MapUTLink link = new MapUTLink(tag, this, mdtOther);
			
			// * Add to this node's neighbors
			if(! links.containsKey(tag))
				links.put(tag, new LinkedList<MapUTLink>());
			links.get(tag).add(link);
			
			// * Add this  in other's neighbors
			if(! mdtOther.links.containsKey(tag))
				mdtOther.links.put(tag, new LinkedList<MapUTLink>());
			mdtOther.links.get(tag).add(link);
			
			neighbors.add(mdtOther);
			mdtOther.neighbors.add(this);
			
			numEdges++;
			modCount++;
			
			return link;
		}

		@Override
		public void disconnect(Node<L> other)
		{	
			if(MapUTGraph.this != other.graph())
				throw new IllegalArgumentException("Can only disconnect nodes that belong to the same graph.");

			if(!connected(other))
				return;
			
			MapUTNode mdtOther = (MapUTNode)other;
			
			int removed = 0;
			
			List<MapUTLink> toRemove = new LinkedList<MapUTLink>();
			for(T tag : links.keySet())
			{
				for(MapUTLink link : links.get(tag))
					if(link.other(this).equals(mdtOther))
						toRemove.add(link);
			}
			
			for(MapUTLink link : toRemove)
				link.remove();
						
			if(removed > 0)
				modCount++;	
		}

		@Override
		public boolean connected(Node<L> other)
		{
			return neighbors.contains(other);
		}

		@Override
		public UTGraph<L, T> graph()
		{
			return MapUTGraph.this;
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
				Collection<MapUTNode> others = nodes.get(label);
				
				int i = 0;
				for(MapUTNode other : others)
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
			
			// * Copy the nodes over to avoid a ConcurrentModificationException
			List<MapUTNode> ns = new ArrayList<MapUTNode>(neighbors);
			
			for(MapUTNode to: ns)
				disconnect(to);
			
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
		public int index()
		{
			return index;
		}

		@Override
		public TLink<L, T> link(TNode<L, T> other)
		{			
			MapUTNode o = (MapUTNode) other;
			
			if(!connected(o))
				return null;
			
			for(T tag : links.keySet())
				for(MapUTLink link : links.get(tag))
					if(link.second().equals(o))
						return link;
			
			return null;
		}

		@Override
		public Collection<? extends UTLink<L, T>> links(TNode<L, T> other)
		{
			MapUTNode o = (MapUTNode) other;
			
			if(! connected(o))
				return Collections.emptyList();
			
			List<MapUTLink> result = new LinkedList<MapUTLink>();
			for(T tag : links.keySet())
				for(MapUTLink link : links.get(tag))
					if(link.other(this).equals(o))
						result.add(link);
			
			return result;
		}

		@Override
		public boolean connected(TNode<L, T> other, T tag)
		{
			if(! links.containsKey(tag))
				return false;
			
			for(MapUTLink link : links.get(tag))
				if(link.other(this).equals(other))
					return true;
				
			return false;
		}

		@Override
		public int degree()
		{
			int n = 0;
			for(T tag : links.keySet())
				n += links.get(tag).size();
			
			return n;
		}

		@Override
		public List<UTLink<L, T>> links()
		{
			List<UTLink<L, T>> list = new ArrayList<UTLink<L,T>>(degree());
			for(T tag : links.keySet())
				list.addAll(links.get(tag));
			
			return list;
		}

		@Override
		public Collection<T> tags()
		{
			return Collections.unmodifiableCollection(links.keySet());
		}
		
		@Override
		public int hashCode()
		{
			// * We base the hashcode on just the label. If adding links changes
			//   the hashcode, our maps will get messed up. 
			// * Even the node index is _not_ persistent.
			int hash = 1;
			
			hash = 31 * hash + (label == null ? 0 : label.hashCode());
			
			return hash;
		}

		@Override
		public Collection<? extends UTLink<L, T>> links(Node<L> other)
		{
			List<UTLink<L, T>> result = new ArrayList<UTLink<L, T>>();
			
			for(T tag : links.keySet())
				for(UTLink<L, T> link : links.get(tag))
					if(link.other(this).equals(other))
						result.add(link);
			
			return result;
		}
		
		public boolean equals(Object other)
		{
			return this == other;
		}
	}

	private final class MapUTLink implements UTLink<L, T>
	{
		private T tag;
		private MapUTNode first, second;
		private boolean dead = false;
		
		public MapUTLink(T tag, MapUTNode first, MapUTNode second)
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
		public UTNode<L, T> first()
		{
			return first;
		}
	
		@Override
		public UTNode<L, T> second()
		{
			return second;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Collection<? extends UTNode<L, T>> nodes()
		{
			return Arrays.asList(first, second);
		}
	
		@Override
		public UTGraph<L, T> graph()
		{
			return MapUTGraph.this;
		}
	
		@Override
		public void remove()
		{
			first.links.get(tag).remove(this);
			second.links.get(tag).remove(this);
			
			// * check whether second should be removed from first.neighborsTo
			boolean occurs = false;
			for(T tag : first.links.keySet())
				for(MapUTLink link : first.links.get(tag))
					if(link.second().equals(second))
						occurs = true;
			if(! occurs)
				first.neighbors.remove(second);
			
			// * check whether first should be removed from second.neighborsFrom
			occurs = false;
			for(T tag : second.links.keySet())
				for(MapUTLink link : second.links.get(tag))
					if(link.first().equals(first))
						occurs = true;
			if(! occurs)
				second.neighbors.remove(first);
			
			dead = true;
			
			numEdges --;
			modCount++;
		}

		@Override
		public boolean dead()
		{
			return dead;
		}
		
		public String toString()
		{
			return first + " -- " + second + (tag == null ? "" : " [label="+tag+"]");
		}

		@Override
		public UTNode<L, T> other(Node<L> current)
		{
			if(first != current)
				return first;
			return second;
		}
		
		@Override
		public int hashCode()
		{
			int result = 1;
			result = 31 * result + (dead ? 1231 : 1237);
			result = 31 * result + ((tag == null) ? 0 : tag.hashCode());
			return result;
		}
	}

	@Override
	public UTNode<L, T> node(L label)
	{
		Set<MapUTNode> n = nodes.get(label);
		if(n == null)
			return null;
	
		return n.iterator().next();
	}

	public int size()
	{
		return nodeList.size();
	}

	@Override
	public Set<? extends UTNode<L, T>> nodes(L label)
	{
		Set<MapUTNode> n = nodes.get(label);
		if(n == null)
			return Collections.emptySet();
		
		return Collections.unmodifiableSet(n); 
	}

	@Override
	public UTNode<L, T> add(L label)
	{
		// * Create the new node. It will add itself to the nodes map and list
		UTNode<L, T> node = new MapUTNode(label);
		
		modCount++;
		 
		//updateIndices();
		
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
		sb.append("graph {");
		
		Set<MapUTNode> nodes = new HashSet<MapUTNode>(nodeList);
		
		boolean first = true;
		for(MapUTLink link : links())
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}
		
		for(MapUTNode node : nodes)
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");
			
			sb.append(node);
		}
		
		sb.append("}");
		
		return sb.toString();
	}

	@Override
	public boolean connected(L first, L second)
	{
		for(MapUTNode f : nodes.get(first))
			for(MapUTNode s : nodes.get(second))
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
	public List<? extends UTNode<L, T>> nodes()
	{
		return Collections.unmodifiableList(nodeList);
	}

	@Override
	public Collection<MapUTLink> links()
	{
		return new LinkCollection();
	}
	
	private class LinkCollection extends AbstractCollection<MapUTLink>
	{

		@Override
		public Iterator<MapUTLink> iterator()
		{
			return new LCIterator();
		}

		@Override
		public int size()
		{
			return numLinks();
		}
		
		private class LCIterator implements Iterator<MapUTLink>
		{
			private static final int BUFFER_SIZE = 5;
			private LinkedList<MapUTLink> buffer = new LinkedList<MapUTLink>();
			private MapUTLink last = null;
			
			private MapUTNode current = null;
			private Iterator<MapUTNode> nodeIt = nodeList.iterator();
			private Iterator<MapUTNode> neighborIt;

			LCIterator()
			{
				
			}
			
			@Override
			public boolean hasNext()
			{
				buffer();
				return ! buffer.isEmpty();
			}

			@Override
			public MapUTLink next()
			{
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
					
					for(T tag : current.links.keySet())
						for(MapUTLink link : current.links.get(tag))
						{
							int curr = current.index(), oth;
							
							if(current == link.first())
								oth = link.second().index();
							else
								oth = link.first().index();
														 
							if(curr <= oth)
								buffer.add(link);
						}
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
		return Collections.unmodifiableSet(tags);
	}
	
	@Override 
	public int hashCode()
	{
		if(hashMod != null && hashMod == modCount)
			return hash;
		
		hash = 1;
		
		for(UTNode<L, T> node : nodes())
		    hash = 31 * hash + (node == null ? 0 : node.hashCode());
		
		// * tags 
		for(UTLink<L, T> link : links())
			hash = 31 * hash + link.hashCode();
		
		// * structure
		for(UTNode<L, T> node : nodes())
		{
			List<Integer> nbIndices = new ArrayList<Integer>(node.degree());
			for(UTNode<L, T> neighbor : node.neighbors())
				nbIndices.add(neighbor.index());
			
			Collections.sort(nbIndices);
			hash = 31 * hash + nbIndices.hashCode();
		}
		
		return hash;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof UTGraph<?, ?>))
			return false;
		
		UTGraph<Object, Object> otherGraph = (UTGraph<Object, Object>) other;
		
		if(! otherGraph.level().equals(level()))
			return false;
		
		if(size() != otherGraph.size())
			return false;
		
		if(numLinks() != otherGraph.numLinks())
			return false;
		
		if(labels().size() != otherGraph.labels().size())
			return false;
		
		// * for all connected nodes
		for(UTNode<L, T> node : nodes())
		{
			if(! Functions.equals(node.label(), otherGraph.get(node.index()).label()))
				return false;
			
			for(UTNode<L, T> neighbor : node.neighbors())
			{
				Collection<? extends UTLink<L, T>> links = node.links(neighbor);
				Collection<? extends UTLink<Object, Object>> otherLinks = 
						otherGraph.get(node.index())
							.links(otherGraph.get(neighbor.index()));
				
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
					
					for(UTLink<L, T> link : links)
						model.add(link.tag());
					
					for(UTLink<Object, Object> otherLink : otherLinks)
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
	public UTNode<L, T> get(int i)
	{
		return nodes().get(i);
	}

	@Override
	public Class<? extends UTGraph<L, T>> level()
	{
		Object obj = UTGraph.class;
		return (Class<? extends UTGraph<L, T>>) obj;
	}
}
