package parameterspace.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import parameterspace.Parameter;
import parameterspace.ParameterConfiguration;
import parameterspace.domain.CategoricalDomain;
import parameterspace.domain.RealDomain;
import parameterspace.graph.AndNode;
import parameterspace.graph.Edge;
import parameterspace.graph.OrNode;
import parameterspace.graph.Node;
import parameterspace.graph.ParameterGraph;

public class ParameterGraphTest {
	private ParameterGraph g = null;
	
	@Before
	public void setUp() throws Exception {
		Set<Parameter> parameters = new HashSet<Parameter>();
		String[] c = {"atom", "body", "hybrid", "no"};
		Parameter param_lookahead = new Parameter("lookahead", "--lookahead=", 0, new CategoricalDomain(c), false, false);
		parameters.add(param_lookahead);
		
		Set<Node> nodes = new HashSet<Node>();
		AndNode start = new AndNode(null, null); nodes.add(start);
		OrNode lookahead = new OrNode(param_lookahead); nodes.add(lookahead);
		AndNode lookahead_vals = new AndNode(param_lookahead, param_lookahead.getDomain()); nodes.add(lookahead_vals);
		
		List<Edge> edges = new LinkedList<Edge>();
		edges.add(new Edge(start, lookahead, 0));
		edges.add(new Edge(lookahead, lookahead_vals, 0));
		
		this.g = new ParameterGraph(nodes, edges, parameters, start);
	}

	@Test
	public void testGetRandomConfiguration() {
		Random rng = new Random();
		ParameterConfiguration p = g.getRandomConfiguration(rng);
		assertTrue(p != null);
		System.out.println(p);
	}

}
