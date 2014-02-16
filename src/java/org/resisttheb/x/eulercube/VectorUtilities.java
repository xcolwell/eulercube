package org.resisttheb.x.eulercube;

public class VectorUtilities {

	/**************************
	 * VECTOR UTILITIES
	 **************************/
	
	public static float mag(final float[] v) {
		return (float) Math.sqrt(magSq(v));
	}
	
	public static float magSq(final float[] v) {
		return v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
	}
	
	public static void norm(final float[] v, final float[] out) {
		final float m = mag(v);
		out[0] = v[0] / m;
		out[1] = v[1] / m;
		out[2] = v[2] / m;
	}
	
	public static void copy(final float[] in, final float[] out) {
		out[0] = in[0];
		out[1] = in[1];
		out[2] = in[2];
	}
	
	public static void diff(final float[] a, final float[] b, final float[] out) {
		out[0] = a[0] - b[0];
		out[1] = a[1] - b[1];
		out[2] = a[2] - b[2];
	}
	
	public static void cross(final float[] a, final float[] b, final float[] out) {
		out[0] = a[1] * b[2] - a[2] * b[1];
		out[1] = a[2] * b[0] - a[0] * b[2];
		out[2] = a[0] * b[1] - a[1] * b[0];
	}
	
	public static float dot(final float[] a, final float[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}
	
	public static float angle(final float[] a, final float[] b) {
		// a.b = cos(theta).|a||b|
		// arccos(a.b / |a||b|)
		// axb = sin(theta).|a||b|
		// arcsin(|axb| / |a||b|) -- use this sign
		
		final float ab = mag(a) * mag(b);
		final float aDotb = dot(a, b);
		final float[] aCrossb = new float[3];
		cross(a, b, aCrossb);
		
		final float dotTheta = (float) Math.acos(aDotb / ab);
		final float crossTheta = (float) Math.asin(mag(aCrossb) / ab);
		
		return (crossTheta < 0 ? -dotTheta : dotTheta); 
	}
	
	/**************************
	 * END VECTOR UTILITIES
	 **************************/
	
	
	private VectorUtilities() {
	}
}
