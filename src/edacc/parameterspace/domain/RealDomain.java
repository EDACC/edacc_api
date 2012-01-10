package edacc.parameterspace.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class RealDomain extends Domain {
	protected Double low, high;
	public static final String name = "Real";
	
	@SuppressWarnings("unused")
    private RealDomain() {
		
	}
	
	public RealDomain(Double low, Double high) {
		this.low = low;
		this.high = high;
	}
	
	public RealDomain(Integer low, Integer high) {
		this.low = Double.valueOf(low);
		this.high = Double.valueOf(high);
	}
	
	@Override
	public boolean contains(Object value) {
		if (!(value instanceof Number)) return false;
		double d = ((Number)value).doubleValue();
		return d >= this.low && d <= this.high;
	}
	
	@Override
	public Object randomValue(Random rng) {
		return rng.nextDouble() * (this.high - this.low) + this.low; 
	}

	@Override
	public String toString() {
		return "[" + this.low + "," + this.high + "]";
	}

	public Double getLow() {
		return low;
	}

	public void setLow(Double low) {
		this.low = low;
	}

	public Double getHigh() {
		return high;
	}

	public void setHigh(Double high) {
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
        return Math.min(Math.max(this.low, ((Number)value).doubleValue() + r), this.high);
    }
	
	@Override
	public List<Object> getDiscreteValues() {
		List<Object> values = new LinkedList<Object>();
		for (double d = low; d <= high; d += (high - low) / 100.0f) {
			values.add(d);
		}
		return values;
	}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Object> getGaussianDiscreteValues(Random rng, Object value,
            float stdDevFactor, int numberSamples) {
        List<Object> vals = new LinkedList<Object>();
        for (int i = 0; i < numberSamples; i++) {
            vals.add(mutatedValue(rng, value, stdDevFactor));
        }
        return vals;
    }

	@Override
	public List<Object> getUniformDistributedValues(int numberSamples) {
		List<Object> vals = new LinkedList<Object>();
		double dist = (high - low) / (double) numberSamples;
		double cur = low;
		for (int i = 0; i < numberSamples; i++) {
			vals.add(cur);
			cur += dist;
		}
		return vals;
	}
}
