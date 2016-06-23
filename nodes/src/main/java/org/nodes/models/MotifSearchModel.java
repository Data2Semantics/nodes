package org.nodes.models;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.util.Fibonacci;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;

/**
 * A copy of the motif model that searches for a good subselection in the 
 * available instances.
 *  
 * @author Peter
 *
 */
public class MotifSearchModel
{

	public static <L> double size(Graph<L> graph, Graph<L> sub, List<List<Integer>> occurrences, final StructureModel<Graph<?>> model, boolean resetWiring)
	{
		return size(graph, sub, occurrences, model, resetWiring, -1);
	}
	
	public static <L> double size(Graph<L> graph, Graph<L> sub, List<List<Integer>> occurrences, final StructureModel<Graph<?>> model, boolean resetWiring, int depth)
	{
		Function<Graph<L>> function = new Function<Graph<L>>()
		{
			public double size(Graph<L> graph, Graph<L> sub,
					List<List<Integer>> occurrences, boolean resetWiring)
			{
				return MotifModel.size(graph, sub, occurrences, model, resetWiring);
			}
		};
		
		FindPhi<Graph<L>> find 
			= new FindPhi<Graph<L>>(graph, sub, occurrences, resetWiring, depth, function);
		
		return find.size();
	}

	public static <L> double sizeBeta(Graph<L> graph, Graph<L> sub, List<List<Integer>> occurrences, boolean resetWiring, final int iterations, final double alpha)
	{
		return sizeBeta(graph, sub, occurrences, resetWiring, iterations, alpha, -1);
	}
	
	public static <L> double sizeBeta(Graph<L> graph, Graph<L> sub, List<List<Integer>> occurrences, boolean resetWiring, final int iterations, final double alpha, int depth)
	{
		Function<Graph<L>> function = new Function<Graph<L>>()
		{
			public double size(Graph<L> graph, Graph<L> sub,
					List<List<Integer>> occurrences, boolean resetWiring)
			{
				return MotifModel.sizeBeta(graph, sub, occurrences, resetWiring, iterations, alpha);
			}
		};
		
		FindPhi<Graph<L>> find 
			= new FindPhi<Graph<L>>(graph, sub, occurrences, resetWiring, depth, function);
		
		return find.size();
	}
	
	public static double sizeER(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring)
	{
		return sizeER(graph, sub, occurrences, resetWiring, -1);
	}
	
	public static double sizeER(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring, int depth)
	{
		Function<Graph<?>> function = new Function<Graph<?>>()
		{
			public double size(Graph<?> graph, Graph<?> sub,
					List<List<Integer>> occurrences, boolean resetWiring)
			{
				return MotifModel.sizeER(graph, sub, occurrences, resetWiring);
			}
		};
		
		FindPhi<Graph<?>> find 
			= new FindPhi<Graph<?>>(graph, sub, occurrences, resetWiring, depth, function);
		
		return find.size();
	}

	public static double sizeEL(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring)
	{
		return sizeEL(graph, sub, occurrences, resetWiring, -1);
	}
	
	public static double sizeEL(Graph<?> graph, Graph<?> sub, List<List<Integer>> occurrences, boolean resetWiring, int depth)
	{
		Function<Graph<?>> function = new Function<Graph<?>>()
		{
			public double size(Graph<?> graph, Graph<?> sub,
					List<List<Integer>> occurrences, boolean resetWiring)
			{
				return MotifModel.sizeEL(graph, sub, occurrences, resetWiring);
			}
		};
		
		FindPhi<Graph<?>> find 
			= new FindPhi<Graph<?>>(graph, sub, occurrences, resetWiring, depth, function);
		
		return find.size();
	}
	
	private static interface Function<G extends Graph<? extends Object>> {
		public double size(G graph, G sub, List<List<Integer>> occurrences, boolean resetWiring);
	}
	
	/** 
	 * Fibonacci search: find the number of occurrences for which the compression is optimal.
	 * 
	 * @author Peter
	 *
	 * @param <G>
	 */
	private static class FindPhi<G extends Graph<? extends Object>> 
	{
		int maxDepth = -1;
		
		G data; 
		G motif;
		List<List<Integer>> occurrences; 
		Function<G> function;
		boolean resetWiring;
		
		int cutoff;
		double size;
		
		public FindPhi(G data, G motif,
				List<List<Integer>> occurrences, 
				boolean resetWiring,
				int maxDepth,
				Function<G> function)
		{
			this.data = data;
			this.motif = motif;
			this.occurrences = occurrences;
			this.resetWiring = resetWiring;
			this.function = function;
			this.maxDepth = maxDepth;
			
			int n = occurrences.size();
			int to = Fibonacci.isFibonacci(n) ? n : (int)Fibonacci.get((int) Math.ceil(Fibonacci.getIndexApprox(n)));

			// always consider 0 occurrences
			sample(0);
			
			find(0, to, 0);
			
			Global.log().info("Search finished. Samples taken: " + cache.size());
		}

		public double size()
		{
			return size;
		}
		
		public int cutoff()
		{
			return cutoff;
		}
		
		private void find(int from, int to, int depth)
		{
			int range = to - from;
			
			if(range <= 2) // base case: from and to are neighbouring integers
			{
				// return the best of from, from +1 and to
				int x0 = from, x1 = from + 1, x2 = to;
				sample(x0);
				sample(x1);
				sample(x2);
			}
			
			if( range <= 2 || (maxDepth >= 0 && depth > maxDepth)) 
			{                                     // return best value found
				size = Double.POSITIVE_INFINITY;
				cutoff = -1;
				
				for(int key : cache.keySet())
				{
					double value = cache.get(key);
					if(size > value)
					{
						size = value;
						cutoff = key;
					}
				}
			
				return;
			}
			
			int r0 = (int)Fibonacci.previous(range);
			int mid1 = to - r0;
			int mid2 = from + r0;
			
			double y1 = sample(mid1);
			double y2 = sample(mid2);
			
			if(y1 > y2)
				find(mid1, to, depth + 1);
			else
				find(from, mid2, depth + 1);
		}
		
		private Map<Integer, Double> cache = new LinkedHashMap<Integer, Double>();
		
		public double sample(int n)
		{
			if(! cache.containsKey(n))
			{
				double size = function.size(data, motif, occurrences.subList(0, Math.min(occurrences.size(), n)), resetWiring);
				cache.put(n, size);
				
				Global.log().info("compression at " + n + " occurrences: " + size);
				
				return size;
			}
			
			return cache.get(n);
		}
	}
}
