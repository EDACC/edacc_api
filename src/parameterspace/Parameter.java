package parameterspace;

import parameterspace.domain.Domain;

public class Parameter implements Comparable<Parameter> {
	private String name;
	private String prefix;
	private Integer order;
	private Domain domain;
	private boolean spaceBeforeValue;
	private boolean attachToPrevParameter;
	
	public Parameter(String name, String prefix, Integer order, Domain domain) {
		this.name = name;
		this.prefix = prefix;
		this.order = order;
		this.domain = domain;
		this.spaceBeforeValue = true;
		this.attachToPrevParameter = false;
	}
	
	public Parameter(String name, String prefix, Integer order, Domain domain,
			boolean spaceBeforeValue, boolean attachToPrevParameter) {
		this.name = name;
		this.prefix = prefix;
		this.order = order;
		this.domain = domain;
		this.spaceBeforeValue = spaceBeforeValue;
		this.attachToPrevParameter = attachToPrevParameter;
	}

	public String getName() {
		return name;
	}

	public String getPrefix() {
		return prefix;
	}

	public Integer getOrder() {
		return order;
	}

	public Domain getDomain() {
		return domain;
	}

	public boolean isSpaceBeforeValue() {
		return spaceBeforeValue;
	}

	public boolean isAttachToPrevParameter() {
		return attachToPrevParameter;
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

	@Override
	public String toString() {
		return "Parameter [name=" + name + ", prefix=" + prefix + ", order="
				+ order + ", domain=" + domain + ", spaceBeforeValue="
				+ spaceBeforeValue + ", attachToPrevParameter="
				+ attachToPrevParameter + "]";
	}

	@Override
	public int compareTo(Parameter o) {
		return this.order - o.getOrder();
	}
	
	
}
