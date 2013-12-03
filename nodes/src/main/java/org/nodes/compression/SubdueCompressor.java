package org.nodes.compression;
//package org.lilian.graphs.compression;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import org.lilian.graphs.Graphs;
//import org.lilian.graphs.MapUTGraph;
//import org.lilian.graphs.UTGraph;
//import org.lilian.graphs.UTLink;
//import org.lilian.graphs.UTNode;
//import org.lilian.graphs.subdue.CostFunctions;
//import org.lilian.graphs.subdue.GraphMDL;
//import org.lilian.graphs.subdue.InexactCost;
//import org.lilian.graphs.subdue.Subdue;
//import org.lilian.util.Compressor;
//import org.lilian.util.graphs.old.Graph;
//
//public class SubdueCompressor<L, T> implements Compressor<UTGraph<L, T>>
//{
//
//	private int maxSubSize;
//	private int maxBest;
//	private int beamWidth;
//	private int matcherBeamWidth = -1;
//	private int iterations;
//	private double threshold;
//	private boolean sparse;
//
//	public SubdueCompressor(
//			int maxSubSize, int maxBest, int beamWidth,
//			int iterations, double threshold, boolean sparse)
//	{
//		this(maxSubSize, maxBest, beamWidth, iterations, threshold, sparse, -1);
//	}
//	
//	public SubdueCompressor(
//			int maxSubSize, int maxBest, int beamWidth,
//			int iterations, double threshold, boolean sparse, int matcherBeamWidth)
//	{
//		this.maxSubSize = maxSubSize;
//		this.maxBest = maxBest;
//		this.beamWidth = beamWidth;
//		this.iterations = iterations;
//		this.sparse = sparse;
//		this.threshold = threshold;
//		this.matcherBeamWidth = matcherBeamWidth;
//	}
//
//	@Override
//	public double compressedSize(Object... objects)
//	{
//		UTGraph<L, T> graph;
//		
//		if(objects.length == 1)
//		{
//			if(! (objects[0] instanceof UTGraph<?, ?>))
//				throw new IllegalArgumentException("SubdueCompressor can only process UTgraph objects");
//			
//			graph = (UTGraph<L, T>)objects[0];
//		} else
//		{
//			graph = new MapUTGraph<L, T>();
//			
//			for(Object object : objects)
//			{
//				if(! (object instanceof UTGraph<?, ?>))
//					throw new IllegalArgumentException("SubdueCompressor can only process UTgraph objects");
//				
//				Graphs.add(graph, (UTGraph<L, T>)object);
//			}
//		}
//		
//		int numLabels = graph.labels().size();
//		int numNodes = graph.size();
//		int numLinks = graph.numLinks();
//		InexactCost<L> costFunction = CostFunctions.transformationCost(numLabels, numNodes, numLinks);
//		
//		Subdue<L, T> subdue = new Subdue<L, T>(graph, costFunction, threshold, matcherBeamWidth);
//		Collection<Subdue<L, T>.Substructure> subs = subdue.search(iterations, beamWidth, maxBest, maxSubSize);
//		
//		return subs.iterator().next().score();
//	}
//
//	@Override
//	public double ratio(Object... object)
//	{
//		throw new UnsupportedOperationException();
//	}
//}
