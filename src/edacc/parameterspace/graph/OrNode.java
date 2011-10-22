package edacc.parameterspace.graph;

import edacc.parameterspace.Parameter;
import java.io.Serializable;

@SuppressWarnings("serial")
public class OrNode extends Node implements Serializable {
	
	@SuppressWarnings("unused")
    private OrNode() {
		
	}
	
	public OrNode(Parameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return "OrNode [" + parameter + "]";
	}
}
