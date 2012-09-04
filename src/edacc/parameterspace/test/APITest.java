package edacc.parameterspace.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edacc.api.APIImpl;
import edacc.parameterspace.ParameterConfiguration;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
	    Random rng = new Random();
		APIImpl api = new APIImpl();
		api.connect("localhost", 3306, "configurator", "edacc", "edaccteam");
		List<ParameterConfiguration> cfs = new LinkedList<ParameterConfiguration>();
		for (int i = 1; i < 1000; i++) 
		    cfs.add(api.loadParameterGraphFromDB(172).getRandomConfiguration(rng));
		int n = 0;
		long start = System.currentTimeMillis();
		for (ParameterConfiguration p: cfs) {
		    api.createSolverConfig(172, p, String.valueOf(n++));
		}
        System.out.println("createSolverConfig: "
                + (System.currentTimeMillis() - start) / (float)cfs.size() + " ms");
		api.disconnect();
	}
}
