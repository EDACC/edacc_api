package edacc.parameterspace.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class IntegerDomain extends Domain {
	protected Integer low, high;
	public static final String name = "Integer";
	
	@SuppressWarnings("unused")
    private IntegerDomain() {
		
	}
	
	public IntegerDomain(Integer low, Integer high) {
		this.low = low;
		this.high = high;
	}
	
	@Override
	public boolean contains(Object value) {
		if (!(value instanceof Number)) return false;
		double d = ((Number)value).doubleValue();
		if (d != Math.round(d)) return false;
		return d >= this.low && d <= this.high;
	}
	
	@Override
	public Object randomValue(Random rng) {
		return rng.nextInt(this.high - this.low + 1) + this.low; 
	}

	@Override
	public String toString() {
		return "[" + this.low + "," + this.high + "]";
	}

	public Integer getLow() {
		return low;
	}

	public void setLow(Integer low) {
		this.low = low;
	}

	public Integer getHigh() {
		return high;
	}

	public void setHigh(Integer high) {
		this.high = high;
	}

	@Override
	public Object mutatedValue(Random rng, Object value) {
		return mutatedValue(rng, value, 0.1f);
	}
	
    @Override
    public Object mutatedValue(Random rng, Object value, float stdDevFactor) {
        if (!contains(value)) return value;
        double r = rng.nextGaussian() * ((high - low) * stdDevFactor);
        return (int) Math.min(Math.max(this.low, Math.round(((Number)value).doubleValue() + r)), this.high);
    }

	@Override
	public List<Object> getDiscreteValues() {
		List<Object> values = new LinkedList<Object>();
		for (int i = this.low; i <= this.high; i++) {
			values.add(i);
		}
		return values;
	}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Object> getGaussianDiscreteValues(Random rng, Object value, float stdDevFactor,
            int numberSamples) {
        if (numberSamples == 1) {
            List<Object> singleVals = new LinkedList<Object>();
            singleVals.add(mutatedValue(rng, value, stdDevFactor));
            return singleVals;
        }
        if (numberSamples >= (high - low + 1)) {
            return getDiscreteValues();
        }
        List<Object> vals = new LinkedList<Object>();
        for (int i = 0; i < numberSamples; i++) {
            if (vals.size() == high - low + 1) break; // sampled all possible values
            Object val = null;
            int tries = 0;
            while ((val == null || vals.contains(val)) && tries++ < (high - low  + 1)) {
                val = mutatedValue(rng, value, stdDevFactor);
            }
            vals.add(mutatedValue(rng, value, stdDevFactor));
        }
        return vals;
    }

	@Override
	public List<Object> getUniformDistributedValues(int numberSamples) {
		if (numberSamples >= (high - low + 1)) {
			return getDiscreteValues();
		}
		List<Object> vals = new LinkedList<Object>();
		double dist = (high - low) / (double) (numberSamples-1);
		double cur = low;
		for (int i = 0; i < numberSamples; i++) {
			if (vals.size() == high - low + 1) break;
			vals.add(new Integer((int) Math.round(cur)));
			cur += dist;
		}
		return vals;
	}

	@Override
	public Object getMidValueOrNull(Object o1, Object o2) {
		if (!(o1 instanceof Integer) || !(o2 instanceof Integer)) {
			return null;
		}
		Integer i1 = (Integer)o1;
		Integer i2 = (Integer)o2;
		Integer mid = (i1 + i2) / 2;
		if (i1.equals(mid) || i2.equals(mid)) {
			return null;
		}
		return mid;
	}
}
