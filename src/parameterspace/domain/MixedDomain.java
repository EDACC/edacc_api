package parameterspace.domain;

import java.util.List;
import java.util.Random;

public class MixedDomain extends Domain {
	List<Domain> domains;
	
	public MixedDomain(List<Domain> domains) {
		this.domains = domains;
	}

	@Override
	public boolean contains(Object value) {
		for (Domain d: domains) {
			if (d.contains(value)) return true;
		}
		return false;
	}

	@Override
	public Object randomValue(Random rng) {
		int i = rng.nextInt(domains.size());
		return domains.get(i).randomValue(rng);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Domain d: domains) {
			sb.append(d.toString());
			sb.append("+");
		}
		return sb.toString();
	}

}
