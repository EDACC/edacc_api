package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.OptionalDomain;
import edacc.util.MersenneTwister;

public class OptionalDomainTest {

	@Test
	public void test() {
		Random rng = new MersenneTwister();
		OptionalDomain o = new OptionalDomain();
		
		assertTrue(o.contains(OptionalDomain.OPTIONS.NOT_SPECIFIED));
		assertTrue(o.getDiscreteValues().contains(OptionalDomain.OPTIONS.NOT_SPECIFIED));
		
		for (int i = 0; i < 1000; i++) {
			assertTrue(o.contains(o.randomValue(rng)));
			assertTrue(o.contains(o.mutatedValue(rng, o.randomValue(rng))));
		}
		
		assertTrue(o.getDiscreteValues().size() == 1);
	}

}
