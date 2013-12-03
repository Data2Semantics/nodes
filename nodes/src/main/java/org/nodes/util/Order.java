package org.nodes.util;

import static org.nodes.util.Series.series;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nodes.Global;


/**
 * An order represents a shuffling of the integers from one to 'n'
 * 
 * The methods are named from the point of view that this object represents a
 * mapping from a list of things to a re-ordered version of that list.  
 * 
 * @author Peter
 *
 */
public final class Order
{
	private List<Integer> master;
	private Order inverse;
	
	private Order()
	{
		
	}
	
	public Order(List<Integer> values)
	{
		master = new ArrayList<Integer>(values);
		List<Integer> inverseMaster = new ArrayList<Integer>();
		
		for(int i : series(values.size()))
			inverseMaster.add(-1);
		
		for(int i : series(values.size()))
			inverseMaster.set(values.get(i), i);
		
		inverse = new Order();
		inverse.master = inverseMaster;
		inverse.inverse = this;
	}
	
	public int newIndex(int originalIndex)
	{
		return master.get(originalIndex);
	}
	
	public int originalIndex(int newIndex)
	{
		return inverse.newIndex(newIndex);
	}
	
	public Order inverse()
	{
		return inverse;
	}

	public static Order random(int n)
	{
		List<Integer> values = new ArrayList<Integer>(series(n));
		Collections.shuffle(values, Global.random());
		
		return new Order(values);
	}
	
	public String toString()
	{
		return master.toString();
	}

	public int size()
	{
		return master.size();
	}
}
