package graphgeneration;

public class Edge {
	Node e1, e2;

	Edge(Node e1, Node e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	@Override
	public boolean equals(Object obj) {
		Edge e = (Edge) obj;

		return e.e1.equals(e1) && e.e2.equals(e2);
	}

	@Override
	public int hashCode() {

		return e1.num * 1000 + e2.num;
	}

}
