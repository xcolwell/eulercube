package org.resisttheb.x.eulercube;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

public interface EdgeRenderer {
	public static final class Simple extends AbstractEdgeRenderer {
		private Color color;
		private float fr;
		private float fg;
		private float fb;
		private float fa;
		
		
		public Simple(final Color _color) {
			this.color = _color;
			fr = color.getRed() / 255.f;
			fg = color.getGreen() / 255.f;
			fb = color.getBlue() / 255.f;
			fa = color.getAlpha() / 255.f;
		}
		
		/**************************
		 * NODERENDERER IMPLEMENTATION
		 **************************/
		
		public void render(final Edge edge, final Object rdata, final float[] r0, final float[] r1) {
			final float zm = (r0[2] + r1[2]) / 2.f;
			gl.glColor4f(fr, fg, fb, (zm - zmin) / (zmax - zmin) * fa);
			gl.glLineWidth(0.1f + 1.2f * (zm - zmin) / (zmax - zmin));
			gl.glBegin(GL.GL_LINES);
				gl.glVertex2f(r0[0], r0[1]);
				gl.glVertex2f(r1[0], r1[1]);
			gl.glEnd();
		}
		
		/**************************
		 * END NODERENDERER IMPLEMENTATION
		 **************************/
	}
	
	public static abstract class AbstractEdgeRenderer implements EdgeRenderer {
		protected GL gl = null;
		protected GLU glu = null;
		protected float zmin;
		protected float zmax;
		
		public AbstractEdgeRenderer() {
		}

		/**************************
		 * NODERENDERER IMPLEMENTATION
		 **************************/
		
		@Override
		public void init0(final GL gl, final GLU glu) {
		}
		
		@Override
		public void init(final GL _gl, final GLU _glu, final float _zmin, final float _zmax) {
			this.gl = _gl;
			this.glu = _glu;
			this.zmin = _zmin;
			this.zmax = _zmax;
		}
		
		@Override
		public void uninit() {
			gl = null;
			glu = null;
		}
		
		@Override
		public Object createRenderData(final Edge edge) {
			return null;
		}
		
		/**************************
		 * END NODERENDERER IMPLEMENTATION
		 **************************/
	}
	
	public void init0(final GL gl, final GLU glu);
	public void init(final GL gl, final GLU glu, final float zmin, final float zmax);
	public void render(final Edge edge, final Object rdata, final float[] r0, final float[] r1);
	public void uninit();
	
	public Object createRenderData(final Edge edge);
}
