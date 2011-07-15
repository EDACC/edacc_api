package edacc.parameterspace.test;

import edacc.api.API;
import edacc.parameterspace.ParameterConfiguration;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
		API api = new API();
		api.connect("localhost", 3306, "EDACC", "edacc", "edaccteam");
		ParameterConfiguration config = api.getParameterConfiguration(15, 1013);
		System.out.println(config + " " + api.toHex(config.getChecksum()));
		long start = System.currentTimeMillis();
		System.out.println("config " + api.exists(15, config));
		System.out.println(System.currentTimeMillis() - start);
		api.disconnect();
	}
}
