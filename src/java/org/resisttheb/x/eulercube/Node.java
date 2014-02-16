package org.resisttheb.x.eulercube;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public interface Node {
	public static class NodeObject implements Node {
		protected final CubePoint point;
		protected final List<Edge> edges;
		
		public NodeObject(final CubePoint _point, final Edge ... _edges) {
			this.point = _point;
			this.edges = new ArrayList<Edge>(4);
			for (Edge _edge : _edges) {
				edges.add(_edge);
			}
		}
		
		
		public void addEdge(final Edge edge) {
			edges.add(edge);
		}
		
		
		/**************************
		 * NODE IMPLEMENTATION
		 **************************/
		
		public CubePoint getPoint() {
			return point;
		}
		
		public int getDegree() {
			return edges.size();
		}
		
		public Edge[] getEdges() {
			return (Edge[]) edges.toArray(new Edge[edges.size()]);
		}
		
		public Node[] getEndpointNodes() {
			final Set<Node> nodeSet = new HashSet<Node>(edges.size());
			for (Edge edge : edges) {
				boolean selfHit = false;
				for (Node node : edge.getEndpointNodes()) {
					if (this == node) {selfHit = true; continue;}
					nodeSet.add(node);
				}
				assert selfHit : "Self is not included in edge.";
			}
			return (Node[]) nodeSet.toArray(new Node[nodeSet.size()]);
		}
		
		/**************************
		 * END NODE IMPLEMENTATION
		 **************************/
		
		/**************************
		 * OBJECT OVERRIDES
		 **************************/
		
		@Override
		public String toString() {
			return String.format("%s", getPoint());
		}
		
		/**************************
		 * END OBJECT OVERRIDES
		 **************************/
	}
	
	
	public CubePoint getPoint();
	
	public int getDegree();
	public Edge[] getEdges();
	public Node[] getEndpointNodes();
}
