package edacc.parameterspace.test;

import static org.junit.Assert.*;

import org.junit.Test;

import edacc.api.APIImpl;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.domain.FlagDomain;
import edacc.parameterspace.graph.ParameterGraph;

public class ParameterConfigurationTest {

	@Test
	public void testEqualsObject() throws Exception {
		APIImpl api = new APIImpl();
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/edacc/parameterspace/test/complex.xml");
		ParameterConfiguration config = new ParameterConfiguration(pspace.getParameterSet());
		config.setParameterValue("c1", 5);
		config.setParameterValue("ps", 0.1);
		config.setParameterValue("flag", FlagDomain.FLAGS.OFF);
		assertTrue("c1: 5 flag: OFF ps: 0.1 ".equals(config.toString()));
		
		ParameterConfiguration config2 = new ParameterConfiguration(pspace.getParameterSet());
		config2.setParameterValue("c1", 5);
		config2.setParameterValue("ps", 0.1);
		config2.unsetParameter("flag"); // set flag to null, should be equal to a configuration with flag = FlagDomain.FLAGS.OFF
		
		assertTrue(config.equals(config2));
	}

}
