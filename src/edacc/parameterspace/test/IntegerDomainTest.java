package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.IntegerDomain;

public class IntegerDomainTest {

	@Test
	public void test() {
		Random rng = new Random();
		IntegerDomain d = new IntegerDomain(10, 1000);
		
		assertTrue(d.contains(10));
		assertTrue(d.contains(1000));
		assertFalse(d.contains(9));
		assertFalse(d.contains(9.9999));
		assertFalse(d.contains(1001));
		assertFalse(d.contains(10.5));
		assertTrue(d.getHigh() == 1000);
		assertTrue(d.getLow() == 10);
		
		for (int i = 0; i < 1000; i++) {
			assertTrue(d.contains(d.randomValue(rng)));
			assertTrue(d.contains(d.mutatedValue(rng, d.randomValue(rng))));
			assertTrue(d.getDiscreteValues().contains(d.randomValue(rng)));
		}
		
		assertTrue(d.getDiscreteValues().size() == 991);
	}

}
