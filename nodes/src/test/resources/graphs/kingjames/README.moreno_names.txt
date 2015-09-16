King James network, part of the Koblenz Network Collection
===========================================================================

This directory contains the TSV and related files of the moreno_names network:

This undirected network contains nouns – places and names – of the King James bible and information about their occurences. A node represents one of the above noun types and an edge indicates that two nouns appeared together in the same verse. The edge weights show how often two nouns occured together.


More information about the network is provided here: 
http://konect.uni-koblenz.de/networks/moreno_names

Files: 
    meta.moreno_names -- Metadata about the network 
    out.moreno_names -- The adjacency matrix of the network in space separated values format, with one edge per line
      The meaning of the columns in out.moreno_names are: 
        First column: ID of from node 
        Second column: ID of to node
        Third column: edge weight


Complete documentation about the file format can be found in the KONECT
handbook, in the section File Formats, available at:

http://konect.uni-koblenz.de/publications

All files are licensed under a Creative Commons Attribution-ShareAlike 2.0 Germany License.
For more information concerning license visit http://konect.uni-koblenz.de/license.



Use the following References for citation:

@MISC{konect:2014:moreno_names,
    title = {King James network dataset -- {KONECT}},
    month = oct,
    year = {2014},
    url = {http://konect.uni-koblenz.de/networks/moreno_names}
}

@misc{konect:harrison,
  howpublished = {\url{http://chrisharrison.net/projects/bibleviz/index.html}},
  note = {Accessed: 2014-08-22}
}


@inproceedings{konect,
	title = {{KONECT} -- {The} {Koblenz} {Network} {Collection}},
	author = {Jérôme Kunegis},
	year = {2013},
	booktitle = {Proc. Int. Conf. on World Wide Web Companion},
	pages = {1343--1350},
	url = {http://userpages.uni-koblenz.de/~kunegis/paper/kunegis-koblenz-network-collection.pdf}, 
	url_presentation = {http://userpages.uni-koblenz.de/~kunegis/paper/kunegis-koblenz-network-collection.presentation.pdf},
}


