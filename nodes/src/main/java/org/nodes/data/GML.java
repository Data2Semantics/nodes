package org.nodes.data;

import static nl.peterbloem.kit.Functions.toString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.UTGraph;
import org.openrdf.query.parser.sparql.StringEscapesProcessor;

import nl.peterbloem.kit.Functions;

/**
 * Methods for reading graph in GML format.
 * 
 * 
 * Works only if all elements (brackets, etc) are separated by whitespace.
 *
 */
public class GML
{
	public static TGraph<String, String> read(File file)
		throws IOException
	{
		Reader reader = new Reader(file);

		return reader.graph();
	}
	
	private static class Reader
	{
		private Map<Integer, Node<String>> nodes = new HashMap<Integer, Node<String>>();
		
		public static final int BUFFER_SIZE = 10;
		public Pattern tokenizer = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		
		private BufferedReader reader;
		private LinkedList<String> buffer = new LinkedList<String>();
		
		private TGraph<String, String> graph;
		private boolean directed;
		
		public Reader(File file) throws IOException
		{
			reader = new BufferedReader(new FileReader(file));
			graph = null;
			
			start();
		}
		
		public TGraph<String, String> graph()
		{
			return graph;
		}
		
		private void read()
		{
			if(buffer.size() > 100)
				return;
			// * Read until whitespace 
			StringBuffer bf = new StringBuffer();
			
			int cr = -1;
			boolean inQuotes = false;
			
			do {
				try
				{
					cr = reader.read();
				} catch (IOException e)
				{
					throw new RuntimeException(e);
				}
				if(cr >= 0)
				{
					bf.append((char)cr);
					
					if((char)cr == '"')
						inQuotes = ! inQuotes;
				}
				
				if(inQuotes && cr == -1)
					throw new IllegalStateException("Stream ended inside quotes.");
				
			} while (
					inQuotes ||
					(
						cr != -1 && 
						(! Character.isWhitespace(bf.charAt(bf.length()-1)) 
									|| bf.length() < BUFFER_SIZE)
					));
			
			// * Tokenize

		    Matcher m = tokenizer.matcher(bf);
		    while(m.find())
				buffer.add(m.group());
		}
		
		private String pop()
		{
			read();
			if(buffer.isEmpty())
				return null;
			return buffer.pop();
		}
		
		
		private void start() 
		{
			String next = pop();
			if(next == null)
				return;
			
			if(next.toLowerCase().equals("creator"))
				readCreator();
			
			if(next.toLowerCase().equals("graph"))
				readGraph();
		}
		
		private void readCreator()
		{
			pop();
			start();
		}
		
		private void readGraph()
		{
			if(pop().equals("["))
				inGraph();
			else 
				throw new IllegalStateException("Keyword 'graph' should be followed by a [");
		}
		
		private void inGraph()
		{
			while(true)
			{
				String next = pop();
				
				if(next.toLowerCase().equals("directed"))
					readDirected();
				
				if(next.toLowerCase().equals("node"))
					readNode();
				
				if(next.toLowerCase().equals("edge"))
					readEdge();
			
				if(next.equals("]"))
					break;
			}
		}
		
		private void readDirected()
		{
			String next = pop();

			if(next.equals("1"))
			{
				directed = true;
				graph = new MapDTGraph<String, String>();
			} else if(next.equals("0"))
			{
				directed = false;
				graph = new  MapUTGraph<String, String>();
			} else
				throw new RuntimeException("Could not read 'directed' statement. Should have been '1' or '0', was '"+next+"'.,");
			
		}
		
		private String label;
		private Integer id;
		
		private void readNode()
		{
			label = null;
			id = null;
			
			if(! pop().equals("["))
				throw new IllegalStateException("Node definition should start with [");
			
			String next = pop();
			while(! next.equals("]"))
			{
				if(next.equals("["))
					readSub();
				
				if(next.toLowerCase().equals("id"))
					id = Integer.parseInt(pop());
				
				if(next.toLowerCase().equals("label"))
					label = pop();
				
				next = pop();	
			}

			if(id == null)
				throw new RuntimeException("Graph description did not contain id.");
			
			nodes.put(id, graph.add(label == null ? ("" + id) : label));
		}
		
		int edges = 0;
		private Integer to, from;	
		private void readEdge()
		{
			to = null;
			from = null;
			if(! pop().equals("["))
				throw new IllegalStateException("Edge definition should start with [");
			
			String next = pop();
			while(! next.equals("]") )
			{
				if(next.toLowerCase().equals("source"))
					from = Integer.parseInt(pop());
				
				if(next.toLowerCase().equals("target"))
					to = Integer.parseInt(pop());
				
				next = pop();
			}
			
			if(to == null)
				throw new RuntimeException("Target id missing from node definition.");
				
			if(from == null)
				throw new RuntimeException("Source id missing from node definition.");
			
			Node<String> fn = nodes.get(from),
			             tn = nodes.get(to);
			
			fn.connect(tn);
			edges++;
		}
		
		/**
		 * Read (and ignores) any attribute containing brackets;
		 */
		private void readSub()
		{	
			String elem;
			do {
				elem = pop();
			} while(!elem.equals("]") && elem != null);

		}
	}
	
	public static <L> String toString(Graph<L> graph)
	{
		StringBuffer bf = new StringBuffer();
		
		bf.append("graph [ \n");
		
		bf.append("\tdirected " + ((graph instanceof DGraph<?>) ? 1 : 0)+"\n");
		
		// * Nodes
		for(Node<L> node : graph.nodes())
		{
			bf.append("\tnode [ \n");
			bf.append("\t\tid " + node.index()+"\n");
			bf.append("\t\tlabel \"" + esc(node.label().toString()) + "\"\n");
			bf.append("\t]\n");
		}
		
		// * Links
		for(Link<L> link : graph.links())
		{
			bf.append("\tedge [ \n");
			bf.append("\t\tsource " + link.first().index()+"\n");
			bf.append("\t\ttarget " + link.second().index()+"\n");
			if(link instanceof TLink<?,?>)
				bf.append("\t\tlabel \"" + esc( Functions.toString( ((TLink<L, ?>)link).tag() )) + "\"\n");
			bf.append("\t]\n");
		}
		
		bf.append("]\n");
		
		return bf.toString();
	}
	
	private static String esc(String in)
	{
		String out = in.replace("\"", "\\\"");
		out = out.replace("\n", "");
		return out;
	}
	
	public static <L> void write(Graph<L> graph, File file)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		out.write(toString(graph));
		
		out.close();		
	}

}
