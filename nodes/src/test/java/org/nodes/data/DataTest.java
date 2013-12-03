package org.nodes.data;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.nodes.UTGraph;

public class DataTest {

	@Test
	public void testReadString() 
	{
		File file = new File("/home/peter/Documents/datasets/graphs/ecoli/EC.dat");
		
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
		File file = new File("/home/peter/Documents/datasets/graphs/neural/celegans.txt");
		
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
