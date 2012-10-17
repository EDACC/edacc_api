package edacc.parameterspace;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edacc.parameterspace.domain.FlagDomain;
import edacc.parameterspace.domain.OptionalDomain;
import edacc.parameterspace.domain.RealDomain;

public class ParameterConfiguration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8241847198586343570L;
	private Map<Parameter, Object> parameter_instances;
	private byte[] checksum;
	private int checksumHashCode;
	
	public ParameterConfiguration(Set<Parameter> parameters) {
		this.checksum = null;
		this.parameter_instances = new HashMap<Parameter, Object>();
		for (Parameter p: parameters) parameter_instances.put(p, null);
	}
	
	public byte[] getChecksum() {
		return checksum;
	}
	
	/**
	 * Updates the checksum of the parameter configuration in canonical representation, i.e.
	 * parameters are sorted by their name and the string representation of their
	 * values are fed to a hash function one after another. 
	 */
	public void updateChecksum() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			List<Parameter> sortedKeys = new ArrayList<Parameter>(parameter_instances.keySet());
			Collections.sort(sortedKeys);
			for (Parameter p: sortedKeys) {
				if (parameter_instances.get(p) != null && 
					!(parameter_instances.get(p) instanceof OptionalDomain.OPTIONS) &&
					!(parameter_instances.get(p).equals(FlagDomain.FLAGS.OFF))) {
					md.update(getValueRepresentation(parameter_instances.get(p)).getBytes());
				}
			}
			this.checksum = md.digest();
			this.checksumHashCode = java.util.Arrays.hashCode(checksum);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Object getParameterValue(Parameter p) {
		if (!parameter_instances.containsKey(p))
			throw new IllegalArgumentException("The parameter has to be part of a solver configuration");
		return parameter_instances.get(p);
	}
	
	public void setParameterValue(Parameter p, Object v) {
		if (!parameter_instances.containsKey(p))
			throw new IllegalArgumentException("The parameter has to be part of a solver configuration");
		if (v == null) {
		    parameter_instances.put(p, null);
		    return;
		}
		
		if (!p.getDomain().contains(v)) {
			throw new IllegalArgumentException("Domain of parameter " + p.getName() + " does not contain the given value " + v + " Domain: " + p.getDomain());
		}
        if (p.getDomain() instanceof RealDomain) {
            if (v instanceof Integer) v = ((Integer)v).floatValue();
        }
		parameter_instances.put(p, v);
	}
	
	   public void setParameterValueFast(Parameter p, Object v) {
	        parameter_instances.put(p, v);
	    }
	
	public void setParameterValue(String parameter_name, Object v) {
		Parameter param = null;
		for (Parameter p: parameter_instances.keySet()) {
			if (p.getName().equals(parameter_name)) param = p;
		}
		if (param == null) return;
		if (v == null) {
		    parameter_instances.put(param, null);
		    return;
		}
		
		if (!param.getDomain().contains(v)) {
			throw new IllegalArgumentException("Domain of parameter " + param.getName() + " does not contain the given value " + v + " Domain: " + param.getDomain());
		}
		if (param.getDomain() instanceof RealDomain) {
		    if (v instanceof Integer) v = ((Integer)v).floatValue();
		}
		parameter_instances.put(param, v);
	}
	
	public void unsetParameter(Parameter p) {
		if (!parameter_instances.containsKey(p)) return;
		parameter_instances.put(p, null);
	}
	
	public void unsetParameter(String parameter_name) {
		Parameter param = null;
		for (Parameter p: parameter_instances.keySet()) {
			if (p.getName().equals(parameter_name)) param = p;
		}
		if (param == null) return;
		
		parameter_instances.put(param, null);
	}
	
	@Override
	public int hashCode() {
		return checksumHashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterConfiguration other = (ParameterConfiguration) obj;
		if (parameter_instances == null) {
			if (other.parameter_instances != null)
				return false;
		} else if (!parameter_instances.keySet().equals(other.parameter_instances.keySet())) // set comparison
			return false;
		else {
			for (Parameter p: parameter_instances.keySet()) {
				if (parameter_instances.get(p) == null && other.getParameterValue(p) == null) continue;
				
				if (parameter_instances.get(p) == null && other.getParameterValue(p).equals(FlagDomain.FLAGS.OFF)) continue;
				if (parameter_instances.get(p) == null && other.getParameterValue(p).equals(OptionalDomain.OPTIONS.NOT_SPECIFIED)) continue;
				
				if (other.getParameterValue(p) == null && parameter_instances.get(p).equals(FlagDomain.FLAGS.OFF)) continue;
				if (other.getParameterValue(p) == null && parameter_instances.get(p).equals(OptionalDomain.OPTIONS.NOT_SPECIFIED)) continue;
				
				if (parameter_instances.get(p) == null) return false;
				
				if (parameter_instances.get(p) instanceof Double || parameter_instances.get(p) instanceof Float) {
				    
					double this_val = ((Number)parameter_instances.get(p)).doubleValue();
					double other_val = ((Number)other.getParameterValue(p)).doubleValue();
					if (Math.abs(other_val - this_val) > 1e-10) {
					    return false;
					}
				}
				else if (!parameter_instances.get(p).equals(other.getParameterValue(p))) {
				    return false;
				}
			}
		}
		return true;
	}

	public Map<Parameter, Object> getParameter_instances() {
		return parameter_instances;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<Parameter> params = new ArrayList<Parameter>();
		params.addAll(parameter_instances.keySet());
		for (Parameter p: params) {
		    if (parameter_instances.get(p)== null) continue;
			sb.append(p.getName());
			sb.append(": ");
			sb.append(getValueRepresentation(parameter_instances.get(p)));
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public ParameterConfiguration(ParameterConfiguration other) {
		// TODO: ensure that other.getParameterValue(p) makes a copy in all cases
		this.parameter_instances = new HashMap<Parameter, Object>();
		if (other.checksum == null) this.checksum = null;
		else this.checksum = other.checksum.clone();
		for (Parameter p: other.parameter_instances.keySet()) {
			parameter_instances.put(p, other.getParameterValue(p));
		}
	}
	
	public String getValueRepresentation(Object value) {
	    // return the textual representation of a parameter value
	    if (value instanceof Float || value instanceof Double) {
	        String r = String.valueOf(value);
	        if (!r.contains(".")) r += ".0";
	        return r;
	    }
	    else return value.toString();
	}
}
