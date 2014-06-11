package org.nodes.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Graph;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;

public class Dot {
	
	private static Pattern list = Pattern.compile(".*\\{(.*)\\}");
	private static Pattern labeledLinkUndirected = Pattern.compile("\\s*(.*)\\s*--\\s*(.*)\\s*\\[label=(.*)\\]");
	private static Pattern unlabeledLinkUndirected = Pattern.compile("\\s*(.*)\\s--\\s(.*)\\s*");	
	
	private static Pattern labeledLinkDirected = Pattern.compile("\\s*(.*)\\s*->\\s*(.*)\\s*\\[label=(.*)\\]");
	private static Pattern unlabeledLinkDirected = Pattern.compile("\\s*(.*)\\s->\\s(.*)\\s*");	

	public static Graph<String> read(String string)
	{
		if(string.trim().toLowerCase().startsWith("digraph"))
			return readDT(string);
		if(string.trim().toLowerCase().startsWith("graph"))
			return readUT(string);
		else throw new IllegalArgumentException("String representation should start with 'digraph' or 'graph'");
	}
	
	public static UTGraph<String, String> readUT(String string)
	{
		Map<String, UTNode<String, String>> map = 
				new HashMap<String, UTNode<String,String>>(); 
		Matcher m = list.matcher(string);
		
		m.matches();
		
		String inner = m.group(1);
		String[] elements = inner.split(";");
		
		UTGraph<String, String> graph = new MapUTGraph<String, String>();
		
		for(String element : elements)
		{
			m = labeledLinkUndirected.matcher(element);
			
			if(m.matches())
			{
				String n1 = m.group(1).trim();
				String n2 = m.group(2).trim();
				String tag = m.group(3);
				
				nodeUT(n1, map, graph).connect(nodeUT(n2, map, graph), tag);
			
				continue;
			}
			
			m = unlabeledLinkUndirected.matcher(element);
			if(m.matches())
			{
				String n1 = m.group(1).trim();
				String n2 = m.group(2).trim();
			
				nodeUT(n1, map, graph).connect(nodeUT(n2, map, graph));
						
				continue;
			}
			
			nodeUT(element.trim(), map, graph);			
		}
		
		return graph;
	}
	
	private static UTNode<String, String> nodeUT(
			String name, 
			Map<String, UTNode<String, String>> map, 
			UTGraph<String, String> graph)
	{
		if(map.containsKey(name))
			return map.get(name);
		
		String label = name;
		if(name.contains("_"))
			label = name.split("_")[0];
		
		UTNode<String, String> node = graph.add(label);
		map.put(name, node);
		
		return node;
	}
	
	public static DTGraph<String, String> readDT(String string)
	{
		Map<String, DTNode<String, String>> map = 
				new HashMap<String, DTNode<String,String>>(); 
		Matcher m = list.matcher(string);
		
		m.matches();
		
		String inner = m.group(1);
		String[] elements = inner.split(";");
		
		DTGraph<String, String> graph = new MapDTGraph<String, String>();
		
		for(String element : elements)
		{
			m = labeledLinkDirected.matcher(element);
			
			if(m.matches())
			{
				String n1 = m.group(1).trim();
				String n2 = m.group(2).trim();
				String tag = m.group(3);
				
				nodeDT(n1, map, graph).connect(nodeDT(n2, map, graph), tag);
			
				continue;
			}
			
			m = unlabeledLinkDirected.matcher(element);
			if(m.matches())
			{
				String n1 = m.group(1).trim();
				String n2 = m.group(2).trim();
			
				nodeDT(n1, map, graph).connect(nodeDT(n2, map, graph));
						
				continue;
			}
			
			nodeDT(element.trim(), map, graph);			
		}
		
		return graph;
	}
	
	private static DTNode<String, String> nodeDT(
			String name, 
			Map<String, DTNode<String, String>> map, 
			DTGraph<String, String> graph)
	{
		if(map.containsKey(name))
			return map.get(name);
		
		String label = name;
		if(name.contains("_"))
			label = name.split("_")[0];
		
		DTNode<String, String> node = graph.add(label);
		map.put(name, node);
		
		return node;
	}
	
	public static <L> void write(Graph<L> graph, File file)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		out.write(graph.toString());
		
		out.close();
	}
}

