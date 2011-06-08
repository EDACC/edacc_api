package edacc.parameterspace.domain;

import java.util.Random;

public class OptionalDomain extends Domain {
	public static enum OPTIONS {
		NOT_SPECIFIED {
			public String toString() {
				return "NOT_SPECIFIED";
			}
		}
	}
	
	private OptionalDomain() {
		
	}
	
	@Override
	public boolean contains(Object value) {
		if (!(value instanceof OPTIONS)) return false;
		return true;
	}

	@Override
	public Object randomValue(Random rng) {
		int n = rng.nextInt(OPTIONS.values().length);
		return OPTIONS.values()[n];
	}

	@Override
	public String toString() {
		return "{<not specified>}";
	}

	@Override
	public Object mutatedValue(Random rng, Object value) {
		if (!contains(value)) return value;
		return OPTIONS.NOT_SPECIFIED;
	}

}