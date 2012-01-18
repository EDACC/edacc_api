package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.RealDomain;
import edacc.util.MersenneTwister;


public class RealDomainTest {
	private static double eps = 0.000000000001;
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
		assertTrue(r.contains(d1));
		Double dist = d2 - d1;
		for (int i = 2; i < vals.size(); i++) {
			d1 = d2;
			d2 = (Double) vals.get(i);
			assertTrue(r.contains(d2));
			Double test = d2 - d1 - dist;
			assertTrue((test > -eps && test < eps));			
		}
		assertEquals(vals.get(0), r.getLow());
		assertEquals(vals.get(vals.size()-1), r.getHigh());
	}
	
	@Test
	public void testMidValue() {
		RealDomain r = new RealDomain(-10.0, 10.0);
		Double d0 = null;
		Random rng = null;
		for (int state = 0; state < 2; state++) {
			if (state == 0)
				rng = new Random(42);
			else if (state == 1)
				rng = new Random();
			for (int i = 0; i < 10000; i++) {
				double d = rng.nextDouble() * 20 - 10;
				assertTrue(r.contains(d));
				if (d0 != null) {
					Object o = r.getMidValueOrNull(d0, d);
					Object o2 = r.getMidValueOrNull(d, d0);
					if (o == null || o2 == null) {
						assertTrue(o == o2);
						assertTrue(d0.equals(d));
					} else {
						assertTrue(o instanceof Double);
						assertTrue(o2 instanceof Double);
						Double d1 = (Double) o;
						Double d2 = (Double) o2;
						double test = d1 - d2;
						assertTrue((test > -eps && test < eps));
						assertTrue(r.contains(d1));
						assertTrue(r.contains(d2));
						test = (d1 + d2) / 2. - d1;
						assertTrue((test > -eps && test < eps));
						test = (d1 + d2) / 2. - d2;
						assertTrue((test > -eps && test < eps));
					}
				}
				d0 = d;
			}
		}
	}
}
