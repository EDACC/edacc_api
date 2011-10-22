package edacc.parameterspace.test;

import java.util.List;
import java.util.Random;

import edacc.api.API;
import edacc.api.APIImpl;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.util.Pair;

public class Benchmark {
	@SuppressWarnings("unused")
    public static void main(String... args) throws Exception {
		API api = new APIImpl();
		Random rng = new edacc.util.MersenneTwister(123456789);
		ParameterGraph graph = api
				.loadParameterGraphFromFile("src/edacc/parameterspace/test/jack.xml");
		System.out.println("Benchmarking parameter graphs...");
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			ParameterConfiguration config = graph.getRandomConfiguration(rng);
		}
		System.out.println("getRandomConfiguration: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");

		ParameterConfiguration config = graph.getRandomConfiguration(rng);
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			List<ParameterConfiguration> nbh = graph.getNeighbourhood(config);
		}
		System.out.println("getNeighbourhood: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");

		ParameterConfiguration p1 = graph.getRandomConfiguration(rng);
		ParameterConfiguration p2 = graph.getRandomConfiguration(rng);
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Pair<ParameterConfiguration, ParameterConfiguration> cross = graph.crossover(p1, p2, rng);
		}
		System.out.println("crossover: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");
		
		ParameterConfiguration pp1 = graph.getRandomConfiguration(rng);
		ParameterConfiguration pp2 = graph.getRandomConfiguration(rng);
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Pair<ParameterConfiguration, ParameterConfiguration> cross = graph.crossover2Point(pp1, pp2, rng);
		}
		System.out.println("crossover2Point: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");
		
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			ParameterConfiguration rnbr = graph.getRandomNeighbour(config, rng);
		}
		System.out.println("getRandomNeighbour: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");
		
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			graph.mutateParameterConfiguration(rng, config);
		}
		System.out.println("mutateParameterConfiguration: "
				+ (System.currentTimeMillis() - start) / 1000.0f + " ms");
	}
}
