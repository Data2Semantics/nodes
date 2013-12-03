package org.nodes.util;

import java.util.Collection;

public class OnlineModel<T> extends FrequencyModel<T>
{
	private double smoothing = 0.5;
	
	public OnlineModel()
	{
		
	}
	
	public OnlineModel(double smoothing)
	{
		this.smoothing = smoothing;
	}
	
	/**
	 * Adds these symbols to the model's store, without incrementing their 
	 * 
	 * 
	 * @param symbols
	 */
	public void symbols(Collection<T> symbols)
	{
		for(T symbol : symbols)
			add(symbol, 0.0);
	}
	
	/**
	 * Combines the act of calculating the probability under the online model 
	 * and observing it
	 *  
	 * @return
	 */
	public double observe(T symbol)
	{
		double p = probability(symbol);
		
		add(symbol);
		
		return p;
	}
	
	@Override
	public double probability(T symbol)
	{
		if(distinct() == 0.0)
			return Double.NaN;
			
		return (frequency(symbol) + smoothing) / (total() + smoothing * distinct());
	}

}
