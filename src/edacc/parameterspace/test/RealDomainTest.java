package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.RealDomain;
import edacc.util.MersenneTwister;


public class RealDomainTest {

	@Test
	public void testContains() {
		RealDomain r = new RealDomain(0, 100);
		assertTrue(r.contains(new Double(0.1)));
		assertFalse(r.contains(new Double(-4.0)));
		assertTrue(r.contains(new Integer(10)));
	}

	@Test
	public void testRandomValue() {
		RealDomain r = new RealDomain(0, 100);
		Random rng = new MersenneTwister();
		for (int i = 0; i < 1000; i++)
			assertTrue(r.contains(r.randomValue(rng)));
	}

	@Test
	public void testToString() {
		RealDomain r = new RealDomain(0, 100);
		assertEquals(r.toString(), "[0.0,100.0]");
	}

	@Test
	public void testRealDomain() {
		RealDomain r = new RealDomain(0, 1);
		r.contains(0.5d);
	}

}
