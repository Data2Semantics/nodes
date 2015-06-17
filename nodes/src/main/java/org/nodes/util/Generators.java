package org.nodes.util;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.util.List;

import org.nodes.Global;


public class Generators
{

	public static Generator<Integer> uniform(int lower, int upper)
	{
		return new UniformGenerator(lower, upper);
	}
	
	private static class UniformGenerator extends AbstractGenerator<Integer>
	{
		private int lower, upper;

		public UniformGenerator(int lower, int upper)
		{
			this.lower = lower;
			this.upper = upper;
		}
		
		@Override
		public Integer generate()
		{	
			return Global.random().nextInt(upper - lower) + lower;
		}
	}
}
