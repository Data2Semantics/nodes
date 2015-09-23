package org.nodes.data;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.HashAttributeSet;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.UTGraph;
import org.nodes.clustering.ConnectionClusterer;
import org.nodes.clustering.ConnectionClusterer.ConnectionClustering;

public class DataTest {

	@Test
	public void testReadString() 
	{
		URL url = this.getClass().getResource("/graphs/ecoli/EC.dat");
		File file = new File(url.getFile());
		
		UTGraph<String, String> graph = null;
		try {
			graph = Data.edgeList(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(graph.size());
		System.out.println(graph.numLinks());
	}

	@Test
	public void testReadStringNeural() 
	{
		URL url = this.getClass().getResource("/graphs/neural/celegans.txt");
		File file = new File(url.getFile());
		
		UTGraph<String, String> graph = null;
		try {
			graph = Data.edgeList(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(graph.size());
		System.out.println(graph.numLinks());
	}
	
	@Test
	public void createCitSMall()
		throws IOException, ParseException
	{
		System.out.println("Loading dates");
		Map<String, Date> dates = new HashMap<String, Date>();
		BufferedReader reader = new BufferedReader(new FileReader(new File("/Users/Peter/Documents/datasets/graphs/cit/dates-th.txt")));
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		String line;
		do {
			line = reader.readLine();
			
			if(line == null)
				continue;
			if(line.trim().startsWith("#"))
				continue;
			if(line.trim().startsWith("%"))
				continue;			
			
			String[] split = line.split("\\s");
			String id = split[0];
			Date date = sdf.parse(split[1]);
			
			if((! dates.containsKey(id)) || date.before(dates.get(id)))
				dates.put(id, date);
			
		} while(line != null);
		System.out.println(dates.size() + " dates read.");
		
		System.out.println("Loading graph");
		File gFile = new File("/Users/Peter/Documents/datasets/graphs/cit/cit-th.txt");
		
		reader = new BufferedReader(new FileReader(gFile));
		DGraph<String> graph = new MapDTGraph<String, String>();
				
		int i = 0;
		int cutoff = 94;

		do {
			line = reader.readLine();
			i++;

			if(line == null)
				continue;
			if(line.trim().isEmpty())
				continue;
			if(line.trim().startsWith("#"))
				continue;
			if(line.trim().startsWith("%"))
				continue;
			
			String[] split = line.split("\\s");
			if(split.length < 2)
				throw new IllegalArgumentException("Line "+i+" does not split into two elements.");
			
			String from, to = null;
		
			from = split[0];			
			to = split[1];
			
			if(! dates.containsKey(from))
				continue;
			if(dates.get(from).getYear() >= cutoff)
				continue;
			
			if(! dates.containsKey(to))
				continue;
			if(dates.get(to).getYear() >= cutoff)
				continue;
			
			DNode<String> nodeFrom, nodeTo;
			nodeFrom = graph.node(from);
			if(nodeFrom == null)
				nodeFrom = graph.add(from);
			nodeTo = graph.node(to);
			if(nodeTo == null)
				nodeTo = graph.add(to);
			
			nodeFrom.connect(nodeTo);

		} while(line != null);
				
				
		System.out.println("Graph size: " + graph.size() + ".");
		System.out.println("num links: " + graph.numLinks());
				
		System.out.println("Removing...");
		List<Node<String>> toRemove = new ArrayList<Node<String>>(graph.size());
		
		for(Node<String> node : graph.nodes())
			if (node.degree() == 0)
				toRemove.add(node);
		
		for(Node<String> node : toRemove)
			node.remove();
		
		System.out.println("Done.");
		System.out.println("Graph size " + graph.size() + ".");
		
		System.out.println("Removing links...");
		List<DLink<String>> linksToRemove = new ArrayList<DLink<String>>();
		for(DLink<String> link : graph.links())
		{
			Node<String> from = link.from(), to = link.to();
			Date dateFrom = dates.get(from.label()), dateTo = dates.get(to.label());
			
			if(dateFrom.before(dateTo))
			{
				linksToRemove.add(link);
				System.out.println(dateFrom + " " + dateTo);
			}
		}
		
		System.out.println("Finding LCC.");
		graph = (DGraph<String>) ConnectionClusterer.largest(graph);
		System.out.println("Graph size " + graph.size() + ".");
		System.out.println("num links: " + graph.numLinks());
		
		for(DLink<String> link : linksToRemove)
			link.remove();
		
		System.out.println("Graph size " + graph.size() + ".");
		System.out.println("num links: " + graph.numLinks());		
		
		Data.writeEdgeList(graph, new File("/Users/Peter/Documents/datasets/graphs/cit/simple2.txt"));
	}
	
	@Test
	public void loadDBPedia()
		throws IOException
	{
		File in = new File("/Users/Peter/Documents/datasets/graphs/wikipedia-nl/wikipedia-nl.txt");
		File out = new File("/Users/Peter/Documents/datasets/graphs/wikipedia-nl/wikipedia-nl-simple.txt");
		
		DGraph<String> graph = Data.edgeListDirectedUnlabeledSimple(in);
		
		System.out.println(Graphs.isSimple(graph));

		System.out.println(graph.size());
		System.out.println(graph.numLinks());
		
		Data.writeEdgeList(graph, out);
	}
}
