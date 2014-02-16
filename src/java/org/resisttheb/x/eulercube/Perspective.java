package org.resisttheb.x.eulercube;


public interface Perspective {
	public static final class Null implements Perspective {
		public void Null() {
		}

		// =========================
		// <code>Perspective</code> implementation
		
		public void apply(float[] point, float[] center, float[] out) {
			out[0] = point[0];
			out[1] = point[1];
		}
		
		// =========================
	}
	
	public static final class LinearPerspective implements Perspective {
		private float alpha;
		
		public LinearPerspective(final float _alpha) {
			alpha = _alpha;
		}
		
		// =========================
		// <code>Perspective</code> implementation
		
		public void apply(final float[] point, final float[] center, final float[] out) {
			final float dz = point[2] - center[2];
			final float m = 1 + alpha * dz;
			out[0] = (point[0] - center[0]) * m + center[0];
			out[1] = (point[1] - center[1]) * m + center[1];
		}
		
		// =========================
	}
	

	public static final class ExponentialPerspective implements Perspective {
		private float base;
		
		public ExponentialPerspective(final float beta, final float alpha) {
			this((float) Math.pow(beta, alpha));
		}
		
		public ExponentialPerspective(final float _base) {
			base = _base;
		}

		// =========================
		// <code>Perspective</code> implementation
		
		public void apply(final float[] point, final float[] center, final float[] out) {
			final float dz = point[2] - center[2];
			final float m = (float) Math.pow(base, dz);
			out[0] = (point[0] - center[0]) * m + center[0];
			out[1] = (point[1] - center[1]) * m + center[1];
		}
		
		// =========================
	}
	
	
	
	// reduces to a 2d point -- out is 2D
	public void apply(final float[] point, final float[] center, final float[] out);
}