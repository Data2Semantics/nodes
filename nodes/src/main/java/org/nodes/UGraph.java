package org.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UGraph<L> extends Graph<L>
{
	@Override
	public UNode<L> node(L label);

	@Override
	public Collection<? extends UNode<L>> nodes(L label);
	
	@Override

	public List<? extends UNode<L>> nodes();
	
	@Override
	public UNode<L> get(int i);
	
	@Override
	public Iterable<? extends ULink<L>> links();
	
	@Override
	public UNode<L> add(L label);
	
	public Class<? extends UGraph<?>> level();

}
