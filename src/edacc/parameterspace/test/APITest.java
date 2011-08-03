package edacc.parameterspace.test;

import edacc.api.API;
import edacc.api.APIImpl;

/**
 * random tests
 */
public class APITest {
	public static void main(String ... args) throws Exception {
		API api = new APIImpl();
		api.connect("localhost", 3306, "EDACC", "edacc", "edaccteam");
		api.disconnect();
	}
}
