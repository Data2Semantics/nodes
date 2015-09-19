package org.nodes;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.nodes.util.Pair.p;
import static org.nodes.util.Series.series;

import java.awt.PageAttributes.OriginType;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.nodes.Global;
import org.nodes.algorithms.Nauty;
import org.nodes.draw.Draw;
import org.nodes.random.RandomGraphs;
import org.nodes.util.BitString;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Order;
import org.nodes.util.Pair;
import org.nodes.util.Series;

public class Graphs
{
	
//	public <L, N extends Node<L, N>> List<L> label(List<N> nodes)
//	{
//		// TODO
//		return null;
//	}
//
//	public <L, N extends Node<L, N>> Collection<L> labelCollection(Collection<N> nodes)
//	{
//		// TODO
//		return null;
//	}
//	
//	public <L, N extends Node<L, N>> Set<L> labelSet(Set<N> nodes)
//	{
//		// TODO
//		return null;
//	}
//	
//	public <L, N extends Node<L, N>> Iterable<L> labelIterable(Iterable<N> nodes)
//	{
//		// TODO
//		return null;
//	}
//	
//	/**
//	 * Finds the given path of labels in the graph. 
//	 * 
//	 * Example use:
//	 * <code>
//	 * Walks.find(graph, Arrays.asList("a", "b", "c", "d", "e"));
//	 * </code>
//	 * 
//	 * @param <T>
//	 * @return
//	 */
//	public static <L, N extends Node<L, N>> Set<Walk<L, N>> find(Iterable<L> track, Graph<L, ?> graph)
//	{
//		// TODO
//		return null;
//	}	
//	
//	public static <L, N extends Node<L, N>> Set<L> labels(Graph<L, N> graph)
//	{
//		Set<L> labels = new LinkedHashSet<L>();
//		
//		for(N node : graph)
//			labels.add(node.label());
//		
//		return labels;
//	}
	
	/**
	 * Returns a graph with the same structure and labels as that in the 
	 * argument, but with the nodes in a different order. Ie. this method 
	 * returns a random isomorphism of the argument.
	 * 
	 * @param graph
	 * @return
	 */
	public static <L, T> UTGraph<L, T> shuffle(UTGraph<L, T> graph)
	{
		List<Integer> shuffle = new ArrayList<Integer>(series(graph.size()));
		Collections.shuffle(shuffle);
		// System.out.println(shuffle);
		
		UTGraph<L, T> out = new MapUTGraph<L, T>();
		for(int i : series(graph.size()))
			out.add(graph.nodes().get(shuffle.get(i)).label());
		
		for(int i : series(graph.size()))
			for(int j : series(i, graph.size()))
				for(T tag : graph.tags())
				{
					if(graph.nodes().get(i).connected(graph.nodes().get(j), tag))
					{
						int iOut = shuffle.get(i), jOut = shuffle.get(j);
						out.nodes().get(iOut).connect(
								out.nodes().get(jOut), tag);
					}
				}
		
		return out;
	}
//	
//		
	/**
	 * Returns a fully connected UT graph of the given size, with the given label
	 * on all nodes (and null for all tags)
	 * 
	 * @param size
	 * @param label
	 * @return
	 */
	public static UTGraph<String, String> k(int size, String label)
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
		
		for(int i : Series.series(size))
		{
			UTNode<String, String> node = graph.add(label);
			
			for(int j : Series.series(graph.size() - 1 ))
				node.connect(graph.nodes().get(j));
		}
		
		return graph;
	}
	
	/**
	 * Returns a fully connected UT graph of the given size, with the given label
	 * on all nodes (and null for all tags)
	 * 
	 * @param size
	 * @param label
	 * @return
	 */
	public static DTGraph<String, String> dk(int size, String label)
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		for(int i : Series.series(size))
		{
			DTNode<String, String> node = graph.add(label);
			
			for(int j : Series.series(graph.size() - 1 ))
			{
				DTNode<String, String> other = graph.nodes().get(j);
				node.connect(other);
				other.connect(node);
			}
		}
		
		return graph;
	}	
	
	public static UTGraph<String, String> line(int n, String label)
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();

		if(n == 0)
			return graph;
			
		UTNode<String, String> last = graph.add(label), next;
		for(int i : series(n-1))
		{
			next = graph.add(label);
			last.connect(next);
			last = next;
		}
		
		return graph;
	}
	
	/**
	 * Returns a graph with n nodes, arranged in a star topology (ie. one node 
	 * is connected to all others, and all others are connected only to that node).
	 * 
	 * 
	 * @param n
	 * @param label
	 * @return
	 */
	public static UTGraph<String, String> star(int n, String label)
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
			
		UTNode<String, String> center = graph.add(label);
		for(int i : Series.series(n))
			center.connect(graph.add(label));
		
		return graph;
	}
	
	public static UTGraph<String, String> ladder(int n, String label)
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();

		if(n == 0)
			return graph;
			
		Node<String> 	lastLeft = graph.add(label),
						lastRight = graph.add(label),				
						nextLeft, nextRight;
		lastLeft.connect(lastRight);
		
		for(int i : series(n-1))
		{
			nextRight = graph.add(label);
			nextLeft  = graph.add(label);
			
			nextLeft.connect(nextRight);
			
			nextRight.connect(lastRight);
			nextLeft.connect(lastLeft);
			
			lastLeft = nextLeft;
			lastRight = nextRight;
		}
		
		return graph;
	}
	
	/**
	 * A graph based on the example graph in the paper by Jonyer, Holder and Cook
	 * 
	 * @return
	 */
	public static UTGraph<String, String> jbc()
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();

		// * triangle 1
		UTNode<String, String> t1a = graph.add("a"),
		                       t1b = graph.add("b"),
		                       t1c = graph.add("c");
		t1a.connect(t1b);
		t1b.connect(t1c);
		t1c.connect(t1a);
		
		// * triangle 2
		UTNode<String, String> t2a = graph.add("a"),
                               t2b = graph.add("b"),
                               t2d = graph.add("d");
		
		t2a.connect(t2b);
		t2b.connect(t2d);
		t2d.connect(t2a);

		// * triangle 3
		UTNode<String, String> t3a = graph.add("a"),
                               t3b = graph.add("b"),
                               t3e = graph.add("e");
		
		t3a.connect(t3b);
		t3b.connect(t3e);
		t3e.connect(t3a);	
		
		// * triangle 4
		UTNode<String, String> t4a = graph.add("a"),
                               t4b = graph.add("b"),
                               t4f = graph.add("f");

		t4a.connect(t4b);
		t4b.connect(t4f);
		t4f.connect(t4a);	
		
		// * square 1
		UTNode<String, String> s1x = graph.add("x"),
                               s1y = graph.add("y"),
                               s1z = graph.add("z"),
                               s1q = graph.add("q");

		s1x.connect(s1y);
		s1y.connect(s1q);
		s1q.connect(s1z);	
		s1z.connect(s1x);	
			
		// * square 2
		UTNode<String, String> s2x = graph.add("x"),
                               s2y = graph.add("y"),
                               s2z = graph.add("z"),
                               s2q = graph.add("q");

		s2x.connect(s2y);
		s2y.connect(s2q);
		s2q.connect(s2z);	
		s2z.connect(s2x);	
		
		// * square 3
		UTNode<String, String> s3x = graph.add("x"),                   
                               s3y = graph.add("y"),                   
                               s3z = graph.add("z"),                   
                               s3q = graph.add("q");                   
                                                        
		s3x.connect(s3y);                                              
		s3y.connect(s3q);                                                  
		s3q.connect(s3z);	                                               
		s3z.connect(s3x);	                                               

		// * square 4
		UTNode<String, String> s4x = graph.add("x"),                   
                               s4y = graph.add("y"),                   
                               s4z = graph.add("z"),                   
                               s4q = graph.add("q");                   
                                             
		s4x.connect(s4y);                                              
		s4y.connect(s4q);                                              
		s4q.connect(s4z);	                                           
		s4z.connect(s4x);			
		
		// rest
		UTNode<String, String> k = graph.add("k"),                   
		                       r = graph.add("r"); 
		
		t1a.connect(t2a);
		t2a.connect(t3a);
		t3a.connect(t4a);
		
		t1c.connect(s1y);
		t4f.connect(s4y);
		
		t2d.connect(k);
		k.connect(r);
		
		s1y.connect(s2x);
		s2y.connect(r);
		s3x.connect(r);
		s3y.connect(s4x);
		
		return graph;
	}
	
	/**
	 * A graph based on the example graph in the paper by Jonyer, Holder and Cook, directed version.
	 * 
	 * @return
	 */
	public static DTGraph<String, String> jbcDirected()
	{
		DTGraph<String, String> graph = new MapDTGraph<String, String>();

		// * triangle 1
		DTNode<String, String> t1a = graph.add("a"),
		                       t1b = graph.add("b"),
		                       t1c = graph.add("c");
		t1a.connect(t1b);
		t1b.connect(t1c);
		t1c.connect(t1a);
		
		// * triangle 2
		DTNode<String, String> t2a = graph.add("a"),
                               t2b = graph.add("b"),
                               t2d = graph.add("d");
		
		t2a.connect(t2b);
		t2b.connect(t2d);
		t2d.connect(t2a);

		// * triangle 3
		DTNode<String, String> t3a = graph.add("a"),
                               t3b = graph.add("b"),
                               t3e = graph.add("e");
		
		t3a.connect(t3b);
		t3b.connect(t3e);
		t3e.connect(t3a);	
		
		// * triangle 4
		DTNode<String, String> t4a = graph.add("a"),
                               t4b = graph.add("b"),
                               t4f = graph.add("f");

		t4a.connect(t4b);
		t4b.connect(t4f);
		t4f.connect(t4a);	
		
		// * square 1
		DTNode<String, String> s1x = graph.add("x"),
                               s1y = graph.add("y"),
                               s1z = graph.add("z"),
                               s1q = graph.add("q");

		s1x.connect(s1y);
		s1y.connect(s1q);
		s1q.connect(s1z);	
		s1z.connect(s1x);	
			
		// * square 2
		DTNode<String, String> s2x = graph.add("x"),
                               s2y = graph.add("y"),
                               s2z = graph.add("z"),
                               s2q = graph.add("q");

		s2x.connect(s2y);
		s2y.connect(s2q);
		s2q.connect(s2z);	
		s2z.connect(s2x);	
		
		// * square 3
		DTNode<String, String> s3x = graph.add("x"),                   
                               s3y = graph.add("y"),                   
                               s3z = graph.add("z"),                   
                               s3q = graph.add("q");                   
                                                        
		s3x.connect(s3y);                                              
		s3y.connect(s3q);                                                  
		s3q.connect(s3z);	                                               
		s3z.connect(s3x);	                                               

		// * square 4
		DTNode<String, String> s4x = graph.add("x"),                   
                               s4y = graph.add("y"),                   
                               s4z = graph.add("z"),                   
                               s4q = graph.add("q");                   
                                             
		s4x.connect(s4y);                                              
		s4y.connect(s4q);                                              
		s4q.connect(s4z);	                                           
		s4z.connect(s4x);			
		
		// rest
		DTNode<String, String> k = graph.add("k"),                   
		                       r = graph.add("r"); 
		
		t1a.connect(t2a);
		t2a.connect(t3a);
		t3a.connect(t4a);
		
		t1c.connect(s1y);
		t4f.connect(s4y);
		
		t2d.connect(k);
		k.connect(r);
		
		s1y.connect(s2x);
		s2y.connect(r);
		s3x.connect(r);
		s3y.connect(s4x);
		
		return graph;
	}
	
	
	public static UTGraph<String, String> single(String label)
	{
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
		graph.add(label);
		
		return graph;
		
	}

	public static List<Integer> degrees(Graph<?> graph)
	{
		List<Integer> degrees = new ArrayList<Integer>(graph.size());
		
		for(Node<?> node : graph.nodes())
			degrees.add(node.degree());
		
		return degrees;
	}
	
	public static List<Integer> inDegrees(DGraph<?> graph)
	{
		List<Integer> inDegrees = new ArrayList<Integer>(graph.size());
		
		for(DNode<?> node : graph.nodes())
			inDegrees.add(node.inDegree());
		
		return inDegrees;
	}
	
	public static List<Integer> outDegrees(DGraph<?> graph)
	{
		List<Integer> outDegrees = new ArrayList<Integer>(graph.size());
		
		for(DNode<?> node : graph.nodes())
			outDegrees.add(node.outDegree());
		
		return outDegrees;
	}
	
	public static <L> void add(Graph<L> graph, Graph<L> addition)
	{
		List<Node<L>> nodesAdded = new ArrayList<Node<L>>(addition.size());
		
		// * Add all nodes
		for(Node<L> aNode : addition.nodes())
		{
			Node<L> gNode = graph.add(aNode.label());
			nodesAdded.add(gNode);
		}
		
		// * Add all links
		for(Link<L> link : addition.links())
		{
			// System.out.println('.');
			int i = link.first().index(), j = link.second().index();
			
			nodesAdded.get(i).connect(nodesAdded.get(j));
		}
	}
	
	/**
	 * Adds one graph to another.
	 * 
	 * @param graph
	 * @param addition
	 */
	public static <L, T> void add(TGraph<L, T> graph, TGraph<L, T> addition)
	{
		List<TNode<L, T>> nodesAdded = new ArrayList<TNode<L,T>>(addition.size());
		
		// * Add all nodes
		for(TNode<L, T> aNode : addition.nodes())
		{
			TNode<L, T> gNode = graph.add(aNode.label());
			nodesAdded.add(gNode);
		}
		
		// * Add all links
		for(TLink<L, T> link : addition.links())
		{
			// System.out.println('.');
			int i = link.first().index(), j = link.second().index();
			
			nodesAdded.get(i).connect(nodesAdded.get(j), link.tag());
		}
	}
	
	/**
	 * Returns a copy of a graph with the labels and tags replaced by canonical 
	 * strings. 
	 * 
	 * @return
	 */
	public static <L, T> UTGraph<String, String> reduce(UTGraph<L, T> graph)
	{
		List<L> labels = new ArrayList<L>(graph.labels());
		List<T> tags = new ArrayList<T>(graph.tags());
		
		UTGraph<String, String> out = new MapUTGraph<String, String>();
		
		for(UTNode<L, T> node : graph.nodes())
			out.add("" + labels.indexOf(node.label()));
		
		for(int i : series(graph.size()))
			for(int j : series(i, graph.size()))
				for(T tag : graph.tags())
					if(graph.nodes().get(i).connected(graph.nodes().get(j), tag))
						out.nodes().get(i).connect(out.nodes().get(j), ""+tags.indexOf(tag));
		
		return out;
	}
	
	public static <L> Graph<String> blank(Graph<L> graph, String label)
	{
		if(graph instanceof DGraph<?>)
			return blank((DGraph<L>)graph, label);
		
		if(graph instanceof UGraph<?>)
			return blank((UGraph<L>)graph, label);

		throw new RuntimeException("Type of graph ("+graph.getClass()+") not recognized");
	}	
	
	public static <L> UGraph<String> blank(UGraph<L> graph, String label)
	{
		UGraph<String> out = new MapUTGraph<String, String>();
		
		for(UNode<L> node : graph.nodes())
			out.add(label);
		
		for(Link<L> link : graph.links())
		{
			int i = link.first().index();
			int j = link.second().index();
			
			out.get(i).connect(out.get(j));
		}
		
		return out;
	}	
	
	public static <L> DGraph<String> blank(DGraph<L> graph, String label)
	{
		DGraph<String> out = new MapDTGraph<String, String>();
		
		for(DNode<L> node : graph.nodes())
			out.add(label);
		
		for(Link<L> link : graph.links())
		{
			int i = link.first().index();
			int j = link.second().index();
			
			out.get(i).connect(out.get(j));
		}
		
		return out;
	}		
	
	/**
	 * Replaces labels by a given value, and tags by null
	 * @return
	 */
	public static <L, T> UTGraph<String, String> blank(UTGraph<L, T> graph, String label)
	{
		UTGraph<String, String> out = new MapUTGraph<String, String>();
		
		for(UTNode<L, T> node : graph.nodes())
			out.add(label);
		
		for(TLink<L, T> link : graph.links())
		{
			int i = link.first().index();
			int j = link.second().index();
			
			out.get(i).connect(out.get(j), null);
		}
		
		return out;
	}	
	
	public static <L, T> DTGraph<String, String> blank(DTGraph<L, T> graph, String label)
	{
		DTGraph<String, String> out = new MapDTGraph<String, String>();
		
		for(DTNode<L, T> node : graph.nodes())
			out.add(label);
		
		for(TLink<L, T> link : graph.links())
		{
			int i = link.first().index();
			int j = link.second().index();
			
			out.get(i).connect(out.get(j), null);
		}
		
		return out;
	}	
	
	public static <L> Graph<L> reorder(Graph<L> graph, Order order)
	{
		if(graph instanceof DGraph<?>)
			return reorder((DGraph<L>)graph, order);
		
		if(graph instanceof UGraph<?>)
			return reorder((UGraph<L>)graph, order);

		throw new RuntimeException("Type of graph ("+graph.getClass()+") not recognized");
	}
	
	public static <L> UGraph<L> reorder(UGraph<L> graph, Order order)
	{
		assert(graph.size() == order.size());
		
		UTGraph<L, String> out = new MapUTGraph<L, String>();
		for(int newIndex : series(order.size()))
			out.add(graph.get(order.originalIndex(newIndex)).label());
		
		for(ULink<L> link : graph.links())
		{
			int originalIndexFirst = link.first().index();
			int originalIndexSecond = link.second().index();
			
			UNode<L> first = out.get(order.newIndex(originalIndexFirst));
			UNode<L> second = out.get(order.newIndex(originalIndexSecond));
			
			first.connect(second);
		}
		
		return out;	
	}
	
	public static <L> DGraph<L> reorder(DGraph<L> graph, Order order)
	{
		assert(graph.size() == order.size());
		
		DTGraph<L, String> out = new MapDTGraph<L, String>();
		for(int newIndex : series(order.size()))
			out.add(graph.get(order.originalIndex(newIndex)).label());
		
		for(DLink<L> link : graph.links())
		{
			int originalIndexFirst = link.first().index();
			int originalIndexSecond = link.second().index();
			
			DNode<L> first = out.get(order.newIndex(originalIndexFirst));
			DNode<L> second = out.get(order.newIndex(originalIndexSecond));
			
			first.connect(second);
		}
		
		return out;	
	}	
	
	public static <L, T> UTGraph<L, T> reorder(UTGraph<L, T> graph, Order order)
	{
		assert(graph.size() == order.size());
		
		UTGraph<L, T> out = new MapUTGraph<L, T>();
		for(int newIndex : series(order.size()))
			out.add(graph.get(order.originalIndex(newIndex)).label());
		
		for(UTLink<L, T> link : graph.links())
		{
			int originalIndexFirst = link.first().index();
			int originalIndexSecond = link.second().index();
			
			UTNode<L, T> first = out.get(order.newIndex(originalIndexFirst));
			UTNode<L, T> second = out.get(order.newIndex(originalIndexSecond));
			
			first.connect(second, link.tag());
		}
		
		return out;	
	}
	
	public static <L, T> DTGraph<L, T> reorder(DTGraph<L, T> graph, Order order)
	{
		assert(graph.size() == order.size());
		
		DTGraph<L, T> out = new MapDTGraph<L, T>();
		for(int newIndex : series(order.size()))
			out.add(graph.get(order.originalIndex(newIndex)).label());
		
		for(DTLink<L, T> link : graph.links())
		{
			int originalIndexFirst = link.first().index();
			int originalIndexSecond = link.second().index();
			
			DTNode<L, T> first = out.get(order.newIndex(originalIndexFirst));
			DTNode<L, T> second = out.get(order.newIndex(originalIndexSecond));
			
			first.connect(second, link.tag());
		}
		
		return out;	
	}	
	
	/**
	 * Returns a string such that two isomorphic graphs will return the same
	 * string for equivalent orderings.
	 * @param graph
	 * @return
	 */
	public static <L> String canonicalString(Graph<L> graph)
	{
		return "";
	}
	
	public static <L> String canonicalString(DGraph<L> graph)
	{
		return "";
	}	
	
	public static <L> String canonicalString(UGraph<L> graph)
	{
		return "";
	}
	
	public static <L, T> String canonicalString(DTGraph<L, T> graph)
	{
		return "";
	}	
	
	public static <L, T> String canonicalString(UTGraph<L, T> graph)
	{
		return "";
	}
	
	public static <L> boolean isSimple(Graph<L> graph)
	{
		return !(hasSelfLoops(graph) || hasMultiEdges(graph));
	}
	
	public static <L> boolean hasSelfLoops(Graph<L> graph)
	{
		for(Link<L> link : graph.links())
			if(link.first().index() == link.second().index())
				return true;
		return false;
	}

	public static <L> boolean hasMultiEdges(Graph<L> graph)
	{
		Set<Pair<Integer, Integer>> set = new HashSet<Pair<Integer, Integer>>();
		
		for(Link<L> link : graph.links())
		{
			int i = link.first().index();
			int j = link.second().index();
			
			Pair<Integer, Integer> p = graph instanceof DGraph ? p(i, j) : p(min(i, j), max(i, j));
			set.add(p);
		}
		
		return graph.numLinks() > set.size();
	}

	
	/**
	 * Converts the input to a simple u graph. Any self-loops and dual links are 
	 * removed. If the original graph is directed, this information is also 
	 * removed.
	 * 
	 * @param data
	 * @return
	 */
	public static <L> UGraph<L> toSimpleUGraph(Graph<L> data)
	{
		return toSimpleUGraph(data, null);
	}
	
	public static <L> UGraph<L> toSimpleUGraph(Graph<L> data, FrequencyModel<Pair<Integer, Integer>> removals)
	{
		MapUTGraph<L, String> result = new MapUTGraph<L, String>();
		
		for(Node<L> node : data.nodes())
			result.add(node.label());
		
		for(Link<L> link : data.links())
		{
			Node<L> a = result.get(link.first().index()),
			             b = result.get(link.second().index());
			if(a.index() != b.index())
			{
				if(!a.connected(b))
				{
					a.connect(b);
				} else if(removals != null)
				{
					int minor = Math.min(a.index(), b.index());
					int major = Math.max(a.index(), b.index());

					removals.add(new Pair<Integer, Integer>(minor, major));
				}
			}
		}
		
		return result;
	}	
	
	/**
	 * Converts the input to a simple d graph. Any self-loops and dual links are 
	 * removed. If the original graph is directed, this information is also 
	 * removed.
	 * 
	 * @param data
	 * @return
	 */
	public static <L> DGraph<L> toSimpleDGraph(DGraph<L> data)
	{
		return toSimpleDGraph(data, null);
	}
	
	public static <L> DGraph<L> toSimpleDGraph(DGraph<L> data, FrequencyModel<Pair<Integer, Integer>> removals)
	{
		MapDTGraph<L, String> result = new MapDTGraph<L, String>();
		
		for(Node<L> node : data.nodes())
			result.add(node.label());
		
		for(DLink<L> link : data.links())
		{
			DNode<L> from = result.get(link.from().index()),
			         to = result.get(link.to().index());
			if(from.index() != to.index())
			{
				if(!from.connectedTo(to))
				{
					from.connect(to);
				} else if(removals != null)
				{
					Pair<Integer, Integer> pair = 
							new Pair<Integer, Integer>(from.index(), to.index());
				
					removals.add(pair);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Returns a collection containing all graphs of the given length. The 
	 * graphs are constructed on the fly.
	 *  
	 * @param n
	 * @param label
	 * @return
	 */
	public static Collection<UGraph<String>> all(final int n, final String label)
	{
		final int m = (n*n-n)/2;
		return new AbstractCollection<UGraph<String>>()
		{
			private final Collection<BitString> master = BitString.all(m); 
			@Override
			public Iterator<UGraph<String>> iterator()
			{
				return new Iterator<UGraph<String>>()
				{
					private final Iterator<BitString> bitstrings = master.iterator();
					
					@Override
					public boolean hasNext()
					{
						return bitstrings.hasNext();
					}

					@Override
					public UGraph<String> next()
					{
						return fromBits(bitstrings.next(), label);
					}

					@Override
					public void remove()
					{
						throw new UnsupportedOperationException(); 
					}
					
					
				};
			}

			@Override
			public int size()
			{
				return (int) Math.pow(2, m);
			
			}
			
		};
	}
	
	/**
	 * Returns all graphs, of the given size, up to isomorphism: for each 
	 * isomorphism class, only the canonical example is included. Note generated 
	 * on the fly, and n larger than 6 is not recommended. 
	 * 
	 * @param n
	 * @param label
	 * @return
	 */
	public static List<UGraph<String>> allIso(int n, String label)
	{
		Set<Graph<String>> set = new LinkedHashSet<Graph<String>>();
		for(Graph<String> graph : all(n, label))
			set.add(Nauty.canonize(graph));
		
		List<UGraph<String>> list = new ArrayList<UGraph<String>>(set.size());
		for(Graph<String> graph : set)
			list.add((UGraph<String>)graph);
			
		return list;
	}
	
	/**
	 * Returns connected graphs, of the given size, up to isomorphism: for each 
	 * isomorphism class, only the canonical example is included. Note generated 
	 * on the fly, and n larger than 6 is not recommended. 
	 * 
	 * @param n
	 * @param label
	 * @return
	 */
	public static List<UGraph<String>> allIsoConnected(int n, String label)
	{
		Set<Graph<String>> set = new LinkedHashSet<Graph<String>>();
		for(UGraph<String> graph : all(n, label))
			if(connected(graph))
				set.add(Nauty.canonize(graph));
		
		List<UGraph<String>> list = new ArrayList<UGraph<String>>(set.size());
		for(Graph<String> graph : set)
			list.add((UGraph<String>)graph);
			
		return list;
	}
	
	public static <L> boolean connected(UGraph<L> graph)
	{
		if(graph.size() == 0)
			return true;
		
		Set<Integer> visited = new HashSet<Integer>();
		
		LinkedList<UNode<L>> buffer = new LinkedList<UNode<L>>();
		buffer.add(graph.get(0));
		
		while(! buffer.isEmpty())
		{
			UNode<L> node = buffer.poll();
			visited.add(node.index());
			
			for(UNode<L> neighbor : node.neighbors())
				if(! visited.contains(neighbor.index()))
					buffer.add(neighbor);
		}
		
		return visited.size() == graph.size();
	}
	
	public static UGraph<String> fromBits(BitString bits, String label)
	{
		int n = numNodes(bits.size());
		
		if(n < 0)
			throw new IllegalArgumentException("Wrong number of bits.");
		
		UGraph<String> res = new MapUTGraph<String, String>();
		
		for(int i : series(n))
			res.add(label);
			
		for(int i : series(bits.size()))
			if(bits.get(i))
			{
				Pair<Integer, Integer>  pair = RandomGraphs.toPairUndirected(i, false);
				res.get(pair.first()).connect(res.get(pair.second()));
			}
		
		return res;
	}
	
	private static int numNodes(int m)
	{
		double d = Math.sqrt(1.0 + 8.0 * m);
		double na = (1.0 + d)/2.0;
		double nb = (1.0 - d)/2.0;
		double n = Math.max(na, nb);
		if(n - Math.floor(n) > 0.00001)
			return -1;
		
		return (int) n; 
	}
}

