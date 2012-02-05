package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.IntegerDomain;
import edacc.util.MersenneTwister;

public class IntegerDomainTest {

	@Test
	public void test() {
		Random rng = new MersenneTwister();
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

	@Test
	public void testUniformDistributedValues() {
		IntegerDomain d = new IntegerDomain(32,3536);
		List<Object> l = d.getUniformDistributedValues(1234);
		assertTrue(l.size() == 1234);
		assertTrue(l.contains(32));
		assertTrue(l.contains(3536));
		Integer dist = (Integer) l.get(1) - (Integer) l.get(0);
		for (int i = 1; i < l.size(); i++) {
			assertTrue(l.get(i).getClass() == Integer.class);
			Integer tmp_dist = (Integer) l.get(i) - (Integer) l.get(i-1);
			Integer tmp = tmp_dist - dist;
			assertTrue(tmp <= 1 && tmp >= -1);
		}
	}
	
	@Test
	public void testMidValue() {
		IntegerDomain d1 = new IntegerDomain(0,1);
		IntegerDomain d2 = new IntegerDomain(0,10);
		IntegerDomain d3 = new IntegerDomain(1,10);
		
		assertNull(d1.getMidValueOrNull(new Integer(0), new Integer(1)));
		assertTrue(new Integer(5).equals(d2.getMidValueOrNull(new Integer(0), new Integer(10))));
		assertNull(d2.getMidValueOrNull(new Integer(5), new Integer(5)));
		assertTrue(new Integer(3).equals(d3.getMidValueOrNull(new Integer(2), new Integer(4))));
	}
	
	@Test
	public void testGetGaussianDiscreteValues() {
	    Random rng = new MersenneTwister();
        IntegerDomain d1 = new IntegerDomain(0,1);
        
        assertTrue(d1.getGaussianDiscreteValues(rng, 0.5f, 0.1f, 7).size() == 2);
	}
}
