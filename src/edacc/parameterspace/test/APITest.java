package edacc.parameterspace.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edacc.api.APIImpl;
import edacc.model.ConfigurationScenarioDAO;
import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.IntegerDomain;
import edacc.parameterspace.graph.ParameterGraph;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
	    Random rng = new Random();
		APIImpl api = new APIImpl();
		
		
		api.connect("edacc3", 3306, "simon", "simon", "x");
		
		for (int i = 0; i < 1; i++) {
		    System.out.println(api.loadParameterGraphFromDB(136).getRandomConfiguration(rng));
		    
		    
		}
		
		ParameterGraph pspace = api.loadParameterGraphFromDB(136);
		
		ParameterConfiguration cfg = pspace.getRandomConfiguration(rng);
		        
		cfg.setParameterValue("function", "1");
		cfg.setParameterValue("epsilon", 0.232);
		
		System.out.println(pspace.validateParameterConfiguration(cfg));
		

		/*List<Parameter> configurableParameters = api.getConfigurableParameters(402);
        Object[] cpRF = api.loadParameterGraphFromDB(402).conditionalParentsForRF(configurableParameters);
        int[][] condParents = (int[][])cpRF[0];
        int[][][] condParentVals = (int[][][])cpRF[1];

		for (int i = 0; i < configurableParameters.size(); i++) {
		    System.out.println("Cond parents of " + configurableParameters.get(i).getName());
		    if (condParents[i] != null) {
		        for (int j = 0; j < condParents[i].length; j++) {
		            System.out.println(condParents[i][j]);
		        }
		    }
		}*/
		
		
	      
        //IntegerDomain d = new IntegerDomain(0, 1);
        //System.out.println(Arrays.toString(d.getGaussianDiscreteValues(rng, 0, 0.2f, 2).toArray()));	
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
