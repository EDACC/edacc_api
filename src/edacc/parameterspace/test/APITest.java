package edacc.parameterspace.test;

import java.util.LinkedList;
import java.util.List;

import edacc.api.API;
import edacc.api.API.COST_FUNCTIONS;
import edacc.api.APIImpl;
import edacc.api.CostFunction;
import edacc.model.ExperimentResult;

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
