package org.resisttheb.x.eulercube;
import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

public interface NodeRenderer {
	public static final class Simple extends AbstractNodeRenderer {
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
		
		public void render(final Node node, final Object rdata, final float[] r0) {
			gl.glColor4f(fr, fg, fb, (r0[2] - zmin) / (zmax - zmin) * fa);
			gl.glPointSize(1.f + 6.f * (r0[2] - zmin) / (zmax - zmin));
			gl.glBegin(GL.GL_POINTS);
				gl.glVertex2f(r0[0], r0[1]);
			gl.glEnd();
		}
		
		/**************************
		 * END NODERENDERER IMPLEMENTATION
		 **************************/
	}
	
	
	public static abstract class AbstractNodeRenderer implements NodeRenderer {
		protected GL gl;
		protected GLU glu;
		protected float zmin;
		protected float zmax;
		
		public AbstractNodeRenderer() {
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
		public Object createRenderData(final Node node) {
			return null;
		}
		
		/**************************
		 * END NODERENDERER IMPLEMENTATION
		 **************************/
	}
	
	public void init0(final GL gl, final GLU glu);
	public void init(final GL gl, final GLU glu, final float _zmin, final float _zmax);
	public void render(final Node node, final Object rdata, final float[] r);
	public void uninit();
	
	public Object createRenderData(final Node node);
}
