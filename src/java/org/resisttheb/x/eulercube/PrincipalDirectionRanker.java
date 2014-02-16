package org.resisttheb.x.eulercube;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class PrincipalDirectionRanker implements EdgeRanker {
	private CubePoint[] dirs;
	private float falloffBase;

	
	public PrincipalDirectionRanker(final float _falloffBase, final CubePoint ... _dirs) {
		this.falloffBase = _falloffBase;
		this.dirs = new CubePoint[_dirs.length];
		System.arraycopy(_dirs, 0, dirs, 0, dirs.length);
	}
	
	
	private void weight(final float[] vector) {
		final float length = (float) Math.sqrt(
				vector[0] * vector[0] +
				vector[1] * vector[1] +
				vector[2] * vector[2]
			);
		final float m = (float) Math.pow(falloffBase, length);
		vector[0] *= m;
		vector[1] *= m;
		vector[2] *= m;
	}
	
	/**************************
	 * EDGERANKER IMPLEMENTATION
	 **************************/
	
	public void sort(final Node current, final Edge[] edges) {
		final Map<Edge, Float> projectionMap = new HashMap<Edge, Float>(edges.length);
		
		// 1. compute all vectors (point from currrent to dirs)
		// 2. weight the vectors based on their length (falloff)
		// 3. average
		// 4. project each edge onto the average, and take the largest value
		
		final CubePoint point = current.getPoint();
		
		final float[][] vectors = new float[dirs.length][];
		for (int i = 0; i < dirs.length; i++) {
			final CubePoint dir = dirs[i];
			vectors[i] = new float[]{
					dir.x - point.x,
					dir.y - point.y,
					dir.z - point.z
			};
		}
		for (float[] vector : vectors) weight(vector);
		
		final float[] average = {0, 0, 0};
		for (float[] vector : vectors) {
			average[0] += vector[0];
			average[1] += vector[1];
			average[2] += vector[2];
		}
		average[0] /= vectors.length;
		average[1] /= vectors.length;
		average[2] /= vectors.length;
		
		for (Edge edge : edges) {
			final Node opposite = EulerUtilities.opposite(current, edge);
			final CubePoint opoint = opposite.getPoint();
			final float[] evector = {
				opoint.x - point.x,	
				opoint.y - point.y,	
				opoint.z - point.z	
			};
			final float proj = 
				evector[0] * average[0] +
				evector[1] * average[1] +
				evector[2] * average[2];
			projectionMap.put(edge, proj);
		}
		
		Arrays.sort(edges, new MapComparator<Edge, Float>(MapComparator.Direction.DESCENDING, projectionMap));
	}
	
	/**************************
	 * END EDGERANKER IMPLEMENTATION
	 **************************/
}
