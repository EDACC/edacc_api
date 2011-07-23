package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.OrdinalDomain;
import edacc.util.MersenneTwister;

public class OrdinalDomainTest {

	@Test
	public void test() {
		Random rng = new MersenneTwister();
		List<String> l = new LinkedList<String>();
		l.add("1");
		l.add("2");
		l.add("3");
		l.add("4.5");
		l.add("5");
		OrdinalDomain o = new OrdinalDomain(l);
		
		assertTrue(o.contains("1"));
		assertTrue(o.contains("3"));
		assertTrue(o.contains("4.5"));
		assertFalse(o.contains(3));
		
		for (int i = 0; i < 1000; i++) {
			assertTrue(o.contains(o.randomValue(rng)));
			assertTrue(o.getDiscreteValues().contains(o.randomValue(rng)));
			assertTrue(o.contains(o.mutatedValue(rng, o.randomValue(rng))));
		}
		
		assertTrue(o.getDiscreteValues().size() == 5);
	}

}
