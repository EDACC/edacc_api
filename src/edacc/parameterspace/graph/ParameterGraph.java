package edacc.parameterspace.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;

@XmlRootElement( name="parameterspace" )
public class ParameterGraph {
	@XmlIDREF public AndNode startNode;
	public Set<Node> nodes;
	public Set<Parameter> parameters;
	public List<Edge> edges;
	
	private Map<Node, List<Edge>> adjacenct_edges; // internal adjacency list
	
	private ParameterGraph() {
		
	}
	
	public ParameterGraph(Set<Node> nodes, List<Edge> edges, Set<Parameter> parameters, AndNode startNode) {
		this.startNode = startNode;
		this.nodes = nodes;
		this.edges = edges;
		this.parameters = parameters;
		buildAdjacencyList();
	}
	
	public void buildAdjacencyList() {
		this.adjacenct_edges = new HashMap<Node, List<Edge>>();
		for (Edge e: edges) {
			if (!this.adjacenct_edges.containsKey(e.getSource())) this.adjacenct_edges.put(e.getSource(), new LinkedList<Edge>());
			this.adjacenct_edges.get(e.getSource()).add(e);
		}
	}
	
	private Set<Node> adjacentNodes(Node node) {
		Set<Node> nodes = new HashSet<Node>();
		if (!adjacenct_edges.containsKey(node)) return nodes;
		for (Edge e: adjacenct_edges.get(node)) {
			nodes.add(e.getTarget());
		}
		return nodes;
	}
	
	private Set<Node> preceedingNodes(OrNode node) {
		Set<Node> nodes = new HashSet<Node>();
		for (Node n: this.nodes) {
			if (!(n instanceof OrNode)) continue;
			for (Edge e: adjacenct_edges.get(n)) {
				if (e.getTarget().equals(node)) nodes.add(n);
			}
		}
		return nodes;
	}
	
	private Node preecedingNode(AndNode node) {
		for (Node n: this.nodes) {
			if (!(n instanceof OrNode)) continue;
			for (Edge e: adjacenct_edges.get(n)) {
				if (e.getTarget().equals(node)) return n;
			}
		}
		return null;
	}
	
	private List<Edge> incomingEdges(Node node) {
		List<Edge> edges = new LinkedList<Edge>();
		for (Node n: this.nodes) {
			if (!this.adjacenct_edges.containsKey(n)) continue;
			for (Edge e: this.adjacenct_edges.get(n)) {
				if (e.getTarget().equals(node)) edges.add(e);
			}
		}
		return edges;
	}
	
	private boolean incomingEdgesDone(Node node, Set<AndNode> doneAndNodes) {
		Map<Integer, List<Edge>> edge_groups = new HashMap<Integer, List<Edge>>();
		for (Edge e: incomingEdges(node)) {
			if (!edge_groups.containsKey(e.getGroup())) edge_groups.put(e.getGroup(), new LinkedList<Edge>());
			edge_groups.get(e.getGroup()).add(e);
		}
		
		if (edge_groups.containsKey(new Integer(0))) {
			for (Edge e: edge_groups.get(0)) {
				if (doneAndNodes.contains(e.getSource())) return true;
			}
		}
		
		boolean any_group_done = false;
		for (Integer group: edge_groups.keySet()) {
			if (group == 0) continue;
			boolean all_group_edges_done = true;
			for (Edge e: edge_groups.get(group)) all_group_edges_done &= doneAndNodes.contains(e.getSource());
			any_group_done |= all_group_edges_done;
		}
		
		return any_group_done;
	}
	
	private <T> T randomElement(Set<T> set, Random rng) {
		int i = rng.nextInt(set.size());
		int j = 0;
		for (T o: set) {
			if (i == j++) return o;
		}
		return null;
	}
	
	public ParameterConfiguration getRandomConfiguration(Random rng) {
		ParameterConfiguration config = new ParameterConfiguration(this.parameters);
		
		Set<AndNode> done_and = new HashSet<AndNode>();
		done_and.add(this.startNode);
		Set<OrNode> done_or = new HashSet<OrNode>();
		
		Set<OrNode> L = new HashSet<OrNode>();
		for (Node n: adjacentNodes(this.startNode)) {
			if (n instanceof OrNode) L.add((OrNode)n);
		}
		
		while (true) {
			Set<OrNode> openOrNodes = new HashSet<OrNode>();
			for (OrNode n: L) 
				if (incomingEdgesDone(n, done_and))
					openOrNodes.add(n);
			if (openOrNodes.isEmpty()) break;
			OrNode or_node = randomElement(openOrNodes, rng);
			L.remove(or_node);
			done_or.add(or_node);
			
			Set<AndNode> adjacentAndNodes = new HashSet<AndNode>();
			for (Node n: adjacentNodes(or_node)) {
				if (n instanceof AndNode) adjacentAndNodes.add((AndNode)n);
			}
			AndNode and_node = randomElement(adjacentAndNodes, rng);
			
			if (config.getParameterValue(and_node.getParameter()) == null) {
				config.setParameterValue(and_node.getParameter(), and_node.getDomain().randomValue(rng));
			}
			done_and.add(and_node);
			
			for (Node n: adjacentNodes(and_node)) {
				if (n instanceof OrNode) L.add((OrNode)n);
			}
		}
		
		return config;
	}
	
	public void mutateParameterConfiguration(Random rng, ParameterConfiguration config) {
		Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
		for (Node n: this.nodes) {
			if (n == startNode || !(n instanceof AndNode)) continue;
			if (config.getParameter_instances().containsKey(((AndNode)n).parameter) &&
				((AndNode)n).getDomain().contains(config.getParameterValue(((AndNode)n).parameter))) {
				assigned_and_nodes.add((AndNode)n);
			}
		}
		
		for (AndNode n: assigned_and_nodes) {
			config.setParameterValue(n.getParameter(), n.getDomain().mutatedValue(rng, config.getParameterValue(n.getParameter())));
		}
	}
	
	public boolean validateParameterConfiguration(ParameterConfiguration config) {
		// TODO: implement
		return false;
	}
}
