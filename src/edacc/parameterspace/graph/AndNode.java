package edacc.parameterspace.graph;

import edacc.parameterspace.Parameter;
import edacc.parameterspace.domain.Domain;

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
