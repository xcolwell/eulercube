package org.resisttheb.x.eulercube;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EulerUtilities {
	private EulerUtilities() {
	}
	
	
	/**************************
	 * UTILITIES
	 **************************/
	
	public static Node opposite(final Node node, final Edge edge) {
		for (Node enode : edge.getEndpointNodes()) {
			if (node != enode) return enode;
		}
		return null;
	}
	
	/**************************
	 * END UTILITIES
	 **************************/

	
	public static boolean isEuler(final Cube cube) {
		final List<Node> nodes = new ArrayList<Node>(32);
		cube.getNodes(nodes);
		for (Node node : nodes) {
			final int d = node.getDegree();
			if (0 != d % 2) {
				return false;
			}
		}
		return true;
	}
	
	
	public static void edgeCircuit(final Node start, final Cube cube, final EdgeRanker ranker, final List<Edge>path) {
		final Set<Edge> edges = new HashSet<Edge>(cube.edgeCount());
		cube.getEdges(edges);
		final Map<CubePointTuple, Edge> edgeMap = new HashMap<CubePointTuple, Edge>(edges.size());
		for (Edge edge : edges) {
			edgeMap.put(new CubePointTuple(edge.getEndpoints()), edge);
		}
		
		final List<Node> nodePath = new ArrayList<Node>(cube.nodeCount());
		circuit(start, new HashSet<Edge>(cube.edgeCount()), ranker, nodePath);
		
		final Iterator<Node> itr = nodePath.iterator();
		Node pnode = itr.next();
		for (Node node; itr.hasNext(); pnode = node) {
			node = itr.next();
			
			final Edge edge = edgeMap.get(new CubePointTuple(pnode.getPoint(), node.getPoint()));
			assert null != edge;
			if (null != edge) {
				path.add(edge);
			}
		}
	}
	
	public static void circuit(
			final Node start, 
			final Set<Edge> explored, 
			final EdgeRanker ranker, 
			final List<Node> path
	) {
		final List<Frame> stack = new LinkedList<Frame>();
		
		stack.add(Frame.create(start, ranker));
		
		while (! stack.isEmpty()) {
			final Frame frame = stack.get(0);
			for (; stack.get(0) == frame && frame.i < frame.edges.length; frame.i++) {
				final Edge edge = frame.edges[frame.i];
				if (explored.contains(edge)) continue;
				explored.add(edge);
				stack.add(0, Frame.create(opposite(frame.node, edge), ranker));
			}
			if (stack.get(0) == frame) {
				path.add(0, frame.node);
				stack.remove(0);
			}
		}
	}
	
	private static final class Frame {
		public static final Frame create(final Node node, final EdgeRanker ranker) {
			final Edge[] edges = node.getEdges();
			ranker.sort(node, edges);
			return new Frame(node, edges);
		}
		
		
		public int i = 0;
		public final Edge[] edges;
		public final Node node;
		
		public Frame(final Node _node, final Edge[] _edges) {
			this.node = _node;
			this.edges = _edges;
		}
	}
}
