package org.nodes.models;

import org.nodes.Graph;
import org.nodes.util.Compressor;

/**
 * 
 * @author Peter
 *
 * @param <L>
 * @param <G>
 */
public interface Model<T> 
{
	public double logProb(T token);
}
