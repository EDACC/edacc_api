package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Before;
import org.junit.Test;

import edacc.api.API;
import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.CategoricalDomain;
import edacc.parameterspace.domain.RealDomain;
import edacc.parameterspace.graph.AndNode;
import edacc.parameterspace.graph.Edge;
import edacc.parameterspace.graph.Node;
import edacc.parameterspace.graph.OrNode;
import edacc.parameterspace.graph.ParameterGraph;



public class ParameterGraphTest {
	private ParameterGraph g = null;
	
	@Before
	public void setUp() throws Exception {
		Set<Parameter> parameters = new HashSet<Parameter>();
		String[] c = {"atom", "body", "hybrid", "no"};
		Parameter param_lookahead = new Parameter("lookahead", new CategoricalDomain(c));
		parameters.add(param_lookahead);
		
		Set<Node> nodes = new HashSet<Node>();
		AndNode start = new AndNode(null, null); nodes.add(start);
		OrNode lookahead = new OrNode(param_lookahead, param_lookahead.getDomain()); nodes.add(lookahead);
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
	}

	@Test
	public void testGetNeighbourhood() {
		Random rng = new Random();
		API api = new API();
		try {
			ParameterGraph pspace = api.loadParameterGraphFromFile("src/sparrow_parameterspace.xml");
			ParameterConfiguration config = pspace.getRandomConfiguration(rng);
			pspace.getNeighbourhood(config);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
