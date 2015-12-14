#Nodes
=====

Nodes is a general purpose graph library. It focuses on simple creation and traversal of graphs with general objects on their nodes and links. Nodes is a work in progress, it should not be considered production-ready in general, though certain aspects may be well-tested.

## Installation

The simplest way to use nodes is to reference its maven package. We do not build releases for nodes (yet), but the latest snapshot can be included using [jitpack](https://jitpack.io). Simply inclide the following repository:

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

And the following dependency:

```xml
	<dependency>
	    <groupId>com.github.data2semantics</groupId>
	    <artifactId>nodes</artifactId>
	    <version>-SNAPSHOT</version>
	</dependency>
```

## The basics

### Terminology
In order to keep object names and variable descriptors short, Nodes uses some non-standard terminology:

<dl>
  <dt>node</dt><dd>A node is an element in a graph, also known as a vertex.</dd>
  <dt>link</dt><dd>A link is what connects nodes, also known as an edge.</dd>
  <dt>label</dt><dd>Labels are the objects that annotate nodes.</dd>
  <dt>tag</dt><dd>Tags are the objects that annotate links.</dd>
  <dt>U</dt><dd>Undirected, a graph for which the links do not have a specific direction.</dd>
  <dt>D</dt><dd>Directed, a graph for which the links have a specific direction.</dd>
  <dt>T</dt><dd>Tagged, a graph for which the links are annotated with objects</dd>
</dl>

These combinations make for four basic interfaces, which form the backbone of Nodes:

<table>
<tr>
  <th></th><th>untagged</th><th>tagged</th>
</tr>
<tr>
  <th>undirected</th><td>UGraph<L></td><td>UTGraph<L, T></td>
</tr>
<tr>
  <th>directed</th><td>DGraph<L></td><td>DTGraph<L, T></td>
</tr>
</table>

These are all subinterfaces of the basic interface org.nodes.Graph<L>.

There are currently three main implementations available:

<dl>
  <dt>org.nodes.MapDTGraph<L, T></dt><dd>Memory-hungry but fast for lookup and traversal.</dd>
  <dt>org.nodes.MapUTGraph<L, T></dt><dd>Undirected version.</dd>
  <dt>org.nodes.LightDGraph<L></dt><dd>More conservative memory use, but slower for some operations, and no tags.</dd>
</dl>

Some final things to note:
* Every graph object has an implicit sorting over its nodes. Since this is true for any graph representation in the memory of a computer, we've decided to make this sorting explicit by giving nodes a fixed index in the list of nodes.
* Labels and Tags should be immutable objects with solid equals() and hashcode() implementations (treat a graph like a HashSet). Modifying label or tag object so that its hashcode is changed may mess things up (depending on which graph implementation is chosen).
* Labels and tags do not have to be unique, but if a graph has unique labels, there are some methods to make life easier.

### Examples

Creating a graph:
```java
// We're using a DTGraph, but we don't care about the specifics
Graph<String> graph = new MapDTGraph<String, String>();
		
Node<String>  a = graph.add("a"),
                         b = graph.add("b"),
                         c = graph.add("c");

a.connect(c);
graph.node("a").connect(graph.node("b"));

System.out.println(graph);
// result (toString prints DOT format): digraph {a -> c; a -> b}
```
## Feedback

For feedback please use the issues on github, or if that's not an option, send an email to nodes * peterbloem * nl, replacing the asterisks with an at symbol and a period respectively.

### Known problems

* Relabeling nodes and links is currently not possible and would be an expensive operation in the implementations available. We're working on a solution. As a workaround, the best option is to copy the graph over to a new graph. A wrapper object around the original object is a solution, but should be used with care, since these will be stored in hashtables.

## Javadoc
API documentation can be found [here](http://pbloem.github.io/nodes_javadoc/nodes/). Note that this may be out-of-date. For the most recent version, the code should be checked out.

## Acknowledgements

Nodes is produced with funding from the Dutch national research program [COMMIT](http://commit-nl.nl/).

![Commit logo](http://www.delaat.net/logos/raw/logo-COMMIT.jpg)
