package edacc.parameterspace;

import edacc.parameterspace.domain.Domain;

public class Parameter {
	private String name;
	private Domain domain;
	
	public Parameter(String name, Domain domain) {
		this.name = name;
		this.domain = domain;
	}
	
	public String getName() {
		return name;
	}

	public Domain getDomain() {
		return domain;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parameter other = (Parameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
