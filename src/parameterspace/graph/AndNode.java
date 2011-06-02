package parameterspace.graph;

import parameterspace.Parameter;
import parameterspace.domain.Domain;

public class AndNode extends Node {
	private Domain domain;
	
	public AndNode(Parameter parameter, Domain domain) {
		this.parameter = parameter;
		this.domain = domain;
	}

	@Override
	public String toString() {
		return "AndNode [" + domain + "]";
	}

	public Domain getDomain() {
		return domain;
	}
}
