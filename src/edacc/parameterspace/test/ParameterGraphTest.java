package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import edacc.api.API;
import edacc.api.APIImpl;
import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.CategoricalDomain;
import edacc.parameterspace.domain.FlagDomain;
import edacc.parameterspace.graph.AndNode;
import edacc.parameterspace.graph.Edge;
import edacc.parameterspace.graph.Node;
import edacc.parameterspace.graph.OrNode;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.util.MersenneTwister;

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
		OrNode lookahead = new OrNode(param_lookahead); nodes.add(lookahead);
		AndNode lookahead_vals = new AndNode(param_lookahead, param_lookahead.getDomain()); nodes.add(lookahead_vals);
		
		List<Edge> edges = new LinkedList<Edge>();
		edges.add(new Edge(start, lookahead, 0));
		edges.add(new Edge(lookahead, lookahead_vals, 0));
		
		this.g = new ParameterGraph(nodes, edges, parameters, start);
	}

	@Test
	public void testGetRandomConfiguration() {
		Random rng = new MersenneTwister();
		ParameterConfiguration p = g.getRandomConfiguration(rng);
		assertTrue(p != null);
	}

	@Test
	public void testGetConstrainedNeighbourhood() throws Exception {
		API api = new APIImpl();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/sparrow_parameterspace.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("ps", 0.2);
		config.setParameterValue("c1", 10);
		config.setParameterValue("c2", 20);
		config.setParameterValue("c3", 20);
		
		List<ParameterConfiguration> nbh = pspace.getConstrainedNeighbourhood(config);
		
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
	public void testGetFullNeighbourhood() throws Exception {
	    API api = new APIImpl();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/complex.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("c1", 5);
		config.setParameterValue("ps", 0.1);
		config.setParameterValue("flag", FlagDomain.FLAGS.ON);
		config.setParameterValue("method", "hybrid");
		config.setParameterValue("cat", "1");
		assertTrue("c1: 5 cat: 1 method: hybrid flag: ON ps: 0.1 prob: null ".equals(config.toString()));
		
		List<ParameterConfiguration> nbh = pspace.getNeighbourhood(config);
		
		ParameterConfiguration nb1 = new ParameterConfiguration(config);		
		nb1.setParameterValue("method", "atom");
		assertTrue(nbh.contains(nb1));
		
		ParameterConfiguration nb2 = new ParameterConfiguration(config);
		nb2.setParameterValue("flag", FlagDomain.FLAGS.OFF);
		assertFalse(nbh.contains(nb2)); // flag -> off should lead to method and cat being removed
		nb2.unsetParameter("cat");
		nb2.unsetParameter("method");
		assertTrue(nbh.contains(nb2));
		
		ParameterConfiguration nb3 = new ParameterConfiguration(config);
		nb3.setParameterValue("c1", 6);
		assertTrue(nbh.contains(nb3));
		nb3.setParameterValue("c1", 5); // same config
		assertFalse(nbh.contains(nb3));
		
	}
	
	@Test
	public void testGetRandomNeighbour() throws Exception {
		Random rng = new MersenneTwister();
		API api = new APIImpl();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/sparrow_parameterspace.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("ps", 0.2);
		config.setParameterValue("c1", 1);
		config.setParameterValue("c2", 2);
		config.setParameterValue("c3", 15);
		
		List<ParameterConfiguration> nbh = pspace.getConstrainedNeighbourhood(config);
		for (int i = 0; i < 100; i++) assertTrue(nbh.contains(pspace.getRandomNeighbour(config, rng)));
	}
	
	@Test
	public void testCrossover() throws Exception {
		Random rng = new MersenneTwister();
		API api = new APIImpl();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/sparrow_parameterspace.xml");
		ParameterConfiguration config1 = new ParameterConfiguration(pspace.getParameterSet());
		config1.setParameterValue("ps", 0.1);
		config1.setParameterValue("c1", 1);
		config1.setParameterValue("c2", 1);
		config1.setParameterValue("c3", 11);
		
		ParameterConfiguration config2 = new ParameterConfiguration(pspace.getParameterSet());
		config2.setParameterValue("ps", 0.2);
		config2.setParameterValue("c1", 2);
		config2.setParameterValue("c2", 2);
		config2.setParameterValue("c3", 22);
		
		ParameterConfiguration cross = pspace.crossover(config1, config2, rng);
		
		for (Parameter p: pspace.parameters) {
			Object p_val = cross.getParameterValue(p);
			assertTrue(config1.getParameterValue(p).equals(p_val) || config2.getParameterValue(p).equals(p_val));
		}
	}

}
