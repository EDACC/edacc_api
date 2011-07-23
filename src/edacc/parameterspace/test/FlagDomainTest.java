package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.FlagDomain;
import edacc.util.MersenneTwister;

public class FlagDomainTest {

	@Test
	public void test() {
		Random rng = new MersenneTwister();
		FlagDomain full = new FlagDomain(true, true);
		
		assertTrue(full.contains(FlagDomain.FLAGS.ON));
		assertTrue(full.contains(FlagDomain.FLAGS.OFF));
		for (int i = 100; i < 100; i++) {
			assertTrue(full.contains(full.randomValue(rng)));
			assertTrue(full.contains(full.mutatedValue(rng, full.randomValue(rng))));
		}
		
		assertTrue(full.getValues().containsAll(full.getDiscreteValues()));
		
		
		FlagDomain on = new FlagDomain(true, false);
		assertTrue(on.contains(FlagDomain.FLAGS.ON));
		assertFalse(on.contains(FlagDomain.FLAGS.OFF));
		
		FlagDomain off = new FlagDomain(false, true);
		assertFalse(off.contains(FlagDomain.FLAGS.ON));
		assertTrue(off.contains(FlagDomain.FLAGS.OFF));
		
	}

}
