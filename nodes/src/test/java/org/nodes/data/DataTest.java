package org.nodes.data;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.nodes.UTGraph;

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
	
}
