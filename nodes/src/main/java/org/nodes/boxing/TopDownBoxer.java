package org.nodes.boxing;
//package org.lilian.graphs.boxing;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.lilian.Global;
//
//import edu.uci.ics.jung.algorithms.shortestpath.UnweightedShortestPath;
//import edu.uci.ics.jung.graph.Graph;
//
//
///**
// * Experimental Boxing algorithm that follows a top down approach
// * @author Peter
// *
// * @param <V>
// * @param <E>
// */
//public class TopDownBoxer<L> implements BoxingAlgorithm<L>
//{
//	private Graph<L> graph;
//
//	public TopDownBoxer(Graph<L> graph)
//	{
//		this.graph = graph;
//		
//		usp = new UnweightedShortestPath<V, E>(graph);
//	}
//
//	@Override
//	public Boxing<V, E> box(int l)
//	{
//		List<Set<V>> result = new ArrayList<Set<V>>(), nextResult;
//		
//		Set<V> all = new HashSet<V>();
//		all.addAll(graph.getVertices());
//		
//		result.add(all);
//		boolean changed;
//
//		
//		do {
//			changed = false;
//
//			nextResult = new ArrayList<Set<V>>();
//			for(Set<V> box : result) 
//			{
//				Diameter d = diameter(box);					
//				System.out.println(d.distance() + " " + box.size());
//				if(d.distance() < l)
//				{
//					nextResult.add(box);
//				} else {
//					changed = true;
//					
//					Set<V> firstBox = new HashSet<V>(), 
//					       secondBox = new HashSet<V>();
//					
//					for(V vertex : box)
//					{
//						int dToFirst =(int) usp.getDistance(d.first(), vertex).doubleValue(),
//							dToSecond = (int)usp.getDistance(d.second(), vertex).doubleValue();
//						if(dToFirst < dToSecond)
//							firstBox.add(vertex);
//						else if(dToSecond < dToFirst)
//							secondBox.add(vertex);
//						else
//							(Global.random.nextBoolean() ? firstBox : secondBox).add(vertex);
//					}
//					
//					nextResult.add(firstBox);
//					nextResult.add(secondBox);
//				}
//			}
//
//			result = nextResult;
//		} while(changed);
//			
//		return new Boxing<V, E>(result, graph);
//	}
//
//	/**
//	 * Returns a diameter object containing the two nodes with the largest 
//	 * distance in all pairs of members from the given set of nodes. 
//	 * @param subgraph
//	 * @return
//	 */
//	private Diameter diameter(Set<V> subgraph)
//	{
//		List<V> verts = new ArrayList<V>(subgraph);
//		Collections.shuffle(verts, Global.random);
//		
//		int max = Integer.MIN_VALUE;
//		V from = null, to = null;
//		
//		for(V a : verts)
//			for(V b : verts)
//			{
//				int d = (int)usp.getDistance(a, b).doubleValue();
//				if(d > max)
//				{
//					from = a;
//					to = b;
//					max = d;
//				}
//			}
//		
//		return new Diameter(from, to, max);
//	}
//	
//	private class Diameter 
//	{
//		V first;
//		V second;
//		int distance;
//		
//		public Diameter(V first, V second, int distance)
//		{
//			this.first = first;
//			this.second = second;
//			this.distance = distance;
//		}
//
//		public V first()
//		{
//			return first;
//		}
//
//		public V second()
//		{
//			return second;
//		}
//
//		public int distance()
//		{
//			return distance;
//		}
//	}
//}
