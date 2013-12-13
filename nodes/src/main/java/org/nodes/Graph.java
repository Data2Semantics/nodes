package org.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 
 * 
 * Design choices:
 * <ul>
 * <li>A Graph is _not_ a Collection of Nodes, because the generic type cannot 
 * be further restricted in the subclasses of graph. Eg. we cannot make DTGraph
 * a collection of DTnodes. thus the client must call nodes() to get a 
 * collection of the graphs nodes. </li>
 * <li>To maintain the LSP, the subclasses of Node can be modified by all types 
 * of node (if we restricted this to subclasses of nodes we would be restricting 
 * preconditions in a subclass). In practice, all implementations have a single 
 * type of node and only allow that type, but that rule is not enforced at the 
 * API level.</li>
 * </ul>
 * 
 * TODO: Create a separate UGraph & UTGraph. Undirected graphs behave differently than 
 * directed graphs. Letting Graph be the interface for undirected graphs violates
 * LSP.
 * 
 * <h2>A note on equality and inheritance</h2>
 * <p>
 * Our choice to use inheritance in the definition of graphs presents a problem
 * with the implementation of equals. Consider the following: a DTGraph 
 * implementation considers another graph equal to itself if its nodes, links, 
 * labels and tags match. For a DGraph implementation, there are no tags, so 
 * only the nodes, links and labels are checked. The second considers itself 
 * equal to the first, but not vice versa. The DGraph has no way of knowing that 
 * the DTGraph has extended functionality, since it isn't aware that such
 * functionality exists.
 * </p><p>
 * The solution is that equality is only defined over a single level of the 
 * hierarchy. Each graph reports not only which graphtypes it inherits from 
 * (through interfaces), but also which level of the graph hierarchy it 
 * identifies with. This can be either an interface, or a class, but for two 
 * graphs to be equal, their level must match.
 * </p>
 *        
 * 
 * 
 * 
 * @param <L>
 */
public interface Graph<L>
{
	/**
	 * Returns the first node in the Graph which has the given label 
	 * 
	 * @param label
	 * @return
	 */
	public Node<L> node(L label);
	
	public Collection<? extends Node<L>> nodes(L label);
	
	public List<? extends Node<L>> nodes();
	
	/**
	 * Returns a collection of links currently in the graph. Note that 
	 * modifying the graph while iterating over its links can cause undefined 
	 * behavior.
	 * @return
	 */
	public Collection<? extends Link<L>> links();
	
	/**
	 * @return The graph's size in nodes.
	 */
	public int size();
	
	/**
	 * Adds a new node with the given label 
	 */
	public Node<L> add(L label);
	
	public int numLinks();
	
	/**
	 * Returns the node labels
	 * @return
	 */
	public Set<L> labels();
	
	/**
	 * Checks whether two nodes exist with the given labels that are connected.
	 * 
	 * If multiple pairs of nodes exist with these labels, only one of them 
	 * needs to be connected for the method to return true.
	 *  
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean connected(L first, L second);

	/**
	 * The state of a graph indicates whether it has changed. If the value 
	 * returned by this method has changed, then a modification has been made.
	 *  
	 * If the value is the same, then with great probability, the graph has not 
	 * been modified. 
	 * 
	 * Please note that this value should only be used in reference to the same
	 * graph object. The state of one graph bears no relation to the state of 
	 * another. Ie. it should be thought of as a mod count rather than a hash 
	 * code.
	 * 
	 * @return
	 */
	public long state();
	
	/**
	 * Shorthand for nodes().get(i);
	 * @param i
	 * @return
	 */
	public Node<L> get(int i);
	
	public Class<? extends Graph<?>> level();
	
}
