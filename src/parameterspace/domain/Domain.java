package parameterspace.domain;

import java.util.Random;

public abstract class Domain {
	public abstract boolean contains(Object value);
	public abstract Object randomValue(Random rng);
	public abstract String toString();
}
