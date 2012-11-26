package edacc.parameterspace.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edacc.api.APIImpl;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.IntegerDomain;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
	    Random rng = new Random();
		APIImpl api = new APIImpl();
		
	      
        IntegerDomain d = new IntegerDomain(0, 1);
        System.out.println(Arrays.toString(d.getGaussianDiscreteValues(rng, 0, 0.2f, 2).toArray()));	
		/*
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
		api.disconnect();*/
	}
}
