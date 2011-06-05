package edacc.parameterspace.domain;

import java.util.Random;

public class RealDomain extends Domain {
	protected Double low, high;
	
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
}
