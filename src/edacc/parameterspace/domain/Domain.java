package edacc.parameterspace.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import javax.xml.bind.annotation.XmlSeeAlso;

@SuppressWarnings("serial")
@XmlSeeAlso({ CategoricalDomain.class, FlagDomain.class, IntegerDomain.class,
		MixedDomain.class, OptionalDomain.class, RealDomain.class,
                OrdinalDomain.class})
public abstract class Domain implements Serializable {
        public static final String[] names = {CategoricalDomain.name, FlagDomain.name, IntegerDomain.name, MixedDomain.name, OptionalDomain.name, OrdinalDomain.name, RealDomain.name};
        
	public abstract boolean contains(Object value);

	public abstract Object randomValue(Random rng);

	public abstract String toString();
	
	public abstract Object mutatedValue(Random rng, Object value);
	
	public abstract Object mutatedValue(Random rng, Object value, float stdDevFactor);
	
	public abstract List<Object> getDiscreteValues();
	
	public abstract List<Object> getGaussianDiscreteValues(Random rng, Object value, float stdDevFactor, int numberSamples);
        
	public abstract List<Object> getUniformDistributedValues(int numberSamples);
	
	public abstract Object getMidValueOrNull(Object o1, Object o2);
	
    public abstract String getName();
}
