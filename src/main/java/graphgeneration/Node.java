package graphgeneration;

public class Node {

	/**
	 * Internal representation of a node.
	 *
	 */

	int num;
	
	double x, y;

	public Node(double x, double y,int num) {
		this.x = x;
		this.y = y;
		this.num = num;
	}

	/**
	 * 
	 * Helper method for insideTriangle
	 * 
	 */
	public double sign(Node p1, Node p2, Node p3) {
		return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
	}

	/**
	 * 
	 * Method for determining if a point is inside a triangle
	 * 
	 */
	public boolean insideTriangle(Node p1, Node p2, Node p3) {
		boolean b1, b2, b3;

		b1 = sign(this, p1, p2) < 0.0f;
		b2 = sign(this, p2, p3) < 0.0f;
		b3 = sign(this, p3, p1) < 0.0f;

		return ((b1 == b2) && (b2 == b3));

	}
	
	@Override
	public boolean equals(Object obj) {
		Node n = (Node)obj;
		return n.num == this.num;
	}
	
}
