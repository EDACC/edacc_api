package edacc.parameterspace.test;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edacc.parameterspace.domain.CategoricalDomain;
import edacc.parameterspace.domain.Domain;
import edacc.parameterspace.domain.IntegerDomain;
import edacc.parameterspace.domain.MixedDomain;

public class MixedDomainTest {

	@Test
	public void test() {
		Random rng = new Random();
		List<Domain> l = new LinkedList<Domain>();
		l.add(new CategoricalDomain(new String[] {"yes", "no", "1", "2"}));
		l.add(new IntegerDomain(0, 100));
		MixedDomain m = new MixedDomain(l);
		
		assertTrue(m.contains("1"));
		assertTrue(m.contains("yes"));
		assertTrue(m.contains(1));
		assertFalse(m.contains("abcd"));
		assertFalse(m.contains(101));
		assertTrue(m.contains(50));
		
		assertTrue(m.getDiscreteValues().contains(10));
		assertTrue(m.getDiscreteValues().contains(1));
		assertTrue(m.getDiscreteValues().contains("yes"));
		assertTrue(m.getDiscreteValues().contains("1"));
		
		for (int i = 0; i < 1000; i++) {
			assertTrue(m.contains(m.randomValue(rng)));
			assertTrue(m.contains(m.mutatedValue(rng, m.randomValue(rng))));
		}

	}

}
