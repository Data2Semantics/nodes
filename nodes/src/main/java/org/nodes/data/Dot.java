package org.nodes.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nodes.MapUTGraph;
import org.nodes.UTGraph;
import org.nodes.UTNode;

public class Dot {
	
	private static Pattern list = Pattern.compile(".*\\{(.*)\\}");
	private static Pattern labeledLink = Pattern.compile("\\s*(.*)\\s*--\\s*(.*)\\s*\\[label=(.*)\\]");
	private static Pattern unlabeledLink = Pattern.compile("\\s*(.*)\\s--\\*(.*)\\s*");	

	public static UTGraph<String, String> read(String string)
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
			m = labeledLink.matcher(element);
			
			if(m.matches())
			{
				String n1 = m.group(1);
				String n2 = m.group(2);
				String tag = m.group(3);
				
				node(n1, map, graph).connect(node(n2, map, graph), tag);
			
				continue;
			}
			
			m = unlabeledLink.matcher(element);
			if(m.matches())
			{
				String n1 = m.group(1);
				String n2 = m.group(2);
			
				node(n1, map, graph).connect(node(n2, map, graph));
						
				continue;
			}
			
			node(element.trim(), map, graph);			
		}
		
		return graph;
	}
	
	private static UTNode<String, String> node(
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
}

