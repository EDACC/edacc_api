package edacc.parameterspace.graph;

import edacc.parameterspace.Parameter;

public class OrNode extends Node {
	
	public OrNode(Parameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return "OrNode [" + parameter + "]";
	}
}
