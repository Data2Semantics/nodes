package org.nodes.util;

import java.util.Collection;

/**
 * A KT estimator for observing and encoding a sequence of models.
 * 
 * Put simply, this encoder keeps a running model, encoding each symbol observed 
 * with its current model, and then updating the model. 
 * 
 * @author Peter
 *
 * @param <T>
 */
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
	 * counts.
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
	 * @return The probability of the given symbol according to the current model
	 * as it is before the method is called.  
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
