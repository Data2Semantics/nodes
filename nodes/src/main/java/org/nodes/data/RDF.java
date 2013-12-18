package org.nodes.data;

import static org.nodes.data.RDF.simplify;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nodes.Global;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.util.Functions;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;

public class RDF
{
	/**
	 * Reads the given file into a graph.
	 * 
	 * @param file
	 * @return
	 */
	public static MapDTGraph<String, String> read(File file)
	{
		return read(file, null);
	}
	
	public static MapDTGraph<String, String> read(File file, List<String> linkWhitelist)
	{
		RDFDataSet testSet = new RDFFileDataSet(file, RDFFormat.RDFXML);

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
		
		Global.log().info("Constructing graph");
		
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
}
