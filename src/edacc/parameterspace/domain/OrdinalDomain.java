package edacc.parameterspace.domain;

import java.util.List;
import java.util.Random;

public class OrdinalDomain extends Domain {
	protected List<String> ordered_list;
	
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
}
