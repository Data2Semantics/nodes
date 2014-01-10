package org.nodes.classification;

import static java.lang.Math.max;
import static org.nodes.util.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nodes.Global;
import org.nodes.draw.Point;
import org.nodes.util.FrequencyModel;
import org.nodes.util.Pair;
import org.nodes.util.Series;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Static utility methods for the task of classification.
 * 
 * @author Peter
 *
 */
public class Classification
{
	
	public static <P> double symmetricError(Classifier<P> classifier, Classified<P> testData)
	{
		double total = testData.size();
		int wrong = 0;
		
		for(int i = 0; i < testData.size(); i++)
			wrong += classifier.classify(testData.get(i)) == testData.cls(i) ? 0 : 1;
		
		return wrong/total;
	}
	
	public static <P> Classified<P> empty()
	{
		return new Combination<P>();		
	}
	
	public static <P> Classified<P> empty(int capacity)
	{
		return new Combination<P>(capacity);
	}	
	
	public static <P> Classified<P> combine(List<P> data, List<Integer> classes)
	{
		return new Combination<P>(data, classes);
	}
	
	private static class Combination<P> implements List<P>, Classified<P>, java.io.Serializable
	{
		private static final long serialVersionUID = -5679638842267261169L;
		private int maxClass = -1;
		private List<P> data;
		private List<Integer> classes;
		
		public Combination() 
		{
			this.data = new ArrayList<P>();
			this.classes = new ArrayList<Integer>();
		}
		
		public Combination(int capacity) 
		{
			this.data = new ArrayList<P>(capacity);
			this.classes = new ArrayList<Integer>(capacity);
		}
		
		public Combination(List<P> data, List<Integer> classes)
		{
			this.data = new ArrayList<P>(data);
			this.classes = new ArrayList<Integer>(classes);
			
			for(int cls : classes)
				maxClass = max(maxClass, cls);
		}


		@Override
		public int cls(int i)
		{
			return classes.get(i);
		}	

		@Override
		public List<Integer> classes()
		{
			return Collections.unmodifiableList(classes);
		}

		@Override
		public boolean add(P item, int cls)
		{
			maxClass = max(maxClass, cls);
			
			classes.add(cls);
			data.add(item);
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends P> c, int cls)
		{
			if(c.isEmpty())
				return false;
			
			for(P item : c)
				add(item, cls);
			return true;
		}

		@Override
		public boolean addAll(int index, Collection<? extends P> c, int cls)
		{
			if(c.isEmpty())
				return false;
			
			for(P item : c)
			    add(index++, item, cls);
			return true;
		}

		@Override
		public P set(int i, P item, int cls)
		{
			maxClass = max(maxClass, cls);
			
			classes.set(i, cls);
			return data.set(i, item);
		}

		@Override
		public P get(int i)
		{
			return data.get(i);
		}

		@Override
		public int size()
		{
			return data.size();
		}

		@Override
		public boolean add(P item)
		{
			throw new UnsupportedOperationException("Items can only be added together with a class");
		}

		@Override
		public void add(int i, P item)
		{
			throw new UnsupportedOperationException("Items can only be added together with a class");			
		}

		@Override
		public boolean addAll(Collection<? extends P> arg0)
		{
			throw new UnsupportedOperationException("Items can only be added together with classes");			
		}

		@Override
		public boolean addAll(int arg0, Collection<? extends P> arg1)
		{
			throw new UnsupportedOperationException();			
		}

		@Override
		public void clear()
		{
			data.clear();
			classes.clear();
		}

		@Override
		public boolean contains(Object item)
		{
			return data.contains(item);
		}

		@Override
		public boolean containsAll(Collection<?> items)
		{
			return data.containsAll(items);
		}

		@Override
		public int indexOf(Object item)
		{
			return data.indexOf(item);
		}

		@Override
		public boolean isEmpty()
		{
			return data.isEmpty();
		}

		@Override
		public Iterator<P> iterator()
		{
			return data.iterator();
		}

		@Override
		public int lastIndexOf(Object item)
		{
			return data.lastIndexOf(item);
		}

		@Override
		public ListIterator<P> listIterator()
		{
			return data.listIterator();
		}

		@Override
		public ListIterator<P> listIterator(int arg0)
		{
			return data.listIterator();
		}

		@Override
		public boolean remove(Object item)
		{
			int i = data.indexOf(item);
			if(i == -1)
				return false;
			
			this.remove(i);
			return true;
		}

		@Override
		public P remove(int i)
		{
			classes.remove(i);
			return data.remove(i);
		}

		@Override
		public boolean removeAll(Collection<?> items)
		{
			boolean modified = false;
			for(Object item : items)
			{
				modified = modified || remove(item);
			}
			return modified;
		}

		@Override
		public boolean retainAll(Collection<?> items)
		{
			boolean modified = false;
			for(P item : this)
				if(! items.contains(item))
				{		
					remove(item);
					modified = true;
				}
			
			return modified;
		}

		@Override
		public P set(int i, P item)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public List<P> subList(int from, int to)
		{
			return data.subList(from, to);
		}

		@Override
		public Object[] toArray()
		{
			return data.toArray();
		}

		@Override
		public <T> T[] toArray(T[] t)
		{
			return data.toArray(t);
		}

		@Override
		public boolean add(int index, P item, int cls)
		{
			data.add(item);
			classes.add(cls);
			return true;
		}

		@Override
		public Classified<P> subClassified(int from, int to)
		{
			return new Combination<P>(data.subList(from, to), classes.subList(from, to));
		}

		@Override
		public int numClasses()
		{
			return maxClass + 1;
		}

		@Override
		public List<P> points(int cls)
		{
			ArrayList<P> points = new ArrayList<P>();
			for(int i = 0; i < this.size(); i ++)
				if(cls(i) == cls)
					points.add(get(i));
			
			return points;
		}

		@Override
		public void setMaxClass(int max) 
		{
			if(max < this.maxClass)
				throw new IllegalArgumentException("Argument ("+max+") cannot be less than current maxClass ("+maxClass+").");
			
			maxClass = max;
		}

		@Override
		public void setClass(int i, int cls)
		{
			classes.set(i, cls);
			if(cls > maxClass)
				maxClass = cls;
		}
	}
	
	/**
	 * Sample n elements randomly with replacement
	 * 
	 * @return
	 */
	public static Classified<Point> sample(Classified<Point> in, int n)
	{

		List<Point> resData = new ArrayList<Point>(n);
		List<Integer> resCls = new ArrayList<Integer>(n);
		
		for(int i : series(n))
		{
			int draw = Global.random().nextInt(in.size());

			resData.add(in.get(draw));
			resCls.add(in.cls(draw));
		}
		
		return combine(resData, resCls);
	}
	
	/**
	 * Returns a random split of the dataset. The first dataset in the 
	 * returned pair contains the ratio of instances indicated by the split 
	 * parameter.
	 * 
	 * @param dataset
	 * @param split
	 * @return
	 */
	public static <M> Pair<Classified<M>, Classified<M>> split(
			Classified<M> dataset, double split)
	{
		int max = dataset.numClasses();
		dataset = copy(dataset);
		
		int nFirst = (int)(dataset.size() * split);
		nFirst += (int)(0.2 * nFirst);
		
		Classified<M> first = Classification.empty(max(16, nFirst));
		
		for(int i : Series.series((int)(dataset.size() * split)))
		{
			int draw = Global.random().nextInt(dataset.size());
			first.add(dataset.get(draw), dataset.cls(draw));
			dataset.remove(draw);
		}
		
		first.setMaxClass(max - 1);

		return new Pair<Classified<M>, Classified<M>>(first, dataset);
	}
	
	/**
	 * Reads a CSV file containing numerical values into a list of points.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Classified<Point> readCSV(File file) throws IOException
	{
		List<Point> points = new ArrayList<Point>();
		List<Integer> classes = new ArrayList<Integer>();
		
	    CSVReader reader = new CSVReader(new FileReader(file));
	    String [] nextLine;
	    while ((nextLine = reader.readNext()) != null) 
	    {
	    	double[] values = new double[nextLine.length - 1];
	    	for(int i = 0; i < nextLine.length - 1; i++)
	    		values[i] = Double.parseDouble(nextLine[i].trim());
	    	points.add(new Point(values));
	    	
	    	int clss = Integer.parseInt(nextLine[nextLine.length - 1].trim());
	    	classes.add(clss);
	    }
	    
	    // * Ensure classes are contiguous (no empty classes)
	    FrequencyModel<Integer> bfm = new FrequencyModel<Integer>(classes); 
	    List<Integer> classNumbers = new ArrayList<Integer>(bfm.tokens());
	    List<Integer> newClasses = new ArrayList<Integer>(classes.size());
	    for(int i : classes)
	    {
	    	newClasses.add(classNumbers.indexOf(i));
	    }
	    
	    return combine(points, newClasses);
	}
	
	public static <M> Classified<M> copy(Classified<M> classified)
	{
		int max = classified.numClasses();
		
		List<M> instances = new ArrayList<M>(classified);
		List<Integer> classes = new ArrayList<Integer>(classified.size());
		
		for(int i : series(classified.size()))
			classes.add(classified.cls(i));
		
		
		Classified<M> copy = combine(instances, classes);
		copy.setMaxClass(max - 1);
		return copy;
	}

	public static void write(Classified<Point> classified, File file)
		throws IOException
	{
		CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(file)));
		
		for(int i : series(classified.size()))
		{
			Point point = classified.get(i);
			int cls = classified.cls(i);
			
			String[] line = new String[point.size()+1];
			for(int j : series(point.size()))
				line[j] = Double.toString(point.get(j));
			line[point.size()] = Integer.toString(cls);
			
			writer.writeNext(line);
		}
		
		writer.close();
	}
}
