package org.nodes.data;

import java.io.File;
import java.io.IOException;

import org.nodes.DGraph;
import org.nodes.UGraph;

public class Examples
{
	/**
	 * This directed network captures innovation spread among 246 physicians in 
	 * four towns in Illinois, Peoria, Bloomington, Quincy and Galesburg. The 
	 * data was collected in 1966. A node represents a physician and an edge 
	 * between two physicians shows that the left physician told that the right 
	 * physician is his friend or that he turns to the right physician if he 
	 * needs advice or is interested in a discussion. There always only exists
	 * one edge between two nodes even if more than one of the listed 
	 * conditions are true.
	 *  
	 * More information about the network is provided here: 
	 * http://konect.uni-koblenz.de/networks/moreno_innovation
	 *  
	 * All files are licensed under a Creative Commons Attribution-ShareAlike 
	 * 2.0 Germany License. For more information concerning license visit 
	 * http://konect.uni-koblenz.de/license.
	 *  
	 * @return
	 */
	public static DGraph<String> physicians()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/physicians/physicians.txt").getFile());
		
		try
		{
			return Data.edgeListDirected(file,true);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the physician graph from the classpath.", e);
		}
	}
	
	/**
	 * This undirected network contains nouns (places and names) of the King 
	 * James bible and information about their occurrences. A node represents 
	 * one of the above noun types and an edge indicates that two nouns appeared
	 * together in the same verse. The edge weights show how often two nouns 
	 * occurred together.
	 * 
	 * More information about the network is provided here: 
	 * http://konect.uni-koblenz.de/networks/moreno_names
	 *  
	 * All files are licensed under a Creative Commons Attribution-ShareAlike 
	 * 2.0 Germany License. For more information concerning license visit 
	 * http://konect.uni-koblenz.de/license.
	 *  
	 * @return
	 */
	public static UGraph<String> kingjames()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/kingjames/kingjames.txt").getFile());
		
		try
		{
			return Data.edgeList(file, false, true);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the kingjames graph from the classpath.", e);
		}
	}
	
	/**
	 * This is the collaboration network between Jazz musicians.  Each node is a
	 * Jazz musician and an edge denotes that two musicians have played together 
	 * in a band.  The data was collected in 2003.
	 *  
	 * More information about the network is provided here:
	 * http://konect.uni-koblenz.de/networks/arenas-jazz
	 * 
	 * @return
	 */
	public static UGraph<String> jazz()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/jazz/jazz.txt").getFile());
		
		try
		{
			return Data.edgeList(file, false, true);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the jazz graph from the classpath.", e);
		}
	}
	
	/**	
	 * 
	 * This undirected network contains protein interactions contained in yeast. 
	 * Research showed that proteins with a high degree were more important for 
	 * the surivial of the yeast than others. A node represents a protein and an
	 * edge represents a metabolic interaction between two proteins.
	 * 
	 * More information about the network is provided here: 
	 *   http://konect.uni-koblenz.de/networks/moreno_propro 
	 * 
	 * Complete documentation about the file format can be found in the KONECT
	 * handbook, in the section File Formats, available at: 
	 * 		http://konect.uni-koblenz.de/publications
	 * 
	 * All files are licensed under a Creative Commons Attribution-ShareAlike 2.0 Germany License
	 * For more information concerning license visit http://konect.uni-koblenz.de/license.
	 */
	public static UGraph<String> yeast()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/yeast/yeast.txt").getFile());
		
		try
		{
			return Data.edgeList(file, false, true);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the yeast graph from the classpath.", e);
		}
	}
	
	/**
	 * A small citations graph extracted from the larger KDD Cup 2003 dataset:
	 *   http://www.cs.cornell.edu/projects/kddcup/datasets.html
	 *   
	 * Includes only papers from before 1994, citations into the future are 
	 * removed.
	 * 
	 * @return
	 */
	public static UGraph<String> citations()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/citations/citations.txt").getFile());
		
		try
		{
			return Data.edgeList(file, false, true);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the citations graph from the classpath.", e);
		}
	}
	
	/**
	 * A snapshot of part of the Gnutella P2P network. Source:
	 * http://snap.stanford.edu/data/p2p-Gnutella30.html
	 *   
	 * @return
	 */
	public static DGraph<String> p2p()
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("graphs/p2p/p2p.txt").getFile());
		
		try
		{
			return Data.edgeListDirected(file);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the P2P graph from the classpath.", e);
		}
	}
}
