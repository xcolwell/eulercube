package org.resisttheb.x.eulercube;

import java.awt.BorderLayout;

import javax.media.opengl.GLCanvas;
import javax.swing.JApplet;

public class VisualWalkerApplet extends JApplet {
	private InteractiveCube icube = null;
	private VisualWalker vw = null;
	
	
	public VisualWalkerApplet() {
	}
	
	
	/**************************
	 * JAPPLET OVERRIDES
	 **************************/
	
	@Override
	public void init() {
		Cube cube = AtomicCube.atom(
				new CubePoint(0, 0, 0), 
				new CubePoint(1, 1, 1)
			);
		cube = cube.subdivide(2);
		cube = new AgglomerateCube(cube);

		icube = new InteractiveCube(cube);
		
		
		vw = new VisualWalker(icube);
		icube.setPerspective(new Perspective.ExponentialPerspective(1.0027f));
		
		icube.setScale(/*37.f*/20.f);
		
		
		final CubePoint point0 = cube.getPoint0();
		final CubePoint point1 = cube.getPoint1();
		
		final EdgeRanker pdr = new PrincipalDirectionRanker(
				0.5f, 
				new CubePoint(point0.x, point0.y, point0.z),
				new CubePoint(point1.x, point0.y, point0.z),
				new CubePoint(point0.x, point1.y, point0.z),
				new CubePoint(point1.x, point1.y, point0.z),
				new CubePoint(point0.x, point0.y, point1.z),
				new CubePoint(point1.x, point0.y, point1.z),
				new CubePoint(point0.x, point1.y, point1.z),
				new CubePoint(point1.x, point1.y, point1.z)
		);
		
		vw.setRanker(pdr);
		vw.setStartPoint(CubePoint.midpoint(point0, point1));
		
		vw.reset();
		
		
		// LAYOUT:
		final GLCanvas canvas = new GLCanvas();
		canvas.addGLEventListener(icube);
		
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);
	}
	
	@Override
	public void start() {
		if (null != icube) {
			icube.start(32.f);
		}
	}
	
	@Override
	public void stop() {
		if (null != icube) {
			icube.stop();
		}
	}
	
	/**************************
	 * END JAPPLET OVERRIDES
	 **************************/
}
