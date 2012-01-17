package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.List;
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
		assertTrue(r.contains(0.5d));
	}

	@Test
	public void testUniformDistributedValues() {
		RealDomain r = new RealDomain(0.54421, 1.35667);
		List<Object> vals = r.getUniformDistributedValues(42);
		assertEquals(vals.size(), 42);
		for (int i = 0; i < vals.size(); i++) {
			assertTrue(vals.get(i) instanceof Double);
		}
		Double d1 = (Double) vals.get(0);
		Double d2 = (Double) vals.get(1);
		Double dist = d2 - d1;
		for (int i = 2; i < vals.size(); i++) {
			d1 = d2;
			d2 = (Double) vals.get(i);
			Double test = d2 - d1 - dist;
			assertTrue((test > -0.000000000001 && test < 0.000000000001));			
		}
		assertEquals(vals.get(0), r.getLow());
		assertEquals(vals.get(vals.size()-1), r.getHigh());
	}
}
