package org.nodes.walks;

import java.util.Iterator;

import org.nodes.Node;

public abstract class AbstractWalk<L> implements Walk<L>
{
	@Override
	public Iterable<L> labels()
	{
		return new Labels();
	}
	
	private class Labels implements Iterable<L>
	{
		@Override
		public java.util.Iterator<L> iterator()
		{
			return new Iterator();
		}
		
		
		private class Iterator implements java.util.Iterator<L>
		{
			java.util.Iterator<Node<L>> master = AbstractWalk.this.iterator();

			@Override
			public boolean hasNext()
			{
				return master.hasNext();
			}

			@Override
			public L next()
			{
				return master.next().label();
			}

			@Override
			public void remove()
			{
				master.remove();
			}

		}

	}

	
}
