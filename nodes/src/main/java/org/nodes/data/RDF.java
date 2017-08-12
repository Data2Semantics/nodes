package org.nodes.data;

import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static org.nodes.data.RDF.simplify;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.LightUGraph;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.UnicodeEscape;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;

public class RDF
{
	public static MapDTGraph<String, String> readHDT(File file) 
		throws FileNotFoundException, IOException
	{
		MapDTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		HDT hdt = HDTManager.loadHDT(
				new BufferedInputStream(new FileInputStream(file)), null);

		int i = 0;
		try {
			// Search pattern: Empty string means "any"
			IteratorTripleString it = hdt.search("", "", "");
			DTNode<String, String> node1, node2;
						
			while(it.hasNext()) {
				TripleString ts = it.next();

				String subject = ts.getSubject().toString(), 
				       predicate = ts.getPredicate().toString(),
				       object = ts.getObject().toString();
				
				node1 = graph.node(subject);
				node2 = graph.node(object);
			
				if (node1 == null) 
					node1 = graph.add(subject);
		
				
				if (node2 == null) 
					node2 = graph.add(object);
								
				node1.connect(node2, predicate);
				
				Functions.dot(i, (int)it.estimatedNumResults());
				i++;
			}
		} catch (NotFoundException e) 
		{
			// File must be empty, return empty graph
		} finally 
		{
			// IMPORTANT: Free resources
			hdt.close();
		}
		
		return graph;
	}
	
	
	/**
	 * Reads the given file into a graph.
	 * 
	 * @param file
	 * @return
	 */
	public static DTGraph<String, String> read(File file) 
			throws IOException
	{		
		RDFFormat format = RDFFormat.forFileName(file.getName());

		InputStream in = new BufferedInputStream(new FileInputStream(file));
		RDFParser parser = Rio.createParser(format);
				
		final DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		parser.setRDFHandler(new RDFHandlerBase()
		{
			int i = 0;
			@Override
			public void handleStatement(Statement statement) 
			{
				String subject = statement.getSubject().toString();
				String object = statement.getObject().toString();
				String verb = statement.getPredicate().toString();
					
				if(graph.node(subject) == null)
					graph.add(subject);
				if(graph.node(object) == null)
					graph.add(object);
					
				DTNode<String, String> subNode = graph.node(subject); 
				DTNode<String, String> obNode = graph.node(object);
				
				subNode.connect(obNode, verb);
				
				if(++i % 2000 == 0)
					System.out.println(i + " statements read.");
			}
		});
		
		try {
			parser.parse(in, "local://");
		} catch (Exception e) 
		{
			throw new RuntimeException("Error parsing file ("+file.getAbsolutePath()+").", e);
		}
		return graph;
	}
	
	public static MapDTGraph<String, String> read(File file, RDFFormat format)
	{
		return read(file, null, format);
	}
	
	public static MapDTGraph<String, String> read(File file, List<String> linkWhitelist)
	{
		return read(file, null, RDFFormat.RDFXML);
	}
	
	public static MapDTGraph<String, String> read(File file, List<String> linkWhitelist, RDFFormat format)
	{
		RDFDataSet testSet = new RDFFileDataSet(file, format);

		List<Statement> triples = testSet.getStatements(null, null, null, false);	
		
		return createDirectedGraph(triples, null, linkWhitelist);
	}
	
	public static MapDTGraph<String, String> readTurtle(File file)
	{
		return readTurtle(file, null);
	}
	
	public static MapDTGraph<String, String> readTurtle(File file, List<String> linkWhitelist)
	{
		RDFDataSet testSet = new RDFFileDataSet(file, RDFFormat.TURTLE);

		List<Statement> triples = testSet.getStatements(null, null, null, false);	
		
		return createDirectedGraph(triples, null, linkWhitelist);
	}
	
	public static MapDTGraph<String, String> createDirectedGraph(
			List<Statement> sesameGraph, 
			List<String> vWhiteList,
			List<String> eWhiteList)
	{
		List<Pattern> vertexWhiteList = null;
		
		if(vWhiteList != null) 
		{
			vertexWhiteList = new ArrayList<Pattern>(vWhiteList.size());
			for(String patternString : vWhiteList)
				vertexWhiteList.add(Pattern.compile(patternString));
		}
		
		
		List<Pattern> edgeWhiteList = null;
		if(eWhiteList != null)
		{
			edgeWhiteList = new ArrayList<Pattern>(eWhiteList.size());
			for(String patternString : eWhiteList)
				edgeWhiteList.add(Pattern.compile(patternString));
		}
		
		MapDTGraph<String, String> graph = new MapDTGraph<String, String>();
		DTNode<String, String> node1, node2;
		
		Global.log().info("Constructing graph (size: "+sesameGraph.size()+")");
		
		int i = 0;
		for (Statement statement : sesameGraph) 
		{
			
			if(vWhiteList != null)
			{
				if(! Functions.matches(statement.getObject().toString(), vertexWhiteList))
					continue;
				if(! Functions.matches(statement.getSubject().toString(), vertexWhiteList))
					continue;
			}
			
			if(eWhiteList != null)
			{
				if(! Functions.matches(statement.getPredicate().toString(), edgeWhiteList))
				{
// 					Global.log().info("Filtered predicate: " + statement.getPredicate().toString());
					continue;
				}
			}
			
			String subject = statement.getSubject().toString(), 
			       object = statement.getObject().toString(), 
			       predicate = statement.getPredicate().toString();
									
			node1 = graph.node(subject);
			node2 = graph.node(object);
		
			if (node1 == null) 
				node1 = graph.add(subject);
	
			
			if (node2 == null) 
				node2 = graph.add(object);
							
			node1.connect(node2, predicate);
			
			Functions.dot(i, sesameGraph.size());
			i++;
		}	
		
		return graph;
	}
	
	private static final int SIMPLIFY_MAX_LENGTH = 23;
	private static final int SIMPLIFY_INCLUDE_TWO_URI_LEVELS = 4;
	
	/**
	 * Simplifies an RDF URI to retain most of its information
	 * @param string
	 * @return
	 */
	public static String simplify(String string)
	{
		if(string == null)
			return null;
		
		String out;
		if(string.contains("^^"))
		{
			String[] split = string.split("\\^\\^");
			out = split[0];
		} else if(string.contains("/"))
		{
				String[] split = string.split("/");
				if(split.length > 3 && split[split.length - 1].length() < SIMPLIFY_INCLUDE_TWO_URI_LEVELS)
					out = split[split.length - 2] + "/" + split[split.length - 1];
				else
					out = split[split.length - 1];
		} else if(string.trim().startsWith("_"))
		{
			out = "_";
		} else 
		{
			out = string;
		}
		
		out = out.replace("\"", "");

		if(out.length() > SIMPLIFY_MAX_LENGTH)
		{
			int half = (SIMPLIFY_MAX_LENGTH - 3) / 2;
			out = out.substring(0, half) + "..." + out.substring(out.length()-half,out.length());
		}
		
		return out;
	}
	
	public static DTGraph<String, String> simplify(DTGraph<String, String> graph)
	{
		DTGraph<String, String> out = new MapDTGraph<String, String>();
		
		for(Node<String> node : graph.nodes())
			out.add(simplify(node.label()));
		
		for(DTLink<String, String> link : graph.links())
			out.get(link.first().index()).connect(out.get(link.second().index()), simplify(link.tag()));
	
		return out;
	}
	
	/**
	 * Reads a simple graph: no self-loops, no multiple edges. Two resources
	 * have an edge if they are connected in either direction by one or more predicates
	 * 
	 * @param file
	 * @return
	 */
	public static UGraph<String> readSimple(File file)
		throws IOException
	{	
		return readSimple(file, null);
	}	
	
	public static UGraph<String> readSimple(File file, List<String> whiteList)
		throws IOException
	{
		if(file.getAbsolutePath().endsWith("hdt"))
			return readSimpleHDT(file, whiteList);
		
		List<Pattern> patterns = whiteList != null ? toPatterns(whiteList) : null;
		
		RDFFormat format = RDFFormat.forFileName(file.getName());
		
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		RDFParser parser = Rio.createParser(format);
				
		final UGraph<String> graph = new LightUGraph<String>();
		final Map<String, UNode<String>> nodes = new HashMap<String, UNode<String>>();
		
		parser.setRDFHandler(new RDFHandlerBase()
		{
			  @Override
			  public void handleStatement(Statement statement) 
			  {
				String predicate = statement.getPredicate().toString();
				
				if(patterns == null || matches(predicate, patterns))
				{
    				String subject = statement.getSubject().toString();
    				String object = statement.getObject().toString();
    					
    				if(! nodes.containsKey(subject))
    					nodes.put(subject, graph.add(subject));
    				if(! nodes.containsKey(object))
    					nodes.put(object, graph.add(object));
    					
    				UNode<String> subNode = nodes.get(subject); 
    				UNode<String> obNode = nodes.get(object);
    				
    				if( (!subNode.connected(obNode)) &&  subNode.index() != obNode.index() )
    					subNode.connect(obNode);
				}
			  }
		});
		
		try {
			   parser.parse(in, "local://");
		} catch (Exception e) 
		{
				throw new RuntimeException("Error parsing file ("+file.getAbsolutePath()+").", e);
		}
		return graph;
	}
	
	/**
	 * Reads a simple graph: no self-loops, no multiple edges. Two resources
	 * have an edge if they are connected in either direction by one or more predicates
	 * 
	 * @param file
	 * @return
	 */
	public static UGraph<String> readSimpleHDT(File file)
		throws IOException
	{	
		return readSimple(file, null); 
	}
	
	public static UGraph<String> readSimpleHDT(File file, List<String> whiteList)
			throws IOException
	{	
		List<Pattern> patterns = whiteList != null ? toPatterns(whiteList) : null;
		
		final UGraph<String> graph = new LightUGraph<String>();
		final Map<String, UNode<String>> nodes = new HashMap<String, UNode<String>>();
		
		HDT hdt = HDTManager.loadHDT(
				new BufferedInputStream(new FileInputStream(file)), null);

		// Search pattern: Empty string means "any"
		IteratorTripleString it;
		try
		{
			it = hdt.search("", "", "");
		} catch (NotFoundException e)
		{
			throw new RuntimeException(e);
		}
	
		DTNode<String, String> node1, node2;
				
		
		Global.log().info("Start loading graph: " + it.estimatedNumResults() + " triples (estimated).");
		
		long read = 0;
		tic();
		while(it.hasNext()) 
		{
			TripleString ts = it.next();

			String predicate = ts.getPredicate().toString();
			
			if(patterns == null || matches(predicate, patterns))
			{
    			String subject = ts.getSubject().toString();
    			String object = ts.getObject().toString();
    			
    			subject = UnicodeEscape.unescapeString(subject);
    			if(! object.startsWith("\""))
    				object  = UnicodeEscape.unescapeString(object);
    				
    			if(! nodes.containsKey(subject))
    				nodes.put(subject, graph.add(subject));
    			if(! nodes.containsKey(object))
    				nodes.put(object, graph.add(object));
    				
    			UNode<String> subNode = nodes.get(subject); 
    			UNode<String> obNode = nodes.get(object);
    			
    			if( (!subNode.connected(obNode)) &&  subNode.index() != obNode.index() )
    				subNode.connect(obNode);
			}
			
			if(toc() > 600)
			{
				Global.log().info("Reading graph in progress: " + graph.size() + " nodes, " + graph.numLinks() + " links added so far. " + read + " triples read.");
				tic();
			}
			read++;
		}
		
		Global.log().info("Graph read.");

		return graph;
	}


	private static boolean matches(String predicate, List<Pattern> patterns)
	{
		for(Pattern pattern : patterns)
			if(pattern.matcher(predicate).matches())
				return true;
		
		return false;
	}


	private static List<Pattern> toPatterns(List<String> whiteList)
	{
		List<Pattern> res = new ArrayList<Pattern>(whiteList.size());
		
		for(String str : whiteList)
			res.add(Pattern.compile(str));
			
		return res;
	}
}
