package org.resisttheb.x.eulercube;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AgglomerateCube implements Cube {
	/**************************
	 * UTILITIES
	 **************************/
	
	private static Collection<float[][]> generateEulerCorrectionPairs(final int segmentCount) {
		// shrink coords
		// in between point per dim = segCount - 1
		final int indexCount = segmentCount - 1;
		final List<float[][]> pairs = new ArrayList<float[][]>(indexCount * indexCount / 2);
		for (int i = 1; i + 1 < segmentCount; i += 2) {
			for (int j = 1; j + 1 < segmentCount; j += 2) {
				pairs.add(new float[][]{{i, j}, {i + 1, j + 1}});
				pairs.add(new float[][]{{i, j + 1}, {i + 1, j}});
			}
		}
		return pairs;
	}
	
	/**************************
	 * END UTILITIES
	 **************************/
	

	// TODO: utilies to find min dx, dy, dz in a cubepoint set
	
	private CubePoint point0 = null;
	private CubePoint point1 = null;
	private float[] div;
	
	private Cube structure = null;
	private final Map<CubePoint, Node> nodeMap;
	// key is endpoints
	private final Map<CubePointTuple, Edge> edgeMap;
	
	
	public AgglomerateCube(final Cube _structure) {
		if (null == _structure)
			throw new IllegalArgumentException();
		
		nodeMap = new HashMap<CubePoint, Node>(4);
		edgeMap = new HashMap<CubePointTuple, Edge>(4);
		
		setStructure(_structure);
	}
	
	
	private void setStructure(final Cube _structure) {
		if (structure == _structure)
			return;
		structure = _structure;
		rebuild();
	}
	
	private void rebuild() {
		nodeMap.clear();
		edgeMap.clear();
		
		// hash all node points and edge tuples in sets
		// add extra edges according to eulerization algorithm
		// 
		// derive multimaps of point -> all connected points
		//    based on edge set
		// create nodes and edges from this map
		// 1. create nodes
		// 2. attach edges
		
		//final Set<CubePoint> pnodes = new HashSet<CubePoint>();
		final Set<CubePointTuple> pedges = new HashSet<CubePointTuple>();
		
		//final List<Node> snodes = new ArrayList<Node>(structure.nodeCount());
		final List<Edge> sedges = new ArrayList<Edge>(structure.edgeCount());
		
		//structure.getNodes(snodes);
		structure.getEdges(sedges);
		
		
		final CubePoint _point0 = structure.getPoint0();
		final CubePoint _point1 = structure.getPoint1();
		final int segmentCount = (int) Math.pow(3, structure.getDivisions());
		div = new float[]{
				(_point1.x - _point0.x) / segmentCount,
				(_point1.y - _point0.y) / segmentCount,
				(_point1.z - _point0.z) / segmentCount
		};
		assert 1.f == div[0];
		assert 1.f == div[1];
		assert 1.f == div[2];
		point0 = structure.getPoint0();
		point1 = structure.getPoint1();
		
		assert point1.x - point0.x == segmentCount;
		assert point1.y - point0.y == segmentCount;
		assert point1.z - point0.z == segmentCount;
		
		for (Edge sedge : sedges) {
			// Scale to integer coords:
			pedges.add(new CubePointTuple(sedge.getEndpoints()));
		}
		
//		final float[][] minmax = CubePoint.minmax(pedges);
//		setExtrema(
//			new CubePoint(minmax[0][0], minmax[0][1], minmax[0][2]),
//			new CubePoint(minmax[1][0], minmax[1][1], minmax[1][2])
//		);
		final float[][] minmax = {
				{point0.x, point0.y, point0.z},
				{point1.x, point1.y, point1.z}
		};
		
		// DEBUGGING {{{
		for (CubePointTuple pedge : pedges) {
			for (CubePoint point : pedge.getPoints()) {
				assert point0.x <= point.x;
				assert point.x <= point1.x;
				assert point0.y <= point.y;
				assert point.y <= point1.y;
				assert point0.z <= point.z;
				assert point.z <= point1.z;
			}
		}
		final float[][] minmax2 = CubePoint.minmax(pedges);
		assert point0.x == minmax2[0][0];
		assert point0.y == minmax2[0][1];
		assert point0.z == minmax2[0][2];
		assert point1.x == minmax2[1][0];
		assert point1.y == minmax2[1][1];
		assert point1.z == minmax2[1][2];
		// }}}
		
		
		
		// Euler Correction {{{
		final Collection<float[][]> pairs = generateEulerCorrectionPairs(segmentCount);
		for (float[][] pair : pairs) {
			for (float[] mm : minmax) {
				pedges.add(new CubePointTuple(
						new CubePoint(mm[0], minmax[0][1] + pair[0][0], minmax[0][2] + pair[0][1]),
						new CubePoint(mm[0], minmax[0][1] + pair[1][0], minmax[0][2] + pair[1][1])
					));
				pedges.add(new CubePointTuple(
						new CubePoint(minmax[0][0] + pair[0][0], mm[1], minmax[0][2] + pair[0][1]),
						new CubePoint(minmax[0][0] + pair[1][0], mm[1], minmax[0][2] + pair[1][1])
					));
				pedges.add(new CubePointTuple(
						new CubePoint(minmax[0][0] + pair[0][0], minmax[0][1] + pair[0][1], mm[2]),
						new CubePoint(minmax[0][0] + pair[1][0], minmax[0][1] + pair[1][1], mm[2])
					));
			}
		}
		// }}}
		
		
//		final Multimap<CubePoint, CubePoint> pnodeMap = new ArrayListMultimap<CubePoint, CubePoint>();
//		// Quadratic memory explosion:
//		for (CubePointTuple pedge : pedges) {
//			for (int i = 0; i < pedge.size(); i++) {
//				for (int j = 0; j < pedge.size(); j++) {
//					if (i == j) continue;
//					pnodeMap.put(pedge.get(i), pedge.get(j));
//				}
//			}
//		}
		final Set<CubePoint> pnodes = new HashSet<CubePoint>(pedges.size() / 2);
		for (CubePointTuple pedge : pedges) {
			for (CubePoint point : pedge.getPoints()) {
				pnodes.add(point);
			}
		}
		
		
		
		for (CubePoint pnode : pnodes) {
			nodeMap.put(pnode, new Node.NodeObject(pnode));
		}
		for (CubePointTuple pedge : pedges) {
			final CubePoint[] points = pedge.getPoints();
			final Node[] nodes = new Node[points.length];
			for (int i = 0; i < points.length; i++) {
				nodes[i] = nodeMap.get(points[i]);
			}
			final Edge edge = new Edge.EdgeObject(nodes);
			for (Node node : nodes) {
				((Node.NodeObject) node).addEdge(edge);
			}
			
			edgeMap.put(pedge, edge);
		}
	}
	
	
	/**************************
	 * INTEGER SCALING
	 **************************/
	/*
	private static float nround(final float v) {
		//return Math.round(v * 10.f) / 10.f;
		return v;
	}
	
	private CubePoint scale(final CubePoint point) {
		return new CubePoint(
			nround(point.x / div[0]),
			nround(point.y / div[1]),
			nround(point.z / div[2])
		);
	}
	
	private CubePointTuple scale(final CubePointTuple tuple) {
		final CubePoint[] points = tuple.getPoints();
		final CubePoint[] spoints = new CubePoint[points.length];
		for (int i = 0; i < spoints.length; i++) {
			spoints[i] = scale(points[i]);
		}
		return new CubePointTuple(spoints);
	}
	
	
	private CubePoint iscale(final CubePoint point) {
		return new CubePoint(
			point.x * div[0],
			point.y * div[1],
			point.z * div[2]
		);
	}
	
	private CubePointTuple iscale(final CubePointTuple tuple) {
		final CubePoint[] points = tuple.getPoints();
		final CubePoint[] spoints = new CubePoint[points.length];
		for (int i = 0; i < spoints.length; i++) {
			spoints[i] = iscale(points[i]);
		}
		return new CubePointTuple(spoints);
	}
	*/
	
	/**************************
	 * END INTEGER SCALING
	 **************************/
	
	
	
	
	/**************************
	 * CUBE IMPLEMENTATION
	 **************************/
	
	public CubePoint getPoint0() {
		return point0;
	}
	
	public CubePoint getPoint1() {
		return point1;
	}
	
	
	public int nodeCount() {
		return nodeMap.size();
	}
	
	public int edgeCount() {
		return edgeMap.size();
	}
	
	public void getNodes(final Collection<Node> nodes) {
		nodes.addAll(nodeMap.values());
	}
	
	public void getEdges(final Collection<Edge> edges) {
		edges.addAll(edgeMap.values());
	}
	
	public Node findClosestNode(final CubePoint point) {
		final Node structureNode = structure.findClosestNode(point);
		assert null != structureNode;
		if (null == structureNode) return null;
		return nodeMap.get(structureNode.getPoint());
	}
	
	public Edge findClosestMidpoint(final CubePoint point) {
		final Edge structureEdge = structure.findClosestMidpoint(point);
		assert null != structureEdge;
		if (null == structureEdge) return null;
		return edgeMap.get(new CubePointTuple(structureEdge.getEndpoints()));
	}
	
	public Cube subdivide(final int iterations) {
		return new AgglomerateCube(structure.subdivide(iterations));
	}
	
	public int getDivisions() {
		return structure.getDivisions();
	}
	
	/**************************
	 * END CUBE IMPLEMENTATION
	 **************************/
}
