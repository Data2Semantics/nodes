package org.nodes.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Map;

import org.nodes.MapUTGraph;
import org.nodes.UGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.data.classification.Classification;
import nl.peterbloem.kit.data.classification.Classified;

/**
 * Load the MUTAG and ENZYME datasets, available here:
 * 
 * 
 * NOTE: Since, the files report each link twice (once for each direction)
 * we only import links where the first node has a lower index than the second. 
 * @author Peter
 *
 */
public class MUTAG {

	public static Classified<UTGraph<Integer, Integer>> read(File directory)
		throws IOException
	{
		Classified<UTGraph<Integer, Integer>> result = Classification.empty();
		
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".graph");
			}
		};
		
		for(File file : directory.listFiles(filter))
			loadGraph(file, result);
		
		Global.log().info(result.size() + " graphs loaded. ");
		return result;
	}
	
	/**
	 * Loads the graph and its class from the given file, and adds them
	 * to the given map.
	 * 
	 * @param file
	 * @param map
	 * 
	 */
	private static void loadGraph(File file, Classified<UTGraph<Integer, Integer>> map)
		throws IOException
	{
		UTGraph<Integer, Integer> result = new MapUTGraph<Integer, Integer>();
		LineNumberReader in = new LineNumberReader(new FileReader(file));
		
		String line = next(in); 
		
		if(! line.startsWith("#v"))
			throw new IOException("Problem with file " + file + ", expected line to start with \"#v\". (line "+in.getLineNumber()+") ");
		
		line = next(in);
		// * Read the nodes and their labels 
		while(line != null && !line.startsWith("#"))
		{
			int label = Integer.parseInt(line.trim());
			
			result.add(label);
			
			line = next(in);
		}
		
		if(line == null)
			throw new IOException("Problem with file " + file + ", expected line to start with \"#e\" or \"#a\". (line "+in.getLineNumber()+") ");
		
		assert(line.startsWith("#"));
		
		if (line.startsWith("#e"))
			readEdgeList(file, in, result);
		else if (line.startsWith("#a"))
			readAdjacencyList(file, in, result);
		else
			throw new IOException("Problem with file " + file + ", expected line to start with \"#e\" or \"#a\". (line "+in.getLineNumber()+") ");

		
		// * read the class label
		line = next(in);
		if(line == null)
			throw new IOException("Problem with file " + file + ", line should contain an integer for the class label. (line "+in.getLineNumber()+") ");

		int cls = Integer.parseInt(line.trim());
		
		map.add(result, cls);
	}

	private static void readEdgeList(File file, LineNumberReader in, UTGraph<Integer, Integer> result)
		throws IOException
	{		
		String line = next(in);

		// * Read the edges and their labels
		while(line != null && !line.startsWith("#c"))
		{
			String[] split = line.split(","); 

			if(split.length != 3)
				throw new IOException("Problem with file "+file+", cannot delimit line into three segments. (line "+in.getLineNumber()+")");
			
			int from, to, label;
			try {
				from = Integer.parseInt(split[0].trim());
			} catch(NumberFormatException e)
			{
				throw new IOException("Problem with file "+file+", first element could not be parsed to integer. (line "+in.getLineNumber()+")");
			}
		
			try {
				to = Integer.parseInt(split[1].trim());
			} catch(NumberFormatException e)
			{
				throw new IOException("Problem with file "+file+", second element could not be parsed to integer. (line "+in.getLineNumber()+")");
			}
			
			try {
				label = Integer.parseInt(split[2].trim());
			} catch(NumberFormatException e)
			{
				throw new IOException("Problem with file "+file+", third element could not be parsed to integer. (line "+in.getLineNumber()+")");
			}
			
			if(from - 1 > result.size() - 1)
				throw new IOException("Problem with file "+file+", first index is too high (index "+from+", number of nodes" + result.size() + "). (line "+in.getLineNumber()+")");
			if(to - 1 > result.size() - 1)
				throw new IOException("Problem with file "+file+", second index is too high (index "+to+", number of nodes" + result.size() + "). (line "+in.getLineNumber()+")");

			if(to > from)
			{
				UTNode<Integer, Integer> fromNode = result.get(from - 1);
				UTNode<Integer, Integer> toNode = result.get(to - 1);
				
				fromNode.connect(toNode, label);
			}
			
			line = next(in);
		}
		
		if(line == null)
			throw new IOException("Problem with file " + file + ", expected line to start with \"#c\". (line "+in.getLineNumber()+") ");
	}
	
	private static void readAdjacencyList(File file, LineNumberReader in,  UTGraph<Integer, Integer> result)
		throws IOException
	{
		String line = next(in);
		int i = 0;

		// * Read the edges and their labels
		while(line != null && !line.startsWith("#c"))
		{
			
			int from = i;
			String[] split = line.split(",");
			for(String sub : split)
			{
				int to = Integer.parseInt(sub.trim()) - 1;
				 
				if(to > from)
					result.get(from).connect(result.get(to));
			}
		
			line = next(in);
			i++;
		}
		
		if(line == null)
			throw new IOException("Problem with file " + file + ", expected line to start with \"#c\". (line "+in.getLineNumber()+") ");
	}
	
	
	private static String next(LineNumberReader reader)
		throws IOException
	{
		String line = reader.readLine();
		while(line != null && line.trim().length() == 0)
			line = reader.readLine();
		
		return line;
	}
}

