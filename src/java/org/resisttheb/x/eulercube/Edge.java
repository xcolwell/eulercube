package org.resisttheb.x.eulercube;
import java.util.ArrayList;
import java.util.List;


// note that the interface supports hyperedges
public interface Edge {
	
	public static class EdgeObject implements Edge {
		protected final List<Node> nodes;
		
		
		public EdgeObject(final Node ... _nodes) {
			nodes = new ArrayList<Node>(4);
			for (Node _node : _nodes) {
				nodes.add(_node);
			}
		}
		
		
		public void addNode(final Node node) {
			nodes.add(node);
		}
		
		
		/**************************
		 * EDGE IMPLEMENTATION
		 **************************/
		
		public CubePoint[] getMidpoints() {
			final CubePoint[] points = getEndpoints();
			if (2 < points.length) {
				// TODO: get smarter for hyperedge
				return new CubePoint[]{CubePoint.midpoint(points)};
			}
			else if (2 == points.length) {
				return new CubePoint[]{
					CubePoint.lerp(points[0], points[1], 1.f / 3.f),
					CubePoint.lerp(points[0], points[1], 2.f / 3.f)
				};
			}
			else if (1 == points.length) {
				return new CubePoint[]{points[0]};
			}
			else {
				return new CubePoint[0];
			}
		}
		
		public CubePoint[] getEndpoints() {
			final Node[] nodes = getEndpointNodes();
			final CubePoint[] points = new CubePoint[nodes.length];
			for (int i = 0; i < nodes.length; i++) {
				points[i] = nodes[i].getPoint();
			}
			return points;
		}
		
		public int getDegree() {
			return nodes.size();
		}
		
		public Node[] getEndpointNodes() {
			return (Node[]) nodes.toArray(new Node[nodes.size()]);
		}
		
		/**************************
		 * END EDGE IMPLEMENTATION
		 **************************/
		
		/**************************
		 * OBJECT OVERRIDES
		 **************************/
		
		@Override
		public String toString() {
			return String.format("%s", new CubePointTuple(getEndpoints()));
		}
		
		/**************************
		 * END OBJECT OVERRIDES
		 **************************/
	}
	
	
	public CubePoint[] getMidpoints();
	public CubePoint[] getEndpoints();
	
	public int getDegree();
	public Node[] getEndpointNodes();
}
