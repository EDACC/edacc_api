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
	public void testGetNeighbourhood() throws FileNotFoundException {
		API api = new API();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/sparrow_parameterspace.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("ps", 0.2);
		config.setParameterValue("c1", 10);
		config.setParameterValue("c2", 20);
		config.setParameterValue("c3", 20);
		
		List<ParameterConfiguration> nbh = pspace.getNeighbourhood(config);
		
		ParameterConfiguration neighbour1 = new ParameterConfiguration(config);
		neighbour1.setParameterValue("c1", 11);
		
		ParameterConfiguration neighbour2 = new ParameterConfiguration(config);
		neighbour2.setParameterValue("c1", 50);
		
		ParameterConfiguration neighbour3 = new ParameterConfiguration(config);
		neighbour3.setParameterValue("ps", 0.5);
		
		ParameterConfiguration nonneighbour1 = new ParameterConfiguration(config);
		nonneighbour1.setParameterValue("c1", 11);
		nonneighbour1.setParameterValue("c2", 19);
		
		assertTrue(nbh.contains(neighbour1));
		assertTrue(nbh.contains(neighbour2));
		assertTrue(nbh.contains(neighbour3));
		assertFalse(nbh.contains(nonneighbour1));
	
		assertTrue(nbh.size() == 100 + 51 + 51 + 21 - 4); // 100 ps, 51 c1, 51 c2, 21 c3 values, minus 4 fixed values from config
		assertFalse(nbh.contains(config));
	}
	
	@Test
	public void testGetRandomNeighbour() throws FileNotFoundException {
		Random rng = new Random();
		API api = new API();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/sparrow_parameterspace.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("ps", 0.2);
		config.setParameterValue("c1", 1);
		config.setParameterValue("c2", 2);
		config.setParameterValue("c3", 15);
		
		List<ParameterConfiguration> nbh = pspace.getNeighbourhood(config);
		for (int i = 0; i < 100; i++) assertTrue(nbh.contains(pspace.getRandomNeighbour(config, rng)));
	}

}
