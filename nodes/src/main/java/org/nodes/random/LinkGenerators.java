package org.nodes.random;

import java.util.ArrayList;
import java.util.List;

import org.nodes.Global;
import org.nodes.util.AbstractGenerator;
import org.nodes.util.Generator;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UTGraph;
import org.nodes.UTLink;

/**
 * A linkgenerator prodcuees a random link from a given graph
 * @author Peter
 *
 */
public class LinkGenerators 
{	
	public static class LinkGenerator<T> extends AbstractGenerator<Link<T>>
	{
		protected List<Link<T>> links;

		public LinkGenerator(Graph<T> graph)
		{
			links = new ArrayList<Link<T>>(graph.links());
		}

		@Override
		public Link<T> generate()
		{
			int i = Global.random().nextInt(links.size());
			return links.get(i);
		}		
	}
	
	public static class ULinkGenerator<L> extends LinkGenerator<L>
	{
		public ULinkGenerator(UGraph<L> graph)
		{
			super(graph);
		}

		@Override
		public ULink<L> generate()
		{
			return (ULink<L>)super.generate();
		}
	}

	public static class DLinkGenerator<L> extends LinkGenerator<L>
	{
		public DLinkGenerator(DGraph<L> graph)
		{
			super(graph);
		}

		@Override
		public DLink<L> generate()
		{
			return (DLink<L>)super.generate();
		}
	}
	
	public static class UTLinkGenerator<L, T> extends LinkGenerator<L>
	{
		public UTLinkGenerator(UTGraph<L, T> graph)
		{
			super(graph);
		}

		@SuppressWarnings("unchecked")
		@Override
		public UTLink<L, T> generate()
		{
			return (UTLink<L, T>)super.generate();
		}
	}

	public static class DTLinkGenerator<L, T> extends LinkGenerator<L>
	{
		public DTLinkGenerator(DTGraph<L, T> graph)
		{
			super(graph);
		}

		@SuppressWarnings("unchecked")
		@Override
		public DTLink<L, T> generate()
		{
			return (DTLink<L, T>)super.generate();
		}
	}
}
