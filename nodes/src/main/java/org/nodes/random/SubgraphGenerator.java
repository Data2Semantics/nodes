package org.nodes.random;

import static org.nodes.util.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.util.AbstractGenerator;
import org.nodes.util.Generator;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.Subgraph;
import org.nodes.random.LinkGenerators.LinkGenerator;
import org.nodes.util.Functions;
import org.nodes.util.Permutations;
import org.nodes.util.Series;

/**
 * Samples subgraphs from a given graph by the algorithm of Kashtan et al (2004)
 * 
 * @author Peter
 *
 * @param <L>
 */
public class SubgraphGenerator<L> extends AbstractGenerator<SubgraphGenerator<L>.Result>
{
	private int n = -1;
	private Generator<Integer> ints;
	
	private Graph<L> graph;
	private LinkGenerator<L> links;
	
	
	public SubgraphGenerator(Graph<L> graph, int n)
	{
		this.n = n;
		this.graph = graph;
		
		links = new LinkGenerator<L>(graph);
	}
	
	public SubgraphGenerator(Graph<L> graph, Generator<Integer> ints)
	{
		this.graph = graph;
		this.ints = ints;
		
		links = new LinkGenerator<L>(graph);
	}


	@Override
	public SubgraphGenerator<L>.Result generate()
	{
		int depth = n != -1 ? n : ints.generate();
				
		Set<Node<L>> nodes = new LinkedHashSet<Node<L>>(depth);
		Set<Link<L>> linksChosen = new LinkedHashSet<Link<L>>();
		Set<Link<L>> linksCandidates = new LinkedHashSet<Link<L>>();
		
		boolean success = false;
				
		while(!success)
		{
			nodes.clear();
			linksChosen.clear();
			linksCandidates.clear();
		
			Link<L> link = links.generate();
			linksCandidates.add(null);
				
			while(! success && ! linksCandidates.isEmpty())
			{
				linksChosen.add(link);
				
				for(Node<L> node : link.nodes())
					nodes.add(node);
				
				if(nodes.size() == depth)
				{
					success = true;
					break;
				}
				
				linksCandidates.clear();
				for(Link<L> linkChosen : linksChosen)
					for(Node<L> node : linkChosen.nodes())
						for(Link<L> linkCandidate : node.links())
							if(! linksChosen.contains(linkCandidate))
								linksCandidates.add(linkCandidate);
				
				if(! linksCandidates.isEmpty() )
					link = Functions.choose(linksCandidates);
			}	
		}
		
		return new Result(
				new ArrayList<Node<L>>(nodes), 
				new ArrayList<Link<L>>(linksChosen));
	}
	
	public class Result 
	{
		private List<Node<L>> nodes;
		private List<Integer> indices;
		private List<Link<L>> trail;
		private double logProbability = 0.0;
		
		public Result(List<Node<L>> nodes, List<Link<L>> linkTrail)
		{
			this.nodes = nodes;
			this.trail = linkTrail;
			
			indices = new ArrayList<Integer>(nodes.size());
			for(Node<L> node : nodes)
				indices.add(node.index());
			
			calculateProbability();
		}

		private void calculateProbability()
		{
			int n = trail.size();
			for(int[] perm : new Permutations(n))
			{
				double sub = Functions.log2(1.0 / graph.numLinks());
				
				for(int i : series(1, n))
				{
					Node<L> mid = common(trail.get(perm[i-1]), trail.get(perm[i]));
					if(mid == null) // not a proper trail
					{
						sub = 0.0;
						break;
					} else 
						sub += Functions.log2(1.0/(mid.degree()-1.0));
				}
					
					
				logProbability = Functions.logSum(logProbability, sub);
			}
			
		}
		
		private Node<L> common(Link<L> a, Link<L> b)
		{
			List<Node<L>> nodes = new ArrayList<Node<L>>(a.nodes());
			nodes.retainAll(b.nodes());
			
			if(nodes.isEmpty())
				return null;
			
			return nodes.get(0);
		}

		public List<Node<L>> nodes()
		{
			return nodes;
		}
		
		public List<Integer> indices()
		{
			return indices;
		}

		public List<Link<L>> linkTrail()
		{
			return trail;
		}

		public double logProbability()
		{
			return logProbability;
		}
		
		public double invProbability()
		{
			return Math.pow(2.0, -logProbability());
		}
		
		public Graph<L> subgraph()
		{
			return Subgraph.subgraph(graph, nodes);
		}
	}
}
