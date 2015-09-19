package org.nodes;

import java.util.Collection;
import java.util.Set;

public interface Node<L>
{
	/**
	 * The neighbors of this node. Each node occurs once, even if there are
	 * multiple links.
	 * 
	 * Will include the node itself if there are multiple links.
	 * 
	 * @return
	 */
	public Collection<? extends Node<L>> neighbors();
	
	/**
	 * Returns a collection of all links to neighboring nodes
	 * 
	 * @return
	 */
	public Collection<? extends Link<L>> links();
	
	public L label();
	
	public Node<L> neighbor(L label);

	public Collection<? extends Node<L>> neighbors(L label);
	
	/** 
	 * <p>Connects this node to another node. </p>
	 * <p>
	 * The only prescription is that if this method succeeds, the other node 
	 * shows up in this nodes' {@link neighbours()}</p>
	 * <p>
	 * The particulars of the connection 
	 * are not prescribed by this interface, nor does this interface prescribe 
	 * what should happen when the connection already exists. </p>
	 *  
	 * @param other
	 */
	public Link<L> connect(Node<L> other);
	
	/**
	 * Removes all links existing between this node and the given node.
	 * 
	 * For directed graphs, this removes links going in both directions.
	 * 
	 * @param other
	 */
	public void disconnect(Node<L> other);
	
	/**
	 * Whether the current node is connected to the given node.
	 * 
	 * If the graph is directed, a connection in either direction will cause
	 * true to be returned. 
	 * 
	 * @param other
	 * @return
	 */
	public boolean connected(Node<L> other);
	
	/** 
	 * Returns all links between this node and the given other node. 
	 * 
	 * For directed graphs, this will return links in both directions.
	 * 
	 * @param other
	 * @return
	 */
	public Collection<? extends Link<L>> links(Node<L> other);

	
	/**
	 * Disconnects and removes this node from the graph
	 * 
	 */
	public void remove();
	
	/**
	 * Returns the graph object to which these nodes belong. Nodes always belong 
	 * to a single graph and cannot be exchanged between them. This is a very 
	 * important property for the correctness of the API.
	 * @return
	 */
	public Graph<L> graph();
	
	/**
	 * The index of the node in the graph to which it belongs
	 * @return
	 */
	public int index();
	
	/**
	 * Since clients can maintain links to nodes that have been removed 
	 * from the graph, there is a danger of these nodes being used and 
	 * causing mayhem. 
	 * 
	 * To prevent such situations such nodes have an explicit a state 
	 * of 'dead'. Using dead nodes in any way (except calling this method) 
	 * can result in undefined behavior. 
	 * 
	 * A dead node is never reachable from a live node and vice versa
	 * 
	 * @return
	 */
	public boolean dead();	
	
	/**
	 * Returns the degree of the node, ie. the number of connections to other 
	 * nodes. Note that this value will differ from neighbors.size() if there
	 * are multiple links between this node and one of its neighbors.
	 * @return
	 */
	public int degree();
}