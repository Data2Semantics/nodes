package org.nodes;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class contains methods and objects that are used globally by all other 
 * classes. 
 * 
 * It contains, for example, a single random number generator that all
 * classes are supposed to use for random numbers. This rng can then be set to 
 * an instance with a specific seed, to make all experiments deterministic and 
 * reproducible.
 *
 * For commonly useful methods available for all classes that do not have this 
 * mandatory character, see org.lilian.util.Functions  
 * 
 */

public class Global
{
	/**
	 * The default random seed. May be changed during runtime.
	 */
	public static final int RANDOM_SEED = 42;
	
	private static Random random = new Random(RANDOM_SEED);
	public static Random random()
	{
		return random;
	}
	
	/**
	 * Gives the RNG a random seed (based on a reasonable source of randomsness,
	 * such as time). This makes the code non-deterministic and non-repeatable. 
	 */
	public static void randomSeed()
	{
		random = new Random();
	}
	
	public static void setSeed(long seed)
	{
		random = new Random(seed);
	}
	
	/**
	 * Shorthand for the global logger
	 * @return
	 */
	public static Logger log() { return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); }
	
}
