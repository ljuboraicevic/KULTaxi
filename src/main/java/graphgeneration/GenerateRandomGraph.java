package graphgeneration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 
 * @author Adam Torzs Generates a planar, simple, undirected graph and
 *         saves it to a .dot file
 * 
 *         Currently it generates a grid
 *         
 */
public class GenerateRandomGraph {

	//private final int NODE_NUM = 10;
	private final int GRID_SIZE = 10;
	//private final double EDGE_DENSITY = 0.3;

	private final int MAP_SIZE = 3000;

	//private final Random random = new Random();

	/**
	 * 
	 * Returns a new node with a random position within the given boundaries
	 * 
	 */
	/*public Node randomNode(int num) {
		// Generate random position
		double x = random.nextDouble() * MAP_SIZE;
		double y = random.nextDouble() * MAP_SIZE;

		return new Node(x, y, num);

	}*/

	/**
	 * 
	 * Generates the required number of nodes
	 * 
	 */
	/*
	 * public List<Node> generateNodes() { ArrayList<Node> nodes = new
	 * ArrayList<Node>(); for (int i = 0; i < NODE_NUM; i++) {
	 * nodes.add(randomNode(i)); } return nodes; }
	 */
	public List<Node> generateNodes() {
		List<Node> nodes = new ArrayList<>();

		for (int i = 0; i < GRID_SIZE; i++) {
			for (int j = 0; j < GRID_SIZE; j++) {
				double x = (double)i / GRID_SIZE * MAP_SIZE;
				double y = (double)j / GRID_SIZE * MAP_SIZE;
				nodes.add(new Node(x, y, i * GRID_SIZE + j));
			}
		}
		return nodes;
	}

	/**
	 * 
	 * Generates the edges for the given nodes
	 * 
	 */
	/*
	 * private List<Edge> generateEdges(List<Node> nodes) { List<Edge> edges =
	 * new ArrayList<Edge>(); int nodeNum = nodes.size(); for(int i =
	 * 0;i<nodeNum;i++) { for(int j = i;j<nodeNum;j++) { if(i==j) continue;
	 * 
	 * if(random.nextDouble()<EDGE_DENSITY) { edges.add(new
	 * Edge(nodes.get(i),nodes.get(j))); edges.add(new
	 * Edge(nodes.get(j),nodes.get(i))); } } } return edges; }
	 */

	private List<Edge> generateEdges(List<Node> nodes) {
		List<Edge> edges = new ArrayList<>();
		for (int i = 0; i < GRID_SIZE; i++) {
			for (int j = 0; j < GRID_SIZE; j++) {
				Node n = nodes.get(i * GRID_SIZE + j);
				if (i != GRID_SIZE - 1) {
					Node n2 = nodes.get((i + 1) * GRID_SIZE + j);
					edges.add(new Edge(n, n2));
					edges.add(new Edge(n2, n));

				}
				if (j != GRID_SIZE - 1) {
					Node n2 = nodes.get(i * GRID_SIZE + j + 1);
					edges.add(new Edge(n, n2));
					edges.add(new Edge(n2, n));
				}
			}
		}
		return edges;
	}

	/*
	 * private List<Edge> generateEdges(List<Node> nodes) {
	 * 
	 * List<Edge> edges = new ArrayList<Edge>(); int n = nodes.size();
	 * 
	 * for (int i = 0; i < n; i++) { for (int j = i + 1; j < n; j++) { for (int
	 * k = j + 1; k < n; k++) { boolean isTriangle = true; for (int a = 0; a <
	 * n; a++) { if (a == i || a == j || a == k) continue; if
	 * (nodes.get(a).insideTriangle(nodes.get(i), nodes.get(j), nodes.get(k))) {
	 * isTriangle = false; break; } } if (isTriangle) { Edge e, f, g; e = new
	 * Edge(nodes.get(i), nodes.get(j)); f = new Edge(nodes.get(i),
	 * nodes.get(k)); g = new Edge(nodes.get(j), nodes.get(k)); if
	 * (!edges.contains(e)) { edges.add(e); } if (!edges.contains(f)) {
	 * edges.add(f); } if (!edges.contains(g)) { edges.add(g); } } } } } return
	 * edges; }
	 */

	public void generate(String filename) throws FileNotFoundException {
		List<Node> nodes = generateNodes();
		List<Edge> edges = generateEdges(nodes);

		String start = "digraph mapgraph {";
		String nodeTemplate = "n%d[p=\"%.1f,%.1f\"]";
		String nodeAposTemplate = "n%d[p='%.1f,%.1f']";
		String edgeTemplate = "n%d -> n%d[d=\"%.1f\"]";
		String edgeAposTemplate = "n%d -> n%d[d='%.1f']";
		String end = "}";

		PrintWriter pw = new PrintWriter("maps\\" + filename + ".dot");
		PrintWriter pwapos = new PrintWriter("maps\\" + filename + ".dotapos");

		pw.println(start);
		pwapos.println(start);
		for (Node n : nodes) {
			String node = String.format(Locale.ENGLISH, nodeTemplate, n.num, n.x, n.y);
			pw.println(node);
			String nodeApos = String.format(Locale.ENGLISH, nodeAposTemplate, n.num, n.x, n.y);
			pwapos.println(nodeApos);
		}

		for (Edge e : edges) {
			String edge = String.format(Locale.ENGLISH, edgeTemplate, e.e1.num, e.e2.num, 300.0f);
			pw.println(edge);
			String edgeApos = String.format(Locale.ENGLISH, edgeAposTemplate, e.e1.num, e.e2.num, 300.0f);
			pwapos.println(edgeApos);
		}
		pw.println(end);
		pwapos.println(end);
		pw.close();
		pwapos.close();
	}

	public static void main(String[] args) {
		GenerateRandomGraph grg = new GenerateRandomGraph();

		try {
			grg.generate("test");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
