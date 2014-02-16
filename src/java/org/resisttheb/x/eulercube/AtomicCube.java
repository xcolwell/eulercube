package org.resisttheb.x.eulercube;
import java.util.Arrays;
import java.util.Collection;


// the atomic cube,
// 
public class AtomicCube extends Cube.AbstractCube {
	private static AtomicCube slice(
			final float x0, final float x1, 
			final float y0, final float y1, 
			final float z0, final float z1
	) {
		return atom(new CubePoint(x0, y0, z0), new CubePoint(x1, y1, z1));
	}
	
	public static AtomicCube atom(final CubePoint point0, final CubePoint point1) {
		final Node[] corners = new Node[8];
		final Edge[] connectors = new Edge[16];
		
		final float x0 = point0.x;
		final float x1 = point1.x;
		final float y0 = point0.y;
		final float y1 = point1.y;
		final float z0 = point0.z;
		final float z1 = point1.z;
		
		corners[0] = node(x0, y0, z0);
		corners[1] = node(x1, y0, z0);
		corners[2] = node(x0, y1, z0);
		corners[3] = node(x1, y1, z0);
		corners[4] = node(x0, y0, z1);
		corners[5] = node(x1, y0, z1);
		corners[6] = node(x0, y1, z1);
		corners[7] = node(x1, y1, z1);
		
		connectors[0] = edge(corners[0], corners[1]);
		connectors[1] = edge(corners[0], corners[2]);
		connectors[2] = edge(corners[0], corners[4]);
		connectors[3] = edge(corners[0], corners[7]);
		
		connectors[4] = edge(corners[1], corners[3]);
		connectors[5] = edge(corners[1], corners[5]);
		connectors[6] = edge(corners[1], corners[6]);
		
		connectors[7] = edge(corners[2], corners[3]);
		connectors[8] = edge(corners[2], corners[6]);
		connectors[9] = edge(corners[2], corners[5]);
		
		connectors[10] = edge(corners[3], corners[7]);
		connectors[11] = edge(corners[3], corners[4]);
		
		connectors[12] = edge(corners[4], corners[5]);
		connectors[13] = edge(corners[4], corners[6]);
		
		connectors[14] = edge(corners[5], corners[7]);
		
		connectors[15] = edge(corners[6], corners[7]);
		
		for (Edge connector : connectors) {
			for (Node corner : connector.getEndpointNodes()) {
				((Node.NodeObject) corner).addEdge(connector);
			}
		}
		
		return new AtomicCube(point0, point1, corners, connectors);
	}
	
	
	private static Node node(final float x, final float y, final float z) {
		return node(new CubePoint(x, y, z));
	}
	
	private static Node node(final CubePoint point) {
		return new Node.NodeObject(point);
	}
	
	private static Edge edge(final Node node0, final Node node1) {
		return new Edge.EdgeObject(node0, node1);
	}
	

	
	
	
	
	private final Node[] corners;
	private final Edge[] connectors;
	
	private AtomicCube(final CubePoint _point0, final CubePoint _point1, 
			final Node[] _corners, final Edge[] _connectors
	) {
		super(_point0, _point1);
		
		this.corners = new Node[_corners.length];
		System.arraycopy(_corners, 0, corners, 0, corners.length);
		this.connectors = new Edge[_connectors.length];
		System.arraycopy(_connectors, 0, connectors, 0, connectors.length);
	}
	
	
	/**************************
	 * CUBE IMPLEMENTATION
	 **************************/
	
	public int nodeCount() {
		return corners.length;
	}
	
	public int edgeCount() {
		return connectors.length;
	}
	
	public void getNodes(final Collection<Node> nodes) {
		nodes.addAll(Arrays.asList(corners));
	}
	
	public void getEdges(final Collection<Edge> edges) {
		edges.addAll(Arrays.asList(connectors));
	}
	
	public Node findClosestNode(final CubePoint point) {
		Node minc = corners[0];
		float mind = minc.getPoint().distance(point);
		for (int i = 1; i < corners.length; i++) {
			final Node corner = corners[i];
			final float d = corner.getPoint().distance(point);
			if (d < mind) {
				mind = d;
				minc = corner;
			}
		}
		return minc;
	}
	
	public Edge findClosestMidpoint(final CubePoint point) {
		Edge minc = connectors[0];
		float mind = CubePoint.minDistance(point, minc.getMidpoints());
		for (int i = 1; i < connectors.length; i++) {
			final Edge connector = connectors[i];
			final float d = CubePoint.minDistance(point, connector.getMidpoints());
			if (d < mind) {
				mind = d;
				minc = connector;
			}
		}
		return minc;
	}
	
	// does a 3x3 subdivision
	// only a 3x3 has a meaningul euler correction
	public Cube subdivide(final int iterations) {
		if (iterations <= 0)
			return this;
		
		final CubePoint point0 = getPoint0();
		final CubePoint point1 = getPoint1();
		
		final float x0 = 3 * point0.x;
		final float x1 = x0 + 3;
		final float xm0 = x0 + 1;
		final float xm1 = x0 + 2;
		
		final float y0 = 3 * point0.y;
		final float y1 = y0 + 3;
		final float ym0 = y0 + 1;
		final float ym1 = y0 + 2;
		
		final float z0 = 3 * point0.z;
		final float z1 = z0 + 3;
		final float zm0 = z0 + 1;
		final float zm1 = z0 + 2;
		
		
		// 0,0,0 -> m,m,m
		// m,0,0 -> 1,m,m
		// 0,m,0 -> m,1,m
		// m,m,0 -> 1,1,m
		// 0,0,m -> m,m,1
		// m,0,m -> 1,m,1
		// 0,m,m -> m,1,1
		// m,m,m -> 1,1,1

		final Cube cube = new ManifoldCube(
			new CubePoint(x0, y0, z0),
			new CubePoint(x1, y1, z1),
			new Cube[]{
//				atomic(new CubePoint(x0, y0, z0), new CubePoint(xm0, ym0, zm0)),
//				atomic(new CubePoint(xm0, y0, z0), new CubePoint(xm1, ym0, zm0)),
//				atomic(new CubePoint(x0, ym0, z0), new CubePoint(xm0, ym1, zm0)),
//				atomic(new CubePoint(xm0, ym0, z0), new CubePoint(xm1, ym1, zm0)),
//				atomic(new CubePoint(x0, y0, zm0), new CubePoint(xm0, ym0, zm1)),
//				atomic(new CubePoint(xm0, y0, zm0), new CubePoint(xm1, ym0, zm1)),
//				atomic(new CubePoint(x0, ym0, zm0), new CubePoint(xm0, ym1, zm1)),
//				atomic(new CubePoint(xm0, ym0, zm0), new CubePoint(xm1, ym1, zm1)),
				
				// Layer by layer, building on z:
				
				// z0-zm0: {{{
				slice(x0, xm0, 		y0, ym0, 	z0, zm0),
				slice(xm0, xm1, 	y0, ym0, 	z0, zm0),
				slice(xm1, x1, 		y0, ym0, 	z0, zm0),
				slice(x0, xm0, 		ym0, ym1, 	z0, zm0),
				slice(xm0, xm1, 	ym0, ym1, 	z0, zm0),
				slice(xm1, x1, 		ym0, ym1, 	z0, zm0),
				slice(x0, xm0, 		ym1, y1, 	z0, zm0),
				slice(xm0, xm1, 	ym1, y1, 	z0, zm0),
				slice(xm1, x1, 		ym1, y1, 	z0, zm0),
				// }}}
				
				// zm0-zm1: {{{
				slice(x0, xm0, 		y0, ym0, 	zm0, zm1),
				slice(xm0, xm1, 	y0, ym0, 	zm0, zm1),
				slice(xm1, x1, 		y0, ym0, 	zm0, zm1),
				slice(x0, xm0, 		ym0, ym1, 	zm0, zm1),
				slice(xm0, xm1, 	ym0, ym1, 	zm0, zm1),
				slice(xm1, x1, 		ym0, ym1, 	zm0, zm1),
				slice(x0, xm0, 		ym1, y1, 	zm0, zm1),
				slice(xm0, xm1, 	ym1, y1, 	zm0, zm1),
				slice(xm1, x1, 		ym1, y1, 	zm0, zm1),
				// }}}
				
				// zm1-z1: {{{
				slice(x0, xm0, 		y0, ym0, 	zm1, z1),
				slice(xm0, xm1, 	y0, ym0, 	zm1, z1),
				slice(xm1, x1, 		y0, ym0, 	zm1, z1),
				slice(x0, xm0, 		ym0, ym1, 	zm1, z1),
				slice(xm0, xm1, 	ym0, ym1, 	zm1, z1),
				slice(xm1, x1, 		ym0, ym1, 	zm1, z1),
				slice(x0, xm0, 		ym1, y1, 	zm1, z1),
				slice(xm0, xm1, 	ym1, y1, 	zm1, z1),
				slice(xm1, x1, 		ym1, y1, 	zm1, z1)
				// }}}
			}
		);
		
		return iterations <= 1
			? cube
			: cube.subdivide(iterations - 1);
	}
	
	public int getDivisions() {
		return 0;
	}
	
	/**************************
	 * END CUBE IMPLEMENTATION
	 **************************/
}
