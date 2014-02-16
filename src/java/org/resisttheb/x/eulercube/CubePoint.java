package org.resisttheb.x.eulercube;
import java.util.Collection;


public final class CubePoint implements Comparable<CubePoint> {
	/**************************
	 * UTILITIES
	 **************************/
	
	private static int countMatches(final CubePoint a, final CubePoint b) {
		int net = 0;
		if (a.x == b.x) ++net;
		if (a.y == b.y) ++net;
		if (a.z == b.z) ++net;
		return net;
	}
	
	
	public static boolean isOuter(final Cube cube, final CubePoint point) {
		return 0 < countMatches(cube.getPoint0(), point) + 
			countMatches(cube.getPoint1(), point);
	}
	
	public static boolean isFace(final Cube cube, final CubePoint point) {
		return 1 == countMatches(cube.getPoint0(), point) + 
			countMatches(cube.getPoint1(), point);
	}
	
	public static boolean isEdge(final Cube cube, final CubePoint point) {
		return 2 == countMatches(cube.getPoint0(), point) + 
			countMatches(cube.getPoint1(), point);
	}
	
	public static boolean isCorner(final Cube cube, final CubePoint point) {
		return 3 == countMatches(cube.getPoint0(), point) + 
			countMatches(cube.getPoint1(), point);
	}
	
	
	public static CubePoint midpoint(final CubePoint ... points) {
		double netx = 0;
		double nety = 0;
		double netz = 0;
		for (CubePoint point : points) {
			netx += point.x;
			nety += point.y;
			netz += point.z;
		}
		final int n = points.length;
		return new CubePoint(
			(float) Math.round(netx / n),
			(float) Math.round(nety / n),
			(float) Math.round(netz / n)
		);
	}
	
	
	public static float lerp(final float a, final float b, final float u) {
		return a + (b - a) * u;
	}
	
	public static CubePoint lerp(final CubePoint a, final CubePoint b, final float u) {
		return new CubePoint(
				lerp(a.x, b.x, u),
				lerp(a.y, b.y, u),
				lerp(a.z, b.z, u)
		);
	}
	
	public static float minDistance(final CubePoint a, final CubePoint ... bs) {
		float mind = a.distance(bs[0]);
		for (int i = 1; i < bs.length; i++) {
			final float d = a.distance(bs[i]);
			if (d < mind) mind = d;
		}
		return mind;
	}
	
	
	public static float[][] minmax(final Collection<CubePointTuple> tuples) {
		final int MIN = 0;
		final int MAX = 1;
		final int X = 0; final int Y = 1; final int Z = 2;
		final float[][] minmax = new float[2][];
		boolean unset = true;
		for (CubePointTuple tuple : tuples) {
			for (CubePoint point : tuple.getPoints()) {
				if (unset) {
					unset = false;
					minmax[MIN] = new float[]{point.x, point.y, point.z};
					minmax[MAX] = new float[]{point.x, point.y, point.z};
					continue;
				}
				
				if (point.x < minmax[MIN][X]) minmax[MIN][X] = point.x;
				if (minmax[MAX][X] < point.x) minmax[MAX][X] = point.x;
				
				if (point.y < minmax[MIN][Y]) minmax[MIN][Y] = point.y;
				if (minmax[MAX][Y] < point.y) minmax[MAX][Y] = point.y;
				
				if (point.z < minmax[MIN][Z]) minmax[MIN][Z] = point.z;
				if (minmax[MAX][Z] < point.z) minmax[MAX][Z] = point.z;
			}
		}
		return minmax;
	}
	
	/**************************
	 * END UTILITIES
	 **************************/
	
	private boolean hashCached 	= false;
	private int hashCache 		= 0;
	
	public final float x;
	public final float y;
	public final float z;
	
	public CubePoint(final float _x, final float _y, final float _z) {
		this.x = _x;
		this.y = _y;
		this.z = _z;
	}
	
	public float[] coords() {
		return new float[]{x, y, z};
	}
	
	public float distance(final CubePoint point) {
		final float dx = x - point.x;
		final float dy = y - point.y;
		final float dz = z - point.z;
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	/**************************
	 * OBJECT OVERRIDES
	 **************************/
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (! (obj instanceof CubePoint))
			return false;
		if (obj.hashCode() != hashCode())
			return false;
		final CubePoint point = (CubePoint) obj;
		return x == point.x && y == point.y && z == point.z;
	}
	
	@Override
	public int hashCode() {
		if (hashCached) return hashCache;
		
		int hash = Float.floatToIntBits(x);
		hash = 37 * hash + Float.floatToIntBits(y);
		hash = 37 * hash + Float.floatToIntBits(z);
		
		hashCache = hash;
		hashCached = true;
		return hashCache;
	}
	
	@Override
	public String toString() {
		return String.format("(%f, %f, %f)", x, y, z);
	}
	
	/**************************
	 * END OBJECT OVERRIDES
	 **************************/
	
	/**************************
	 * COMPARABLE IMPLEMENTATION
	 **************************/
	
	public int compareTo(final CubePoint point) {
		if (this.equals(point)) return 0;
		float d = x - point.x;
		if (0 != d) return (int) Math.signum(d);
		d = y - point.y;
		if (0 != d) return (int) Math.signum(d);
		d = z - point.z;
		if (0 != d) return (int) Math.signum(d);
		return 0;
	}
	
	/**************************
	 * END COMPARABLE IMPLEMENTATION
	 **************************/
}
