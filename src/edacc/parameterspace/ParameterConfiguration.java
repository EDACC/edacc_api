package edacc.parameterspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ParameterConfiguration {
	private Map<Parameter, Object> parameter_instances;
	
	public ParameterConfiguration(Set<Parameter> parameters) {
		this.parameter_instances = new HashMap<Parameter, Object>();
		for (Parameter p: parameters) parameter_instances.put(p, null);
	}
	
	public Object getParameterValue(Parameter p) {
		if (!parameter_instances.containsKey(p))
			throw new IllegalArgumentException("The parameter has to be part of a solver configuration");
		return parameter_instances.get(p);
	}
	
	public void setParameterValue(Parameter p, Object v) {
		if (!parameter_instances.containsKey(p))
			throw new IllegalArgumentException("The parameter has to be part of a solver configuration");
		if (!p.getDomain().contains(v)) {
			throw new IllegalArgumentException("Parameter domain does not contain the given value");
		}
		parameter_instances.put(p, v);
	}
	
	public Map<Parameter, Object> getParameter_instances() {
		return parameter_instances;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<Parameter> params = new ArrayList<Parameter>();
		params.addAll(parameter_instances.keySet());
		for (Parameter p: params) {
			sb.append(p.getName());
			sb.append(": ");
			sb.append(parameter_instances.get(p));
			sb.append(" ");
		}
		return sb.toString();
	}
}
