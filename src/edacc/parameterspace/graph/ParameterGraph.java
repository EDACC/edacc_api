package edacc.parameterspace.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.OrdinalDomain;
import edacc.util.Pair;

@XmlRootElement( name="parameterspace" )
public class ParameterGraph {
	@XmlIDREF public AndNode startNode;
	public Set<Node> nodes;
	public Set<Parameter> parameters;
	public List<Edge> edges;
	
	private Map<Node, List<Edge>> adjacent_edges; // internal adjacency list
	private Map<Parameter, Object> fixedParameters;
	
	@SuppressWarnings("unused")
    private ParameterGraph() {
	    this.fixedParameters = new HashMap<Parameter, Object>();
	}

	public ParameterGraph(Set<Node> nodes, List<Edge> edges, Set<Parameter> parameters, AndNode startNode) {
		this.startNode = startNode;
		this.nodes = nodes;
		this.edges = edges;
		this.parameters = parameters;
		buildAdjacencyList();
		this.fixedParameters = new HashMap<Parameter, Object>();
	}
	
	/**
	 * Set a subset of parameters to be fixed, i.e. they don't have to be considered
	 * in neighbourhood calculation. Their values will be set to null in the resulting configurations.
	 * @param parameters
	 */
	public void setFixedParameters(Map<Parameter, Object> parameters) {
	    if (parameters == null) throw new IllegalArgumentException("fixedParameters can't be set to null");
	    fixedParameters = parameters;
	}
	
	public Map<String, Parameter> getParameterMap() {
		Map<String, Parameter> map = new HashMap<String, Parameter>();
		for (Parameter p: this.parameters) {
			map.put(p.getName(), p);
		}
		return map;
	}
	
	public Set<Parameter> getParameterSet() {
		return java.util.Collections.unmodifiableSet(this.parameters);
	}
	
	public void buildAdjacencyList() {
		this.adjacent_edges = new HashMap<Node, List<Edge>>();
                if (edges == null) {
                    edges = new LinkedList<Edge>();
                }
		for (Edge e: edges) {
			if (!this.adjacent_edges.containsKey(e.getSource())) this.adjacent_edges.put(e.getSource(), new LinkedList<Edge>());
			this.adjacent_edges.get(e.getSource()).add(e);
		}
	}
	
	private Set<Node> adjacentNodes(Node node) {
		Set<Node> nodes = new HashSet<Node>();
		if (!adjacent_edges.containsKey(node)) return nodes;
		for (Edge e: adjacent_edges.get(node)) {
			nodes.add(e.getTarget());
		}
		return nodes;
	}
	
	/*private Set<Node> preceedingNodes(OrNode node) {
		Set<Node> nodes = new HashSet<Node>();
		for (Node n: this.nodes) {
			if (!(n instanceof OrNode)) continue;
			for (Edge e: adjacent_edges.get(n)) {
				if (e.getTarget().equals(node)) nodes.add(n);
			}
		}
		return nodes;
	}*/
	
	private OrNode preceedingNode(AndNode node) {
		for (OrNode n: getOrNodes()) {
			for (Edge e: adjacent_edges.get(n)) {
				if (e.getTarget().equals(node)) return n;
			}
		}
		return null;
	}
	
	private List<Edge> incomingEdges(Node node) {
		List<Edge> edges = new LinkedList<Edge>();
		for (Node n: this.nodes) {
			if (!this.adjacent_edges.containsKey(n)) continue;
			for (Edge e: this.adjacent_edges.get(n)) {
				if (e.getTarget().equals(node)) edges.add(e);
			}
		}
		return edges;
	}
	
	private Set<OrNode> getOrNodes() {
		Set<OrNode> s = new HashSet<OrNode>();
		for (Node n: this.nodes) {
			if (n instanceof OrNode) s.add((OrNode)n);
		}
		return s;
	}
	
	private Set<AndNode> getAndNodes() {
		Set<AndNode> s = new HashSet<AndNode>();
		for (Node n: this.nodes) {
			if (n instanceof AndNode) s.add((AndNode)n);
		}
		return s;
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
	
	private boolean valuesEqual(Object v1, Object v2) {
		if (v1 instanceof Double || v1 instanceof Float) {
			if (!(v2 instanceof Number)) return false;
			double cur_val = ((Number)v2).doubleValue();
			double val = (Double)v1;
			if (cur_val - 0.00000001 < val && val < cur_val + 0.00000001) return true;
		}
		
		return v1.equals(v2);
	}
	
	/**
	 * Color (assign integers) the nodes of the transitive closure starting at currentNode.]
	 * @param currentNode
	 * @param color
	 * @param colors
	 * @return
	 */
	private int colorDFS(Node currentNode, int color, Map<Node, Integer> colors) {
		if (colors.containsKey(currentNode) && colors.get(currentNode) != color) return colors.get(currentNode);
		colors.put(currentNode, color);
		for (Node n: adjacentNodes(currentNode)) {
			int c = colorDFS(n, color, colors);
			if (c != color) return c;
		}
		return color;
	}
	
	/**
	 * Creates a random parameter configuration.
	 * @param rng Random number generator instance
	 * @return random parameter configuration
	 */
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
		
		for (Parameter fp: fixedParameters.keySet()) {
		    config.setParameterValue(fp, fixedParameters.get(fp));
		}
		
		config.updateChecksum();
		return config;
	}
	
	/**
	 * Returns all parameter configurations that are considered neighbours of the given configuration.
	 * This neighbourhood is constrained to values that are located in the same AND-nodes as the values
	 * of the given configuration. 
	 * @param config
	 * @return (constrained) list of all neighbouring configurations
	 */
	public List<ParameterConfiguration> getConstrainedNeighbourhood(ParameterConfiguration config) {
		Set<OrNode> assigned_or_nodes = new HashSet<OrNode>();
		Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
		for (Parameter p: config.getParameter_instances().keySet()) {
    		for (AndNode n: getAndNodes()) {
    			if (n == startNode) continue;
    			if (n.getParameter().equals(p) && n.getDomain().contains(config.getParameterValue(p))) {
    				assigned_or_nodes.add(preceedingNode(n));
    				assigned_and_nodes.add(n);
    			}
    		}
		}
		
		List<ParameterConfiguration> nbh = new LinkedList<ParameterConfiguration>();
		for (AndNode node: assigned_and_nodes) {
		    if (fixedParameters.containsKey(node.getParameter())) continue;
			for (Object value: preceedingNode(node).getParameter().getDomain().getDiscreteValues()) {
				if (node.getDomain().contains(value)) { // same subdomain, different value
					if (valuesEqual(value, config.getParameterValue(node.getParameter()))) continue;
					ParameterConfiguration neighbour = new ParameterConfiguration(config);
					neighbour.setParameterValue(node.getParameter(), value);
					neighbour.updateChecksum();
					nbh.add(neighbour);
				}
			}
			
		}
		
		return nbh;
	}
	
	/**
	 * Returns all parameter configurations that are considered neighbours of the given configuration.
	 * @param config The configuration of which the neighbourhood should be generated.
	 * @return list of all neighbouring configurations
	 */
	public List<ParameterConfiguration> getNeighbourhood(ParameterConfiguration config) {
		//Map<Parameter, OrNode> assigned_or_nodes = new HashMap<Parameter, OrNode>();
		Map<Parameter, AndNode> old_assigned_and_nodes = new HashMap<Parameter, AndNode>();
		for (Parameter p: config.getParameter_instances().keySet()) {
    		for (AndNode n: getAndNodes()) {
    			if (n == startNode) continue;
    			if (n.getParameter().equals(p) && n.getDomain().contains(config.getParameterValue(p))) {
    				//assigned_or_nodes.put(p, preceedingNode(n));
    				old_assigned_and_nodes.put(p, n);
    			}
    		}
		}
		
		List<ParameterConfiguration> nbh = new LinkedList<ParameterConfiguration>();
		for (Parameter p: config.getParameter_instances().keySet()) {
		    if (fixedParameters.containsKey(p)) continue;
			for (Object v: p.getDomain().getDiscreteValues()) {
				if (old_assigned_and_nodes.get(p) == null) continue; // this parameter wasn't actually set
				if (old_assigned_and_nodes.get(p).getDomain().contains(v)) { // same AND node
					if (valuesEqual(v, config.getParameterValue(p))) continue; // same value as current -> skip
					ParameterConfiguration neighbour = new ParameterConfiguration(config);
					neighbour.setParameterValue(p, v);
					neighbour.updateChecksum();
					nbh.add(neighbour);
				} else { // different AND node
					ParameterConfiguration neighbour = new ParameterConfiguration(config);
					neighbour.setParameterValue(p, v);
					
					// find new AND node of this value
					AndNode new_and_node = null;
					for (OrNode or_node: getOrNodes()) {
						if (!or_node.getParameter().equals(p)) continue;
						for (Node n: adjacentNodes(or_node)) {
							AndNode and_node = (AndNode)n;
							if (and_node.getDomain().contains(v)) {
								new_and_node = and_node;
								break;
							}
						}
						if (new_and_node != null) break;
					}

					Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
					// copy over old assigned and nodes except the old one of the current new one
					for (AndNode n: old_assigned_and_nodes.values()) {
						if (n != old_assigned_and_nodes.get(p)) {
							assigned_and_nodes.add(n);
						}
					}
					assigned_and_nodes.add(new_and_node);
					
					// find now unsatisfied OR-nodes and reset their parameter values to null
					AndNode old_and_node = old_assigned_and_nodes.get(p);
					Set<Node> closure = new HashSet<Node>();
					Queue<Node> Q = new LinkedList<Node>();
					closure.add(old_and_node);
					Q.add(old_and_node);
					while (!Q.isEmpty()) {
						Node n = Q.remove();
						if (n instanceof OrNode) {
							if (incomingEdgesDone(n, assigned_and_nodes) == false) {
								// this is a now unsatisified OR node, remove its AND-node
								// from the assigned_and_nodes set
								neighbour.unsetParameter(n.getParameter());
								for (Node an: adjacentNodes(n))	assigned_and_nodes.remove(an);
							}
						}
						for (Node an: adjacentNodes(n)) {
							if (closure.contains(an)) continue; // already visited
							closure.add(an);
							Q.add(an);
						}

					}
					
					// now build the partial configuration starting at the new AND node
					// instead of making random decisions, always choose the 'first' option
					// this constrains the neighbourhood a little, but makes this deterministic
					Set<AndNode> done_and = new HashSet<AndNode>();
					done_and.add(this.startNode);
					done_and.addAll(assigned_and_nodes);
					Set<OrNode> done_or = new HashSet<OrNode>();
					for (AndNode n: assigned_and_nodes) {
						done_or.add(preceedingNode(n));
					}
					
					Set<OrNode> L = new HashSet<OrNode>();
					for (Node n: adjacentNodes(new_and_node)) {
						if (n instanceof OrNode) L.add((OrNode)n);
					}
					
					while (true) {
						Set<OrNode> openOrNodes = new HashSet<OrNode>();
						for (OrNode n: L) 
							if (incomingEdgesDone(n, done_and))
								openOrNodes.add(n);
						if (openOrNodes.isEmpty()) break;
						OrNode or_node = openOrNodes.iterator().next(); // randomElement(openOrNodes, rng);
						L.remove(or_node);
						done_or.add(or_node);
						
						Set<AndNode> adjacentAndNodes = new HashSet<AndNode>();
						for (Node n: adjacentNodes(or_node)) {
							if (n instanceof AndNode) adjacentAndNodes.add((AndNode)n);
						}
						AndNode and_node = adjacentAndNodes.iterator().next(); // randomElement(adjacentAndNodes, rng);
						
						if (neighbour.getParameterValue(and_node.getParameter()) == null) {
							neighbour.setParameterValue(and_node.getParameter(), and_node.getDomain().getDiscreteValues().get(0)); // simply first value for now 
						}
						done_and.add(and_node);
						
						for (Node n: adjacentNodes(and_node)) {
							if (n instanceof OrNode) L.add((OrNode)n);
						}
					}

					neighbour.updateChecksum();
					nbh.add(neighbour);
				}
			}
		}

		return nbh;
	}
	
    /**
     * Returns all parameter configurations that are considered neighbours of the given configuration.
     * In contrast to the other neighbourhood method, real, integer and optionally ordinal parameters
     * are sampled <code>numSamples</code> times according to a gaussian distribution around their
     * current value. The standard deviation of the gaussian is the size of the domain multiplied by 
     * <code>stdDevFactor</code>.
     * @param config The configuration of which the neighbourhood should be generated.
     * @param rng
     * @param stdDevFactor
     * @param numSamples how many samples should be taken for real, integer and ordinal parameters
     * @param gaussianOrdinal whether to sample ordinal domains according to a gaussian distribution or not
     * @return list of all neighbouring configurations
     */
    public List<ParameterConfiguration> getGaussianNeighbourhood(ParameterConfiguration config,
            Random rng, float stdDevFactor, int numSamples, boolean gaussianOrdinal) {
        //Map<Parameter, OrNode> assigned_or_nodes = new HashMap<Parameter, OrNode>();
        Map<Parameter, AndNode> old_assigned_and_nodes = new HashMap<Parameter, AndNode>();
        for (Parameter p: config.getParameter_instances().keySet()) {
            for (AndNode n: getAndNodes()) {
                if (n == startNode) continue;
                if (n.getParameter().equals(p) && n.getDomain().contains(config.getParameterValue(p))) {
                    //assigned_or_nodes.put(p, preceedingNode(n));
                    old_assigned_and_nodes.put(p, n);
                }
            }
        }
        
        List<ParameterConfiguration> nbh = new LinkedList<ParameterConfiguration>();
        for (Parameter p: config.getParameter_instances().keySet()) {
            if (fixedParameters.containsKey(p)) continue;
            List<Object> domain_vals;
            if (!gaussianOrdinal && (p.getDomain() instanceof OrdinalDomain)) {
                domain_vals = p.getDomain().getDiscreteValues();
            }
            else {
                domain_vals = p.getDomain().getGaussianDiscreteValues(rng, config.getParameterValue(p), stdDevFactor, numSamples);
            }
             
            for (Object v: domain_vals) {
                if (old_assigned_and_nodes.get(p) == null) continue; // this parameter wasn't actually set
                if (old_assigned_and_nodes.get(p).getDomain().contains(v)) { // same AND node
                    if (valuesEqual(v, config.getParameterValue(p))) continue; // same value as current -> skip
                    ParameterConfiguration neighbour = new ParameterConfiguration(config);
                    neighbour.setParameterValue(p, v);
                    neighbour.updateChecksum();
                    nbh.add(neighbour);
                } else { // different AND node
                    ParameterConfiguration neighbour = new ParameterConfiguration(config);
                    neighbour.setParameterValue(p, v);
                    
                    // find new AND node of this value
                    AndNode new_and_node = null;
                    for (OrNode or_node: getOrNodes()) {
                        if (!or_node.getParameter().equals(p)) continue;
                        for (Node n: adjacentNodes(or_node)) {
                            AndNode and_node = (AndNode)n;
                            if (and_node.getDomain().contains(v)) {
                                new_and_node = and_node;
                                break;
                            }
                        }
                        if (new_and_node != null) break;
                    }

                    Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
                    // copy over old assigned and nodes except the old one of the current new one
                    for (AndNode n: old_assigned_and_nodes.values()) {
                        if (n != old_assigned_and_nodes.get(p)) {
                            assigned_and_nodes.add(n);
                        }
                    }
                    assigned_and_nodes.add(new_and_node);
                    
                    // find now unsatisfied OR-nodes and reset their parameter values to null
                    AndNode old_and_node = old_assigned_and_nodes.get(p);
                    Set<Node> closure = new HashSet<Node>();
                    Queue<Node> Q = new LinkedList<Node>();
                    closure.add(old_and_node);
                    Q.add(old_and_node);
                    while (!Q.isEmpty()) {
                        Node n = Q.remove();
                        if (n instanceof OrNode) {
                            if (incomingEdgesDone(n, assigned_and_nodes) == false) {
                                // this is a now unsatisified OR node, remove its AND-node
                                // from the assigned_and_nodes set
                                neighbour.unsetParameter(n.getParameter());
                                for (Node an: adjacentNodes(n)) assigned_and_nodes.remove(an);
                            }
                        }
                        for (Node an: adjacentNodes(n)) {
                            if (closure.contains(an)) continue; // already visited
                            closure.add(an);
                            Q.add(an);
                        }

                    }
                    
                    // now build the partial configuration starting at the new AND node
                    // instead of making random decisions, always choose the 'first' option
                    // this constrains the neighbourhood a little, but makes this deterministic
                    Set<AndNode> done_and = new HashSet<AndNode>();
                    done_and.add(this.startNode);
                    done_and.addAll(assigned_and_nodes);
                    Set<OrNode> done_or = new HashSet<OrNode>();
                    for (AndNode n: assigned_and_nodes) {
                        done_or.add(preceedingNode(n));
                    }
                    
                    Set<OrNode> L = new HashSet<OrNode>();
                    for (Node n: adjacentNodes(new_and_node)) {
                        if (n instanceof OrNode) L.add((OrNode)n);
                    }
                    
                    while (true) {
                        Set<OrNode> openOrNodes = new HashSet<OrNode>();
                        for (OrNode n: L) 
                            if (incomingEdgesDone(n, done_and))
                                openOrNodes.add(n);
                        if (openOrNodes.isEmpty()) break;
                        OrNode or_node = openOrNodes.iterator().next(); // randomElement(openOrNodes, rng);
                        L.remove(or_node);
                        done_or.add(or_node);
                        
                        Set<AndNode> adjacentAndNodes = new HashSet<AndNode>();
                        for (Node n: adjacentNodes(or_node)) {
                            if (n instanceof AndNode) adjacentAndNodes.add((AndNode)n);
                        }
                        AndNode and_node = adjacentAndNodes.iterator().next(); // randomElement(adjacentAndNodes, rng);
                        
                        if (neighbour.getParameterValue(and_node.getParameter()) == null) {
                            neighbour.setParameterValue(and_node.getParameter(), and_node.getDomain().getDiscreteValues().get(0)); // simply first value for now 
                        }
                        done_and.add(and_node);
                        
                        for (Node n: adjacentNodes(and_node)) {
                            if (n instanceof OrNode) L.add((OrNode)n);
                        }
                    }

                    neighbour.updateChecksum();
                    nbh.add(neighbour);
                }
            }
        }

        return nbh;
    }
	
	/**
	 * Generates a random neighbour
	 * TODO: use generalized algorithm instead of the "same-AND-node" constrained neighbourhood. 
	 * @param config The configuration of which a random neighbour should be returned
	 * @param rng Random number generator instance
	 * @return random neighbour of the passed configuration
	 */
	public ParameterConfiguration getRandomNeighbour(ParameterConfiguration config, Random rng) {
		Set<OrNode> assigned_or_nodes = new HashSet<OrNode>();
		Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
		Set<Parameter> params = new HashSet<Parameter>(config.getParameter_instances().keySet());
		params.removeAll(fixedParameters.keySet());
		for (Parameter p: params) {
    		for (AndNode n: getAndNodes()) {
    			if (n == startNode) continue;
    			if (n.getParameter().equals(p) && n.getDomain().contains(config.getParameterValue(p))) {
    				assigned_or_nodes.add(preceedingNode(n));
    				assigned_and_nodes.add(n);
    			}
    		}
		}
		
		AndNode node = randomElement(assigned_and_nodes, rng);
		List<Object> vals = node.getDomain().getDiscreteValues();
		ParameterConfiguration n = new ParameterConfiguration(config);
		
		int tried = 0;
		int num_vals = vals.size();
		while (tried++ < num_vals) {
			Object val = vals.get(rng.nextInt(vals.size()));
			vals.remove(val);
			if (valuesEqual(val, config.getParameterValue(node.getParameter()))) continue;
			n.setParameterValue(node.getParameter(), val);
			break;
		}
		n.updateChecksum();
		return n;
	}
	
    /**
     * Generates a random neighbour where real, integer and ordinal parameters are sampled according
     * to a gaussian distribution around their old value (see getGaussianNeighbourhood)
     * TODO: use generalized algorithm instead of the "same-AND-node" constrained neighbourhood. 
     * @param config The configuration of which a random neighbour should be returned
     * @param rng Random number generator instance
     * @return random neighbour of the passed configuration
     */
    public ParameterConfiguration getGaussianRandomNeighbour(ParameterConfiguration config, Random rng,
            float stdDevFactor, int numSamples, boolean gaussianOrdinal) {
        Set<OrNode> assigned_or_nodes = new HashSet<OrNode>();
        Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
        Set<Parameter> params = new HashSet<Parameter>(config.getParameter_instances().keySet());
        params.removeAll(fixedParameters.keySet());
        for (Parameter p: params) {
            for (AndNode n: getAndNodes()) {
                if (n == startNode) continue;
                if (n.getParameter().equals(p) && n.getDomain().contains(config.getParameterValue(p))) {
                    assigned_or_nodes.add(preceedingNode(n));
                    assigned_and_nodes.add(n);
                }
            }
        }
        
        AndNode node = randomElement(assigned_and_nodes, rng);
        List<Object> vals;
        if (!gaussianOrdinal && node.getDomain() instanceof OrdinalDomain) {
            vals = node.getDomain().getDiscreteValues();
        } else {
            vals = node.getDomain().getGaussianDiscreteValues(rng, config.getParameterValue(node.getParameter()), stdDevFactor, numSamples);
        }
        ParameterConfiguration n = new ParameterConfiguration(config);
        
        int tried = 0;
        int num_vals = vals.size();
        while (tried++ < num_vals) {
            Object val = vals.get(rng.nextInt(vals.size()));
            vals.remove(val);
            if (valuesEqual(val, config.getParameterValue(node.getParameter()))) continue;
            n.setParameterValue(node.getParameter(), val);
            break;
        }
        n.updateChecksum();
        return n;
    }
	
	/**
	 * mutate a configuration in each parameter according to a gaussian distribution around the
	 * current parameter value, if the domain allows it.
	 * TODO: generalize ...
	 * @param rng
	 * @param config
	 * @param stdDevFactor standard deviation of the gaussian distribution will be <size of domain> * stdDevFactor
	 * @param mutationProb the probability of mutation each parameter
	 */
	public void mutateParameterConfiguration(Random rng, ParameterConfiguration config, float stdDevFactor, float mutationProb) {
		Set<AndNode> assigned_and_nodes = new HashSet<AndNode>();
		for (Node n: this.nodes) {
			if (n == startNode || !(n instanceof AndNode)) continue;
			if (config.getParameter_instances().containsKey(((AndNode)n).parameter) &&
				((AndNode)n).getDomain().contains(config.getParameterValue(((AndNode)n).parameter))) {
				assigned_and_nodes.add((AndNode)n);
			}
		}
		
		for (AndNode n: assigned_and_nodes) {
		    if (rng.nextFloat() < mutationProb) {
		        config.setParameterValue(n.getParameter(), n.getDomain().mutatedValue(rng, config.getParameterValue(n.getParameter()), stdDevFactor));
		    }
		}
		config.updateChecksum();
	}
	
    public void mutateParameterConfiguration(Random rng, ParameterConfiguration config) {
        mutateParameterConfiguration(rng, config, 0.1f, 0.05f);
    }
	
	/**
	 * Random uniform crossover. The parameter graph is split into connected
	 * components by removing the start AND node. Of each group, either all parameter values of
	 * the first configuration or all parameter values of the second configuration are copied to
	 * the resulting configuration.
	 * @param config1
	 * @param config2
	 * @param rng
	 * @return
	 */
	public Pair<ParameterConfiguration, ParameterConfiguration> crossover(ParameterConfiguration config1, ParameterConfiguration config2, Random rng) {
		Map<Node, Integer> colors = new HashMap<Node, Integer>();
		int currentColor = 1;
		for (Node node: adjacentNodes(startNode)) {
			int c = colorDFS(node, currentColor, colors);
			if (c == currentColor) currentColor++;
		}

		ParameterConfiguration c1 = new ParameterConfiguration(parameters);
		ParameterConfiguration c2 = new ParameterConfiguration(parameters);
		
		for (int col = 1; col < currentColor; col++) {
			if (rng.nextFloat() < 0.5) {
				for (Node n: colors.keySet()) {
					if (colors.get(n) == col) {
						c1.setParameterValue(n.getParameter(), config2.getParameterValue(n.getParameter()));
						c2.setParameterValue(n.getParameter(), config1.getParameterValue(n.getParameter()));
					}
				}
			} else {
				for (Node n: colors.keySet()) {
					if (colors.get(n) == col) {
						c1.setParameterValue(n.getParameter(), config1.getParameterValue(n.getParameter()));
                        c2.setParameterValue(n.getParameter(), config2.getParameterValue(n.getParameter()));
					}
				}
			}
		}
		
		c1.updateChecksum();
		c2.updateChecksum();
		return new Pair<ParameterConfiguration, ParameterConfiguration>(c1, c2);
	}
	
	/**
	 * Two point crossover operator
	 * @param config1
	 * @param config2
	 * @param rng
	 * @return
	 */
	public Pair<ParameterConfiguration, ParameterConfiguration> crossover2Point(ParameterConfiguration config1, ParameterConfiguration config2, Random rng) {
        Map<Node, Integer> colors = new HashMap<Node, Integer>();
        int currentColor = 1;
        for (Node node: adjacentNodes(startNode)) {
            int c = colorDFS(node, currentColor, colors);
            if (c == currentColor) currentColor++;
        }

        ParameterConfiguration c1 = new ParameterConfiguration(parameters);
        ParameterConfiguration c2 = new ParameterConfiguration(parameters);
        int crossoverFirstColor = rng.nextInt(currentColor / 2) + 1;
        int crossoverSecondColor = rng.nextInt(currentColor / 2 + 1) + 1 + currentColor / 2;
        
        for (int col = 1; col < currentColor; col++) {
            for (Node n: colors.keySet()) {
                if (colors.get(n) == col) {
                    if (col <= crossoverFirstColor) { // first section (unchanged)
                        c1.setParameterValue(n.getParameter(), config1.getParameterValue(n.getParameter()));
                        c2.setParameterValue(n.getParameter(), config2.getParameterValue(n.getParameter()));
                    } else if (col <= crossoverSecondColor) { // second section (swap)
                        c1.setParameterValue(n.getParameter(), config2.getParameterValue(n.getParameter()));
                        c2.setParameterValue(n.getParameter(), config1.getParameterValue(n.getParameter()));
                    } else { // third section (unchanged)
                        c1.setParameterValue(n.getParameter(), config1.getParameterValue(n.getParameter()));
                        c2.setParameterValue(n.getParameter(), config2.getParameterValue(n.getParameter()));
                    }
                }
            }
        }
        
        c1.updateChecksum();
        c2.updateChecksum();
	    return new Pair<ParameterConfiguration, ParameterConfiguration>(c1, c2);
	}
	
	public boolean validateParameterConfiguration(ParameterConfiguration config) {
		// TODO: implement
		return false;
	}
}
