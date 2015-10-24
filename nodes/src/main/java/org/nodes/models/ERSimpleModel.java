package org.nodes.models;

import static org.nodes.util.Functions.log2;
import static org.nodes.util.Functions.log2Choose;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.UGraph;
import org.nodes.util.Functions;

public class ERSimpleModel implements StructureModel<Graph<? extends Object>>, RestrictedToSimple
{
	private boolean withPrior;
	
	/**
	 * 
	 * @param withPrior If true, includes the bits required to specify the size
	 * and number of links. If false, this is a 'cheating code' (ie. a lowerbound
	 * on the true codelength).
	 */
	public ERSimpleModel(boolean withPrior)
	{
		this.withPrior = withPrior;
	}

	@Override
	public double codelength(Graph<? extends Object> graph)
	{
		if(graph instanceof UGraph<?>)
		{
			double n = graph.size();
			double t = n * (n - 1) / 2;
			
			try {
				return (withPrior ? Functions.prefix((int)n) + log2(t): 0) + log2Choose(graph.numLinks(), t);	
			} catch (RuntimeException e)
			{
				System.out.println(graph);
				throw e;
			}
			
		} else
		{
		
			double n = graph.size();
			double t = n * n - n;
			
			return (withPrior ? Functions.prefix(graph.size()) + log2(t) : 0) + log2Choose(graph.numLinks(), t);
		}
	}
	

}
