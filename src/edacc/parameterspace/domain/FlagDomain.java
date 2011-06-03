package edacc.parameterspace.domain;

import java.util.Random;

public class FlagDomain extends Domain {
	public static enum OPTIONS {
		ON {
			public String toString() {
				return "ON";
			}
		},
		OFF {
			public String toString() {
				return "OFF";
			}
		}
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
		return "{ON, OFF}";
	}

}
