package parameterspace.domain;

import java.util.Random;

public class IntegerDomain extends Domain {
	private Integer low, high;
	
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
}
