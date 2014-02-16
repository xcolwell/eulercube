package org.resisttheb.x.eulercube;
import java.util.Arrays;


public class CubePointTuple implements Comparable<CubePointTuple> {
	/**************************
	 * UTILITIES
	 **************************/
	
	public static boolean isFace(final Cube cube, final CubePoint ... points) {
		final CubePoint[] xpoints = {
				cube.getPoint0(),
				cube.getPoint1()
		};
		
		final boolean[][] faces = {
				{false, false, false},
				{false, false, false}
		};
		
		for (int i = 0; i < 2; i++) {
			final float[] coords = points[i].coords();
			for (int j = 0; j < 2; j++) {
				final float[] xcoords = xpoints[j].coords();
				
				for (int k = 0; k < 3; k++) {
					if (coords[k] == xcoords[k]) faces[i][k] = true;
				}
			}
		}
		
		for (int i = 0; i < 3; i++) {
			if (faces[0][i] && faces[1][i]) return true;
		}
		return false;
	}
	
	/**************************
	 * END UTILITIES
	 **************************/
	
	private boolean hashCached 	= false;
	private int hashCache 		= 0;
	
	private final CubePoint[] points;
	
	public CubePointTuple(final CubePoint ... _points) {
		points = new CubePoint[_points.length];
		System.arraycopy(_points, 0, points, 0, points.length);
		Arrays.sort(points);
	}
	
	
	public CubePoint[] getPoints() {
		final CubePoint[] _points = new CubePoint[points.length];
		System.arraycopy(points, 0, _points, 0, points.length);
		return _points;
	}
	
	
	/**************************
	 * OBJECT OVERRIDES
	 **************************/
	
	@Override
	public int hashCode() {
		if (hashCached) return hashCache;
		
		int hash = 0;
		for (CubePoint point : points) {
			hash = 37 * hash + point.hashCode();
		}
		
		hashCache = hash;
		hashCached = true;
		return hashCache;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (! (obj instanceof CubePointTuple))
			return false;
		if (obj.hashCode() != hashCode())
			return false;
		final CubePointTuple tuple = (CubePointTuple) obj;
		if (points.length != tuple.points.length)
			return false;
		for (int i = 0; i < points.length; i++) {
			if (! points[i].equals(tuple.points[i])) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		final StringBuffer buffer = new StringBuffer(16 * points.length);
		buffer.append("[");
		for (int i = 0; i < points.length; i++) {
			if (0 < i) buffer.append(", ");
			buffer.append(points[i].toString());
		}
		buffer.append("]");
		return buffer.toString();
	}
	
	/**************************
	 * END OBJECT OVERRIDES
	 **************************/
	
	
	/**************************
	 * COMPARABLE IMPLEMENTATION
	 **************************/
	
	public int compareTo(final CubePointTuple tuple) {
		if (this.equals(tuple)) return 0;
		if (points.length != tuple.points.length) {
			return points.length - tuple.points.length;
		}
		for (int i = 0; i < points.length; i++) {
			final int d = points[i].compareTo(tuple.points[i]);
			if (0 != d) return d;
		}
		return 0;
	}
	
	/**************************
	 * END COMPARABLE IMPLEMENTATION
	 **************************/
	
	
}
