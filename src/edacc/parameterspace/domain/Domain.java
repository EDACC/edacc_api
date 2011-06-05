package edacc.parameterspace.domain;

import java.util.Random;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({ CategoricalDomain.class, FlagDomain.class, IntegerDomain.class,
		MixedDomain.class, OptionalDomain.class, RealDomain.class })
public abstract class Domain {
	public abstract boolean contains(Object value);

	public abstract Object randomValue(Random rng);

	public abstract String toString();
}
