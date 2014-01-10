package org.nodes.rdf;

import java.util.Comparator;

import org.nodes.Node;

public class ScorerComparator implements Comparator<Token>
{
	private Scorer scorer;
	private int depth;

	public ScorerComparator(Scorer scorer)
	{
		super();
		this.scorer = scorer;
	}

	@Override
	public int compare(Token t1, Token t2)
	{
		return Double.compare(
				scorer.score(t1.node(), t1.depth()), 
				scorer.score(t2.node(), t2.depth())
			);
	}
	
}
