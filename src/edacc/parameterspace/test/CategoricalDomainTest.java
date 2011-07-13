package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.CategoricalDomain;

public class CategoricalDomainTest {

	@Test
	public void test() {
		Random rng = new Random();
		CategoricalDomain c = new CategoricalDomain(new String[] {"yes", "no", "1", "2"});
		assertTrue(c.contains("yes"));
		assertTrue(c.contains("no"));
		assertTrue(c.contains("1"));
		assertFalse(c.contains(1));
		assertFalse(c.contains("abcd"));
		
		for (int i = 0; i < 1000; i++) {
			assertTrue(c.contains(c.randomValue(rng)));
			assertTrue(c.getDiscreteValues().contains(c.randomValue(rng)));
			assertTrue(c.contains(c.mutatedValue(rng, c.randomValue(rng))));
		}
		
		assertTrue(c.getCategories().containsAll(c.getDiscreteValues()));
		
	}

}
