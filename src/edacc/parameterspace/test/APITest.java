package edacc.parameterspace.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edacc.api.APIImpl;
import edacc.model.ConfigurationScenarioDAO;
import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.IntegerDomain;
import edacc.parameterspace.graph.Node;
import edacc.parameterspace.graph.ParameterGraph;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
	    Random rng = new Random();
		APIImpl api = new APIImpl();
		
		api.connect("edacc3.informatik.uni-ulm.de", 3306, "x", "y", "z");
		
		
		ParameterGraph pspace = api.loadParameterGraphFromDB(398);
		/*List<ParameterConfiguration> cfgs = new ArrayList<ParameterConfiguration>();
		for (Integer id: api.getSolverConfigurations(398)) {
			ParameterConfiguration cfg = api.getParameterConfiguration(398, id);
			cfgs.add(cfg);
			if (!pspace.validateParameterConfiguration(cfg)) {
			System.out.println(id + " " + cfg);
			}
		}*/
		
		
		for (int i = 0; i < 10000; i++) {
			ParameterConfiguration c = pspace.getRandomConfiguration(rng);
			c = pspace.getGaussianNeighbourhood(c, rng, 1.0f, 100, true).get(0);
			if (!pspace.validateParameterConfiguration(c)) {
				System.err.println("Generated invalid random config " + c);
				System.err.flush();
			}
			System.out.println(c);
		}

	
	}
}
