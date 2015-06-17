package org.nodes.util;

import java.util.Collection;

/**
 * A KT estimator for observing and encoding a sequence of tokens.
 * 
 * Put simply, this encoder keeps a running model, encoding each symbol observed 
 * with its current model, and then updating the model. 
 * 
 * This model requires a record of all symbols that will be observed (the 
 * collection used in the constructor) before symbols are observed. If these are
 * not available in a collection and performance is important, the following 
 * approach is equivalent
 * <pre>
 *   OnlineModel model = new OnlineModel(Collections.emptyList());
 * 
 *   for(T symbol : ...)
 *   {
 *   	model.add(symbol, 0.0);
 *   }
 *   
 *   // start observing
 * </pre>
 * 
 * Note that if this pattern is used, the user MUST ensure that all tokens are 
 * added before observe is called for the first time.
 * 
 * TODO: Check that something like model.add("x", 15.3) still works.
 * 
 * @author Peter
 *
 * @param <T>
 */
public class OnlineModel<T> extends FrequencyModel<T>
{
	private double smoothing = 0.5;
	
	public OnlineModel(Collection<T> symbols)
	{
		for(T symbol : symbols)
			add(symbol, 0.0);
	}
	
	/**
	 * Adds a symbol to the dictionary without incrementing any frequencies
	 * 
	 * @param symbol
	 */
	public void addToken(T symbol)
	{
		add(symbol, 0.0);
	}
	
	
	
	@Override
	public void add(T token)
	{
		if((! frequencies.containsKey(token)) && started())
			throw new IllegalStateException("Observations have started, no new symbols can be added (Attempted to add symbol "+token+").");
		
		super.add(token);
	}

	public OnlineModel(double smoothing)
	{
		this.smoothing = smoothing;
	}
	
	/**
	 * Combines the act of calculating the probability under the online model 
	 * and observing it
	 *  
	 * @return The probability of the given symbol according to the current model
	 * as it is before the symbol is added. 
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
	
	/**
	 * Whether observations have started (in which case no new symbols may be added).
	 * @return
	 */
	public boolean started()
	{
		return total() > 0.0;
	}

}
