package org.resisttheb.x.eulercube;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class ManifoldCube extends Cube.AbstractCube {
	/**
	 * The number of segments on each edge.
	 */
	private final int segCount;
	
	private final Map<CubePoint, Cube> cubeMap;
	
	
	public ManifoldCube(final CubePoint _point0, final CubePoint _point1, 
			final Cube ... cubes
	) {
		super(_point0, _point1);
		
		segCount = (int) Math.round(Math.pow(cubes.length, 1.f / 3.f));
		cubeMap = new HashMap<CubePoint, Cube>(cubes.length);
		for (Cube cube : cubes) {
			cubeMap.put(index(cube.getPoint0()), cube);
		}
	}
	
	
	private CubePoint index(final CubePoint point) {
		final float ux = (point.x - point0.x) / (point1.x - point0.x);
		final float uy = (point.y - point0.y) / (point1.y - point0.y);
		final float uz = (point.z - point0.z) / (point1.z - point0.z);
		
		final int ix = Math.min((int) Math.floor(ux * segCount), segCount - 1);
		final int iy = Math.min((int) Math.floor(uy * segCount), segCount - 1);
		final int iz = Math.min((int) Math.floor(uz * segCount), segCount - 1);
		
		return new CubePoint(ix, iy, iz);
	}
	
	
	/**************************
	 * CUBE IMPLEMENTATION
	 **************************/
	
	public int nodeCount() {
		int count = 0;
		for (Cube cube : cubeMap.values()) {
			count += cube.nodeCount();
		}
		return count;
	}
	
	public int edgeCount() {
		int count = 0;
		for (Cube cube : cubeMap.values()) {
			count += cube.edgeCount();
		}
		return count;
	}
	
	public void getNodes(final Collection<Node> nodes) {
		for (Cube cube : cubeMap.values()) {
			cube.getNodes(nodes);
		}
	}
	
	public void getEdges(final Collection<Edge> edges) {
		for (Cube cube : cubeMap.values()) {
			cube.getEdges(edges);
		}
	}
	
	public Node findClosestNode(final CubePoint point) {
		final Cube cube = cubeMap.get(index(point));
		assert null != cube;
		if (null == cube) return null;
		return cube.findClosestNode(point);
	}
	
	public Edge findClosestMidpoint(final CubePoint point) {
		final Cube cube = cubeMap.get(index(point));
		assert null != cube;
		if (null == cube) return null;
		return cube.findClosestMidpoint(point);
	}
	
	public Cube subdivide(final int iterations) {
		if (iterations <= 0)
			return this;
		
		final Cube[] scubes = new Cube[cubeMap.size()];
		int i = 0;
		for (Cube cube : cubeMap.values()) {
			scubes[i++] = cube.subdivide(iterations);
		}
		int m = 3;
		for (i = 1; i < iterations; i++) m *= 3;
		return new ManifoldCube(
				new CubePoint(m * point0.x, m * point0.y, m * point0.z), 
				new CubePoint(m * point1.x, m * point1.y, m * point1.z), 
				scubes);
	}
	
	public int getDivisions() {
		int maxd = 0;
		for (Cube cube : cubeMap.values()) {
			final int d = cube.getDivisions();
			if (maxd < d) maxd = d;
			assert d == maxd : "Cube is not symmetrically divided.";
		}
		return 1 + maxd;
	}
	
	/**************************
	 * END CUBE IMPLEMENTATION
	 **************************/
}
