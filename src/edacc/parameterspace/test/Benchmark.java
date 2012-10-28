package edacc.parameterspace.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edacc.api.API;
import edacc.api.APIImpl;
import edacc.parameterspace.Parameter;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.CategoricalDomain;
import edacc.parameterspace.domain.FlagDomain;
import edacc.parameterspace.domain.IntegerDomain;
import edacc.parameterspace.domain.OrdinalDomain;
import edacc.parameterspace.domain.RealDomain;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.util.Pair;

public class Benchmark {
    public static void main(String... args) throws Exception {
	    new Benchmark().benchmark();
	}
	
	public void benchmark() throws Exception {
	    API api = new APIImpl();
	    Random rng = new edacc.util.MersenneTwister(123456789);
	    ParameterGraph graph = api
	            .loadParameterGraphFromFile("src/edacc/parameterspace/test/cplex.graph");
	    ParameterGraph.calculateChecksums = false;
	    System.out.println("Benchmarking parameter graphs...");

	    long start = System.currentTimeMillis();
	    for (int i = 0; i < 100000; i++) {
	        ParameterConfiguration config = graph.getRandomConfigurationFast(rng);
	    }
	    System.out.println("getRandomConfiguration: "
	            + (System.currentTimeMillis() - start) / 100000.0f + " ms");


	    List<Parameter> cParams = new LinkedList<Parameter>(graph.getParameterSet());
	    cacheMap(cParams);
	    List<ParameterConfiguration> randomConfigs = new LinkedList<ParameterConfiguration>();
	    for (int i = 0; i < 10000; i++) randomConfigs.add(graph.getRandomConfiguration(rng));
	    start = System.currentTimeMillis();
	    for (ParameterConfiguration config: randomConfigs) {
	        double[] t = paramConfigToTuple(config, cParams);
	    }
	    System.out.println("paramConfigToTuple: "
	            + (System.currentTimeMillis() - start) / (double)randomConfigs.size() + " ms");
	    
	    ParameterConfiguration randomConfig = graph.getRandomConfiguration(rng);
	    start = System.currentTimeMillis();
	    for (int i = 0; i < 2000; i++) {
	        graph.getGaussianNeighbourhood(randomConfig, rng, 0.2f, 1, true);
	    }
	    System.out.println("getGaussianNeighbourhood: "
	            + (System.currentTimeMillis() - start) / 2000.0f + " ms");
	    
        start = System.currentTimeMillis();
        for (int i = 0; i < 2000; i++) {
            graph.getGaussianNeighbourhoodFast(randomConfig, rng, 0.2f, 1, true);
        }
        System.out.println("getGaussianNeighbourhoodFast: "
                + (System.currentTimeMillis() - start) / 2000.0f + " ms");

	    ParameterConfiguration config = graph.getRandomConfiguration(rng);
	    start = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
	        List<ParameterConfiguration> nbh = graph.getNeighbourhood(config);
	    }
	    System.out.println("getNeighbourhood: "
	            + (System.currentTimeMillis() - start) / 100.0f + " ms");

	    ParameterConfiguration p1 = graph.getRandomConfiguration(rng);
	    ParameterConfiguration p2 = graph.getRandomConfiguration(rng);
	    start = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
	        Pair<ParameterConfiguration, ParameterConfiguration> cross = graph.crossover(p1, p2, rng);
	    }
	    System.out.println("crossover: "
	            + (System.currentTimeMillis() - start) / 100.0f + " ms");

	    ParameterConfiguration pp1 = graph.getRandomConfiguration(rng);
	    ParameterConfiguration pp2 = graph.getRandomConfiguration(rng);
	    start = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
	        Pair<ParameterConfiguration, ParameterConfiguration> cross = graph.crossover2Point(pp1, pp2, rng);
	    }
	    System.out.println("crossover2Point: "
	            + (System.currentTimeMillis() - start) / 100.0f + " ms");

	    start = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
	        ParameterConfiguration rnbr = graph.getRandomNeighbour(config, rng);
	    }
	    System.out.println("getRandomNeighbour: "
	            + (System.currentTimeMillis() - start) / 100.0f + " ms");

	    start = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
	        graph.mutateParameterConfiguration(rng, config);
	    }
	    System.out.println("mutateParameterConfiguration: "
	            + (System.currentTimeMillis() - start) / 100.0f + " ms");
	        
	}
	
	private Map<Parameter, Map<String, Integer>> catDomainMap = new HashMap<Parameter, Map<String, Integer>>();
	
	private void cacheMap( List<Parameter> configurableParameters) {
	    for (Parameter p: configurableParameters) {
	        if (p.getDomain() instanceof CategoricalDomain) {
                Map<String, Integer> valueMap = new HashMap<String, Integer>();
                int intVal = 1;
                List<String> sortedValues = new LinkedList<String>(((CategoricalDomain)p.getDomain()).getCategories());
                Collections.sort(sortedValues);
                for (String val: sortedValues) {
                    valueMap.put(val, intVal++);
                }
                catDomainMap.put(p, valueMap);
	        }
	    }
	}
	
    private double[] paramConfigToTuple(ParameterConfiguration paramConfig, List<Parameter> configurableParameters) {
        double[] theta = new double[configurableParameters.size()];
        for (Parameter p: configurableParameters) {
            int pIx = configurableParameters.indexOf(p);
            Object paramValue = paramConfig.getParameterValue(p);
            if (paramValue == null) theta[pIx] = Double.NaN;
            else {
                if (p.getDomain() instanceof RealDomain) {
                    if (paramValue instanceof Float) {
                        theta[pIx] = (Float)paramValue;
                    } else if (paramValue instanceof Double) {
                        theta[pIx] = (Double)paramValue;
                    }
                } else if (p.getDomain() instanceof IntegerDomain) {
                    if (paramValue instanceof Integer) {
                        theta[pIx] = (Integer)paramValue;
                    } else if (paramValue instanceof Long) {
                        theta[pIx] = (Long)paramValue;
                    }
                } else if (p.getDomain() instanceof CategoricalDomain) {
                    theta[pIx] = catDomainMap.get(p).get((String)paramValue);
                } else if (p.getDomain() instanceof OrdinalDomain) {
                    // map ordinal parameters to integers 1 through domain.size, 0 = not set
                    Map<String, Integer> valueMap = new HashMap<String, Integer>();
                    int intVal = 1;
                    for (String val: ((OrdinalDomain)p.getDomain()).getOrdered_list()) {
                        valueMap.put(val, intVal++);
                    }
                    
                    theta[pIx] = valueMap.get((String)paramValue);
                } else if (p.getDomain() instanceof FlagDomain) {
                    // map flag parameters to {0, 1}
                    if (FlagDomain.FLAGS.ON.equals(paramValue)) theta[pIx] = 2;
                    else theta[pIx] = 1;
                } else {
                    // TODO
                    theta[pIx] = paramValue.hashCode();
                }
            }
            
        }
        
        return theta;
    }
}
