package edacc.parameterspace.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class OrdinalDomain extends Domain {
	protected List<String> ordered_list;
	public static final String name = "Ordinal";
	
	@SuppressWarnings("unused")
    private OrdinalDomain() {
		
	}
	
	public OrdinalDomain(List<String> ordered_list) {
		this.ordered_list = ordered_list;
	}

	@Override
	public boolean contains(Object value) {
		return ordered_list.contains(value);
	}

	@Override
	public Object randomValue(Random rng) {
		int i = rng.nextInt(ordered_list.size());
		return ordered_list.get(i);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String s: ordered_list) {
			sb.append(s);
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public List<String> getOrdered_list() {
		return ordered_list;
	}

	public void setOrdered_list(List<String> ordered_list) {
		this.ordered_list = ordered_list;
	}

	@Override
	public Object mutatedValue(Random rng, Object value) {
	    return mutatedValue(rng, value, 0.1f);
	}
	
    @Override
    public Object mutatedValue(Random rng, Object value, float stdDevFactor) {
        if (!contains(value)) return value;
        int ix = ordered_list.indexOf(value);
        double r = rng.nextGaussian() * (ordered_list.size() * stdDevFactor);
        int n = Math.min((int)Math.max(Math.round(ix + r), ordered_list.size()), 0);
        return ordered_list.get(n);
    }
	
	@Override
	public List<Object> getDiscreteValues() {
		List<Object> values = new LinkedList<Object>();
		values.addAll(ordered_list);
		return values;
	}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Object> getGaussianDiscreteValues(Random rng, Object value,
            float stdDevFactor, int numberSamples) {
        if (numberSamples >= ordered_list.size()) {
            return getDiscreteValues();
        }
        List<Object> vals = new LinkedList<Object>();
        for (int i = 0; i < numberSamples; i++) {
            if (vals.size() == ordered_list.size()) break; // sampled all possible values
            Object val = null;
            while (val == null || vals.contains(val)) {
                val = mutatedValue(rng, value, stdDevFactor);
            }
            vals.add(mutatedValue(rng, value, stdDevFactor));
        }
        return vals;
    }
}
