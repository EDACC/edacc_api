package parameterspace.domain;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CategoricalDomain extends Domain {
	private Set<String> categories;
	
	public CategoricalDomain(String[] categories) {
		this.categories = new HashSet<String>();
		for (String s: categories) {
			this.categories.add(new String(s));
		}
	}
	
	public CategoricalDomain(Set<String> categories) {
		this.categories = categories;
	}
	
	@Override
	public boolean contains(Object value) {
		return categories.contains(value);
	}

	@Override
	public Object randomValue(Random rng) {
		if (categories.size() == 0) return null;
		return categories.toArray()[rng.nextInt(categories.size())];
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (String c: categories) {
			sb.append(c);
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

}
