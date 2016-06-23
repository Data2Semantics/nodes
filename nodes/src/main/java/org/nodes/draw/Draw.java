package org.nodes.draw;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static nl.peterbloem.kit.Series.series;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DegreeIndexComparator;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.Node;
import org.nodes.util.BufferedImageTranscoder;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import nl.peterbloem.kit.Series;


public class Draw
{
	private static final String ns = SVGDOMImplementation.SVG_NAMESPACE_URI;
	
	public static <L> BufferedImage draw(Graph<L> graph, int width)
	{
		return draw(graph, new CircleLayout<L>(graph), width);
	
	}
	
	public static <L> BufferedImage draw(Graph<L> graph, Layout<L> layout, int width)
	{
		SVGDocument svg = svg(graph, layout);
		return draw(svg, width);
	}
	
	public static <L> BufferedImage draw(SVGDocument svg, int width)
	{
		BufferedImageTranscoder t = new BufferedImageTranscoder();
		
	    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH,  (float) width);
	    // t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);
	    
	   // t.addTranscodingHint(PNGTranscoder.KEY_,  (float) width);
	
	    TranscoderInput input = new TranscoderInput(svg);
	    try
		{
			t.transcode(input, null);
		} catch (TranscoderException e)
		{
			throw new RuntimeException(e);
		}
	 
	    return t.getBufferedImage();
	}
	
	public static <L> Document svg(Graph<L> graph)
	{
		return svg(graph, new CircleLayout<L>(graph));
	}	
	
	public static <L> void write(Document svg, File file)
		throws IOException
	{
		 try
		 {
			  // Use a Transformer for output
			  TransformerFactory tFactory =
			    TransformerFactory.newInstance();
			  Transformer transformer = tFactory.newTransformer();
	
			  DOMSource source = new DOMSource(svg);
			  StreamResult result = new StreamResult(new FileOutputStream(file));
		
				transformer.transform(source, result);
		} catch (TransformerException e)
		{
			throw new RuntimeException(e);
		} 
	}
	
	public static <L> SVGDocument svg(Graph<L> graph, Layout<L> layout)
	{
		// * Set up an SVG generator
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		SVGDocument doc = (SVGDocument) impl.createDocument(ns, "svg", null);

		Element canvas = doc.getDocumentElement();
		canvas.setAttributeNS(null, "width", "2.0");
		canvas.setAttributeNS(null, "height", "2.0");
		
		Element g = doc.createElementNS(ns, "g");
		g.setAttributeNS(null, "transform", "translate(1,1)");
		canvas.appendChild(g);
				
		// * Draw the edges
		for(Link<L> link : graph.links())
		{
			Point a = layout.point(link.first()),
			      b = layout.point(link.second());
			
			
			Element line = doc.createElementNS(ns, "line");
			line.setAttributeNS(null, "x1", "" + a.get(0));
			line.setAttributeNS(null, "y1", "" + a.get(1));
			line.setAttributeNS(null, "x2", "" + b.get(0));
			line.setAttributeNS(null, "y2", "" + b.get(1));

			line.setAttributeNS(null, "stroke", "black");
			line.setAttributeNS(null, "stroke-opacity", "0.1");
			line.setAttributeNS(null, "stroke-width", "0.005");

			g.appendChild(line);
		}	
		
		// * Draw the nodes
		for(Node<L>  node : graph.nodes())
		{
			Point p = layout.point(node);
			
			Element circle = doc.createElementNS(ns, "circle");
			circle.setAttributeNS(null, "cx", "" + p.get(0));
			circle.setAttributeNS(null, "cy", "" + p.get(1));
			circle.setAttributeNS(null, "r", "0.01");
			circle.setAttributeNS(null, "fill", "red");

			g.appendChild(circle);
		}
		
	
		
		return doc;
	}
	
	public static <L> void show(Graph<L> graph)
	{
		
	}
	
	/**
	 * Draws a density plot of the adjacency matrix
	 * 
	 * @param graph
	 * @param layout
	 * @param width
	 * @param height
	 * @return
	 */
	public static <L> BufferedImage matrix(
			Graph<L> graph, int width, int height)
	{
		return matrix(graph, width, height, null);
	}
	
	public static <L> BufferedImage matrix(
			Graph<L> graph, int width, int height, List<Integer> order)
	{
		boolean log = true;
		boolean directed = graph instanceof DGraph<?>;
		
		float max = Float.NEGATIVE_INFINITY;
		float[][] matrix = new float[width][];
		for(int x = 0; x < width; x++)
		{
			matrix[x] = new float[height];
			for(int y = 0; y < height; y++)
				matrix[x][y] = 0.0f;				
		}
		
		int xp, yp;
		for(Link<L> link : graph.links())
		{
			int i = link.first().index(), j = link.second().index();

			int ii = order == null ? i : order.get(i);
			int jj = order == null ? j : order.get(j);
			
			xp = toPixel(ii, width, 0, graph.size()); 
			yp = toPixel(jj, height, 0, graph.size());
		
			if(xp >= 0 && xp < width && yp >= 0 && yp < height)
			{
				matrix[xp][yp] ++;
				max = Math.max(matrix[xp][yp], max);
			}
			
			if(! directed)
			{
				// * Swap
				int t = ii;
				ii = jj;
				jj = t;
				
				xp = toPixel(ii, width, 0, graph.size()); 
				yp = toPixel(jj, height, 0, graph.size());
			
				if(xp >= 0 && xp < width && yp >= 0 && yp < height)
				{
					matrix[xp][yp] ++;
					max = Math.max(matrix[xp][yp], max);
				}
			}
		}
		
		BufferedImage image = 
			new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		Color color;
		
		if(log)
			max = (float)Math.log(max + 1.0f);
		
		for(int x = 0; x < width; x++)
			for(int y = 0; y < height; y++)
			{
				//float value = matrix[x][yRes - y - 1];
				float value = matrix[x][y];				
				if(log)
					value = (float)Math.log(value + 1.0f);
		
				float gray = value / max;
				
				if(gray < 0.0)
					color = Color.BLUE;
				else if(gray > 1.0)
					color = Color.RED;
				else				
					color  = new Color(gray, gray, gray, 1.0f);
				
				image.setRGB(x, y, color.getRGB());
			}
		
		return image;
	}
	
	/**
	 * REMINDER: 
	 * 
	 * - The order has order.get(indexInOriginalGraph) = indexInOrderedGraph
	 * - The inverseOrder has inverseOrder.get(indexInOrderedGraph) = indexInOriginalGraph
	 * 
	 * in the order the elements represent the indices of the ordered graph
	 * in the inverseOrder the elements represent the indices of the original graph
	 * 
	 * @param graph
	 * @return
	 */
	public static <L> List<Integer> degreeOrdering(Graph<L> graph)
	{
		List<Integer> inv = new ArrayList<Integer>(Series.series(graph.size()));
		
		Comparator<Integer> comp =
				Collections.reverseOrder(new DegreeIndexComparator(graph));
		
		Collections.sort(inv, comp);
		
		return inverse(inv);
	}
	
	public static List<Integer> inverse(List<Integer> order)
	{
		List<Integer> inv = new ArrayList<Integer>(order.size());
		for(int i : series(order.size()))
			inv.add(null);
		
		for(int index : series(order.size()))
			inv.set(order.get(index), index);
		
		return inv;
	}
	
	/**
	 * Converts a double value to its index in a given range when that range 
	 * is discretized to a given number of bins. Useful for finding pixel values 
	 * when creating images.
	 * 
	 * @param coord 
	 * @param res
	 * @param rangeStart
	 * @param rangeEnd
	 * @return The pixel index. Can be out of range (negative of too large).
	 */
	public static int toPixel(double coord, int res, double rangeStart, double rangeEnd)
	{
		double pixSize = abs(rangeStart - rangeEnd) / (double) res;
		return (int)floor((coord - rangeStart)/pixSize);
	}
	
	/**
	 * 
	 * @param pixel
	 * @param res
	 * @param rangeStart
	 * @param rangeEnd
	 * @return
	 */
	public static double toCoord(int pixel, int res, double rangeStart, double rangeEnd){
		double pixSize = abs(rangeStart - rangeEnd) / (double) res;
		return pixSize * ((double) pixel) + pixSize * 0.5 + rangeStart;		
	}				
}
