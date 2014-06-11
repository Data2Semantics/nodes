package org.nodes.gephi;

import static org.nodes.util.Functions.toString;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGOMSVGElement;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.preview.SVGExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.nodes.DGraph;
import org.nodes.Graph;
import org.nodes.Link;
import org.nodes.TGraph;
import org.nodes.TLink;
import org.nodes.UGraph;
import org.nodes.draw.Draw;
import org.nodes.util.Functions;
import org.nodes.util.Series;
import org.openide.util.Lookup;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.xml.sax.SAXException;

/**
 * Utility methods from exporting from and to Gephi, and using their drawing 
 * library.
 * 
 * @author Peter
 *
 */
public class Gephi
{	
	public static <L> org.gephi.graph.api.Graph gephiGraph(Graph<L> nodesGraph)
	{
		Workspace workspace;
	
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		workspace = pc.getCurrentWorkspace();
		
		boolean directed = nodesGraph instanceof DGraph<?>; 
		
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		
		
		List<Node> gephiNodes = new ArrayList<Node>(nodesGraph.size());
		List<Edge> gephiEdges = new ArrayList<Edge>(nodesGraph.numLinks());
		
		// * Add all nodes
		for(org.nodes.Node<L> node : nodesGraph.nodes())
		{
			Node gNode = graphModel.factory().newNode(node.index() + "");
			
			gNode.getNodeData().setLabel(Functions.toString(node.label()));
			gNode.getNodeData().setSize(10f);
						
			gephiNodes.add(gNode);
		}
		
		for(Link<L> link : nodesGraph.links())
		{
			int i = link.first().index(), j = link.second().index();
			Node iNode = gephiNodes.get(i), jNode = gephiNodes.get(j);
			
			Edge edge = graphModel.factory().newEdge(iNode,  jNode, 1f, directed);
			
			if(link instanceof TLink<?, ?>)
			{
				Object tag = ((TLink<?, ?>)link).tag();
				edge.getEdgeData().setLabel(tag == null ? "" : tag.toString());
			}
			
			gephiEdges.add(edge);
		}
		
		org.gephi.graph.api.Graph out = null;
		if(directed)
			out = graphModel.getDirectedGraph();
		else 
			out = graphModel.getUndirectedGraph();
		
		for(Node node : gephiNodes)
			out.addNode(node);
		for(Edge edge : gephiEdges)
			out.addEdge(edge);
		
		System.out.println("Graph has " + out.getEdgeCount() + " edges");
		
		return out;
	}
	

	
	public static <L> BufferedImage draw(Graph<L> graph, int width)
	{	
		SVGDocument svg = svg(gephiGraph(graph));
		return Draw.draw(svg, width);
	}

	public static SVGDocument svg(org.gephi.graph.api.Graph graph)
	{
		try
		{
			double n = graph.getNodeCount();
			double m = graph.getEdgeCount();
			
			GraphModel graphModel = null;
			if(graph instanceof DirectedGraph)
				graphModel = ((DirectedGraph)graph).getGraphModel();
			if(graph instanceof UndirectedGraph)
				graphModel = ((UndirectedGraph)graph).getGraphModel();
			
			// * Layout with Yifan Hu
			
			ForceAtlas2 layout = new ForceAtlas2Builder().buildLayout();
			
	        layout.setGraphModel(graphModel);
	        layout.setScalingRatio(200.0);
	        layout.setAdjustSizes(true);
	        
//			YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
//			
//			layout.setGraphModel(graphModel);
//			layout.resetPropertiesValues();
//			layout.setOptimalDistance((float)(500.0 / Math.sqrt(n)));
			
			layout.initAlgo();
			for(int i : Series.series(1000))
			{
				if(! layout.canAlgo())
					break;
				
				layout.goAlgo();
			}
			
			PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
			model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
			model.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, true);
			
			model.getProperties().putValue(PreviewProperty.NODE_BORDER_COLOR, new DependantColor(Color.BLACK));
			model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 2f);
			model.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, true);

			model.getProperties().putValue(PreviewProperty.EDGE_CURVED, true);
			model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.BLACK));
			model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(1f));
			model.getProperties().putValue(PreviewProperty.EDGE_OPACITY, new Float(10f));

			
			
			model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, 
					model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
			
			ExportController ec = Lookup.getDefault().lookup(ExportController.class);
			
			// * export the SVG to a byte array
			SVGExporter exp = (SVGExporter) ec.getExporter("svg");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(baos);
			
			ec.exportWriter(writer, exp);
			baos.flush();
			byte[] ba = baos.toByteArray();
			
			// * read the byte array
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			
			String parser = XMLResourceDescriptor.getXMLParserClassName();
			SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
			SVGDocument svg = f.createSVGDocument("http://www.peterbloem.nl", bais);
			
			UserAgent      userAgent;
			DocumentLoader loader;
			BridgeContext  ctx;
			GVTBuilder     builder;
			GraphicsNode   rootGN;
			
			userAgent = new UserAgentAdapter();
			loader    = new DocumentLoader(userAgent);
			ctx       = new BridgeContext(userAgent, loader);
			ctx.setDynamicState(BridgeContext.DYNAMIC);
			builder   = new GVTBuilder();
			rootGN    = builder.build(ctx, svg);

			AffineTransform at = rootGN.getGlobalTransform();
			Rectangle2D aoi = rootGN.getTransformedBounds(at);
			
			SVGOMSVGElement root = (SVGOMSVGElement) svg.getElementsByTagName("svg").item(0);
			
			SVGRect r = root.getBBox();
			System.out.println(r.getX() + " " + r.getY() + " " + r.getWidth() + r.getHeight());
			
			svg.getRootElement().setAttribute("viewBox", 
					r.getX() + " " + r.getY() + " "
							+ (int)r.getWidth() + " " + (int)r.getHeight());
						
			return svg;
			
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		} 
		
	}
}