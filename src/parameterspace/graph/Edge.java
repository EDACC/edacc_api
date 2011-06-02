package parameterspace.graph;

public class Edge {
	private Node source;
	private Node target;
	private int group;
	
	public Edge(Node source, Node target, int group) {
		this.source = source;
		this.target = target;
		this.group = group;
	}

	public Node getSource() {
		return source;
	}

	public void setSource(Node source) {
		this.source = source;
	}

	public Node getTarget() {
		return target;
	}

	public void setTarget(Node target) {
		this.target = target;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}
}
