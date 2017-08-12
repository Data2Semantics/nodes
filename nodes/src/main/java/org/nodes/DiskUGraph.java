package org.nodes;

import static java.util.Collections.unmodifiableList;
import static nl.peterbloem.kit.Functions.equals;
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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.mapdb.serializer.SerializerUtils;
import org.nodes.DiskDGraph.SerializerIntList;
import org.nodes.data.Data;

import com.google.code.externalsorting.ExternalSort;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * A version of the LightUGraph which stores all data on disk. It's slower, 
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
public class DiskUGraph implements UGraph<String>, FastWalkable<String, UNode<String>>
{
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;
	
	private DB db;
	
	private boolean nullLabels = false;
	private List<String> labels;
	
	List<List<Integer>> neighbors;
	
	private long numLinks = 0;
	private long modCount = 0;
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just node removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;
	
	private boolean sorted = false;
	
	public DiskUGraph(File dbFile)
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
	public DiskUGraph(File dbFile, boolean nullLabels)
	{
		this.nullLabels = nullLabels;
		
		db = DBMaker.fileDB(dbFile).make();
		
		labels = nullLabels ? null : db.indexTreeList("labels", Serializer.STRING).createOrOpen();
				
		neighbors = db.indexTreeList("neighbors", new SerializerIntList()).createOrOpen();
		
		if(!nullLabels && labels.size() != neighbors.size())
			throw new IllegalStateException("labels list has size "+ labels.size() + ", should be " + neighbors.size() + ".");
		
		if(db.exists("numLinks"))
			numLinks = db.atomicLong("numLinks").createOrOpen().get();
		else
			for(List<Integer> list : neighbors)
				numLinks += list.size();
	}
	
	@Override
	public int size()
	{
		return neighbors.size();
	}

	@Override
	public long numLinks()
	{
		return numLinks;
	}

	@Override
	public UNode<String> node(String label)
	{
		int i = labels.indexOf(label);
		if(i == -1)
			return null;	
		
		return new DiskUNode(i);
	}
	
	private class DiskUNode implements UNode<String>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;

		public DiskUNode(int index)
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
			
			int linksRemoved = degree();
			linksRemoved -= links(this).size();
			
			numLinks -= linksRemoved;

			
			for(int i : series(neighbors.size()))
			{
				List<Integer> nn = new ArrayList<Integer>(neighbors.get(i));
				
				Iterator<Integer> it = nn.iterator();
				while(it.hasNext())
					if(index == (int)it.next())
						it.remove();
				
				neighbors.set(i, nn);
			}
			
			// * move through all neighbor lists and decrement every index that 
			//   is higher than the one we just removed.  
			for(int i : series(neighbors.size()))
			{
				List<Integer> nn = new ArrayList<Integer>(neighbors.get(i));

				for(int j : series(nn.size()))
				{
					Integer value = nn.get(j);
					if(value > index)
						nn.set(j, value - 1);
				}	
				
				neighbors.set(i, nn);
			}
			
			neighbors.remove((int)index);
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
		public Collection<? extends UNode<String>> neighbors()
		{
			check();
			
			Set<Integer> set = new LinkedHashSet<Integer>(neighbors.get(index));
			
			return new NodeList(new ArrayList<Integer>(set));
		}

		@Override
		public UNode<String> neighbor(String label)
		{
			check();
			
			for(int i : neighbors.get(this.index))
				if(eq(labels.get(i), label))
					return new DiskUNode(index);
			
			return null;
		}

		@Override
		public Collection<? extends UNode<String>> neighbors(String label)
		{
			check();
			
			List<Integer> indices = new ArrayList<Integer>(degree());
	
			for(int i : neighbors.get(this.index))
				if(eq(labels.get(i), label))
					indices.add(i);
			
			return new NodeList(indices);
		}

		@Override
		public ULink<String> connect(Node<String> to)
		{
			check();
			
			int fromIndex = index, toIndex = to.index();
			
			List<Integer> nn = new ArrayList<Integer>(neighbors.get(fromIndex));
			nn.add(toIndex);
			neighbors.set(fromIndex, nn);
			
			if(fromIndex != toIndex)
			{				
				nn = new ArrayList<Integer>(neighbors.get(toIndex));
				nn.add(fromIndex);
				neighbors.set(toIndex, nn);

			}
						
			modCount++;			
			numLinks++;
			
			sorted = false;
			
			return new DiskULink(index(), to.index());
		}

		@Override
		public void disconnect(Node<String> other)
		{
			check();
			
			int mine = index, his = other.index();
			int removed = 0;
		
			List<Integer> nn;
			
			nn = new ArrayList<Integer>(neighbors.get(mine));
			while(nn.remove((Integer)his))
				removed ++;
			neighbors.set(mine, nn);

			nn = new ArrayList<Integer>(neighbors.get(his));
			while(nn.remove((Integer)mine));
			neighbors.set(his, nn);

			numLinks -= removed;
			modCount++;
		}

		@Override
		public boolean connected(Node<String> other)
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
		public UGraph<String> graph()
		{
			check();
			
			return DiskUGraph.this;
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
			
			DiskUNode other = (DiskUNode) obj;
			
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
		public List<ULink<String>> links()
		{
			check();
			
			List<ULink<String>> list = new ArrayList<ULink<String>>(degree());
			for(int neighbor : neighbors.get(index))
				list.add(new DiskULink(index, neighbor));
			
			return list;
		}

		@Override
		public Collection<? extends ULink<String>> links(Node<String> other)
		{
			check();
			
			List<ULink<String>> list = new ArrayList<ULink<String>>();
			
			int o = other.index();
			for(int neighbor : neighbors.get(index))
				if(neighbor == o)
					list.add(new DiskULink(index, neighbor));
						
			return list;
		}
	}
	
	private class DiskULink implements ULink<String>
	{
		private UNode<String> from, to;
		
		private long nodeModState = nodeModCount;
		
		private boolean dead = false;
		
		public DiskULink(int from, int to)
		{
			this.from = new DiskUNode(from);
			this.to = new DiskUNode(to);
		}
		
		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");
			
			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		
		
		@Override
		public Collection<? extends UNode<String>> nodes()
		{
			check();
			return Arrays.asList(from, to);
		}

		@Override
		public Graph<String> graph()
		{
			check();
			return DiskUGraph.this;
		}

		@Override
		public void remove()
		{
			check();
			
			List<Integer> nn;
			
			nn = new ArrayList<Integer>(neighbors.get(to.index()));
			nn.remove((Integer)from.index());
			neighbors.set(to.index(), nn);
						
			nn = new ArrayList<Integer>(neighbors.get(from.index()));
			nn.remove((Integer)to.index());
			neighbors.set(from.index(), nn);
			
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
		public UNode<String> first()
		{
			check();
			return from;
		}

		@Override
		public UNode<String> second()
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
			DiskULink other = (DiskULink) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dead != other.dead)
				return false;
			
			if(from.equals(other.from) && to.equals(other.to))
				return true;
			
			if(from.equals(other.to) && to.equals(other.from))
				return true;

			return false;
		}

		private DiskUGraph getOuterType()
		{
			return DiskUGraph.this;
		}
		
		public String toString()
		{
			check();
			return from + " -- " + to;
		}

		@Override
		public UNode<String> other(Node<String> current)
		{
			if(! first().equals(current))
				return first();
			return second();
		}
	}

	private class NodeList extends AbstractList<UNode<String>>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public DiskUNode get(int index)
		{
			return new DiskUNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}
	
	@Override
	public Collection<? extends UNode<String>> nodes(String label)
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
	public List<? extends UNode<String>> nodes()
	{
		return new NodeList(Series.series(size()));
	}
	
	@Override
	public Iterable<? extends ULink<String>> links()
	{
		return new LinkCollection();
	}
	
	/**
	 * A collection of all links in this graph.
	 * 
	 * @author Peter
	 *
	 */
	private class LinkCollection extends AbstractCollection<ULink<String>>
	{
		@Override
		public Iterator<ULink<String>> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return (int)numLinks;
		}
		
		private class LLIterator implements Iterator<ULink<String>>
		{
			private static final int BUFFER_LIMIT = 5;
			private long graphState = state();
			private Deque<DiskULink> buffer = new LinkedList<DiskULink>();
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
			public ULink<String> next()
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
				if(next >= DiskUGraph.this.size())
					return;
				
				while(buffer.size() < BUFFER_LIMIT && next < DiskUGraph.this.size())
				{
					int from = next;
					
					List<Integer> tos = neighbors.get(from);
					for(int to : tos)
						if(to >= from)
							buffer.add(new DiskULink(from, to));
					
					next++;
				}
					
			}
		}
		
	}

	@Override
	public UNode<String> add(String label)
	{
		if(nullLabels && label != null)
			throw new IllegalArgumentException("Graph is set to null labels only.");
		
		if(! nullLabels)
			labels.add(label);
				
		neighbors.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		
		sorted = false;
		return new DiskUNode(neighbors.size() - 1);
	}

	@Override
	public Set<String> labels()
	{
		if(nullLabels)
		{
			Set<String> res = new LinkedHashSet<String>();
			res.add(null);
			return res;
		}
		
		return new LinkedHashSet<String>(labels);
	}

	@Override
	public boolean connected(String from, String to)
	{
		for(UNode<String> a : nodes(from))
			for(UNode<String> b : nodes(to))
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
		
		Set<UNode<String>> nodes = new HashSet<UNode<String>>(nodes());

		int i = 0;
		for(ULink<String> link : links())
		{
			if(i++ != 0)
				sb.append(";");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}

		for(UNode<String> node : nodes)
		{
			if(i++ != 0) 
				sb.append(";");
			
			sb.append(node);
		}
		
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
//		for(int i : Series.series(neighbors.size()))
//		{
//			List<Integer> old = neighbors.get(i);
//			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
//			nw.addAll(old);
//			
//			neighbors.set(i, nw);
//		}
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
		
		for(int i : Series.series(neighbors.size()))
		{
			List<Integer> nn = new ArrayList<Integer>(neighbors.get(i));
			Collections.sort(nn);
			neighbors.set(i, nn);
		}
		
		sorted = true;
	}
	
	/**
	 * Creates a copy of the given graph as a LightUGraph object. 
	 * 
	 * @param graph
	 * @return
	 */
	public static DiskUGraph copy(Graph<String> graph, File db)
	{
		
		DiskUGraph copy = new DiskUGraph(db);
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
		    hash = 31 * hash + (labels.get(i) == null ? 0 : labels.get(i).hashCode());
		
		// * structure
		for(UNode<String> node : nodes())
		{
			List<Integer> nbIndices = new ArrayList<Integer>(node.degree());
			for(UNode<String> neighbor : node.neighbors())
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
		
		for(UNode<String> node : nodes())
		{
			UNode<Object> othNode = oth.get(node.index());
			
			if(! Functions.equals(node.label(), othNode.label()))
				return false;
			
			FrequencyModel<Integer> myNeighbors = new FrequencyModel<Integer>(),
			                        hisNeighbors = new FrequencyModel<Integer>();
			for(UNode<String> myNeighbor : node.neighbors())
				myNeighbors.add(myNeighbor.index());
			
			for(UNode<Object> hisNeighbor : othNode.neighbors())
				hisNeighbors.add(hisNeighbor.index());
						
			if(! myNeighbors.equals(hisNeighbors))
				return false;
		}
		
		return true;
		
	}

	@Override
	public UNode<String> get(int i)
	{
		return nodes().get(i);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends UGraph<String>> level()
	{
		Object obj = DGraph.class;
		return (Class<? extends UGraph<String>>) obj;
	}

	@Override
	public List<UNode<String>> neighborsFast(Node<String> node)
	{
		if(node.graph() != this)
			throw new IllegalArgumentException("Cannot call with node from another graph.");
		
		List<Integer> indices = neighbors.get(node.index());
		
		return new NodeList(indices);
	}
	
	/**
	 * Loads a previous converted graph.
	 * 
	 * @param dbFile
	 * @return
	 * @throws IOException
	 */
	public static DiskUGraph fromDB(File dbFile)
			throws IOException
	{
		DB db = DBMaker.fileDB(dbFile).make();

		if(db.exists("labels"))
		{
			db.close();
			return new DiskUGraph(dbFile, false);
		}
		
		db.close();
		return new DiskUGraph(dbFile, true);
	}
	
	/**
	 * Reads a (large) edgelist-encoded file into a DiskDGraph. 
	 *  
	 * @param file
	 * @param dir Where to put temporary files, used in reading the graph
	 * @return
	 */
	public static DiskUGraph fromFile(File file, File dir)
		throws IOException
	{
		int id = (new Random()).nextInt(10000000);

		return fromFile(file, dir, new File("graph."+id+".db"));	
	}
	
	public static DiskUGraph fromFile(File file, File tmpDir, File dbFile)
			throws IOException
		{
		DiskUGraph graph = new DiskUGraph(dbFile, true);

		// * sort the input file by first element
        File forward = new File(tmpDir, "forward.edgelist");
        
        
        List<File> files = ExternalSort.sortInBatch(
        		file, 
        		new LComp(true), ExternalSort.DEFAULTMAXTEMPFILES, 
        		Charset.defaultCharset(), tmpDir, false);
        ExternalSort.mergeSortedFiles(files, forward, new LComp(true), Charset.defaultCharset());
        
        Global.log().info("Forward sort finished");
        
        readSorted(graph.neighbors, forward, true);
        
        Global.log().info("Forward list read");

        forward.delete();
        
        
        File backward = new File(tmpDir, "backward.edgelist");
        
        files = ExternalSort.sortInBatch(
        		file, 
        		new LComp(false), ExternalSort.DEFAULTMAXTEMPFILES, 
        		Charset.defaultCharset(), tmpDir, false);
        ExternalSort.mergeSortedFiles(files, backward, new LComp(false), Charset.defaultCharset());
        
        Global.log().info("Backward sort finished");
        
        long links = readSorted(graph.neighbors, backward, false);
        
        Global.log().info("Backward list read");
        
        backward.delete();
        
        
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
				if(forward)
				{
					// create a new list
    				try {
    					list.add(neighbors);
    					
    					if(a < list.size())
    						throw new IllegalStateException("Next index is "+a+", while list size is already " + list.size() + ". It seems like the sorting of the file went wrong.");
    				} catch(AssertionError e)
    				{
    					throw new AssertionError("Failed to add list to IndexTreeList. current list size: "+list.size()+", list to be added "+neighbors);
    				}
				} else
				{
					if(list.size() > current)
					{
						List<Integer> existing = list.get(current);
						List<Integer> comb = Functions.concat(existing, neighbors);
						
						list.set(current, comb);
					} else 
					{
	    				try {

	    					list.add(neighbors);
	    				} catch(AssertionError e)
	    				{
	    					throw new AssertionError("Failed to add list to IndexTreeList. Current list size: "+list.size()+", list to be added "+neighbors);
	    				}
					}
				}
				
				neighbors.clear();
						
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
}
