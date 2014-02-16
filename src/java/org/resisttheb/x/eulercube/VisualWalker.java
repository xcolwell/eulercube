package org.resisttheb.x.eulercube;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;


public class VisualWalker {
	private static final int HOLD = 2;
	private static final int TAIL = 200;
	
	private static final float N_COLOR_A = 0.5f;
	private static final float N_COLOR_B = 0.5f;
	private static final float N_ALPHA = 0.55f;
	private static final float N_SIZE_A = 1.f;
	private static final float N_SIZE_B = 6.f;
	
	private static final float E_ACTIVE_SIZE_A = 2.f;
	private static final float E_ACTIVE_SIZE_B = 5.5f;
	private static final float E_ACTIVE_SIZE_C = 5.f;
	private static final float E_COLOR_A = 0.6f;
	private static final float E_COLOR_B = 0.4f;
	private static final float E_ALPHA = 0.37f;
	private static final float E_SPECIAL_ALPHA = 0.77f;
	private static final float E_SIZE_A = 0.2f;
	private static final float E_SIZE_B = 1.1f;
	
	
	/**************************
	 * RENDER DATA STRUCTS
	 **************************/
	
	private static final class _NRData {
		public CubePoint point;
		public float fr, fg, fb;
		
		public int edgeCount;
		public boolean visible;
		
		public _NRData() {
		}
	}
	
	private static final class _ERData {
		public CubePoint point;
		public float fr, fg, fb;
		public float afr, afg, afb;
		
		public boolean active;
		public boolean reactive;
		public int activeFrames;
		public boolean visible;
		public boolean special;
		
		public _ERData() {
		}
	}
	
	/**************************
	 * END RENDER DATA STRUCTS
	 **************************/
	
	
	// uses color, as long as all attached edges are visible (there's a count on the node data that's active edges)
	private final class _NR extends NodeRenderer.AbstractNodeRenderer {
		protected float dz;
		
		public _NR() {
		}
		
		/**************************
		 * NODERENDERER IMPLEMENTATION
		 **************************/
		
		public Object createRenderData(Node node) {
			return nodeRenderData.get(node);
		}
		
		@Override
		public void init(GL _gl, GLU _glu, float _zmin, float _zmax) {
			super.init(_gl, _glu, _zmin, _zmax);
			dz = zmax - zmin;
		}
		
		@Override
		public void render(final Node node, final Object rdata, final float[] r) {
			final _NRData ndata = (_NRData) rdata;
			if (! ndata.visible) return;
			
			final float m = (r[2] - zmin) / dz;
			gl.glColor4f(ndata.fr, ndata.fg, ndata.fb, N_ALPHA * m);
			gl.glPointSize(N_SIZE_A + N_SIZE_B * m);
			gl.glBegin(GL.GL_POINTS);
				gl.glVertex2f(r[0], r[1]);
			gl.glEnd();
		}
		
		/**************************
		 * END NODERENDERER IMPLEMENTATION
		 **************************/
	}
	
	// for current edge, grow for M seconds.
	// after this, hide, decrement attached nodes, call advance()
	private final class _ER extends EdgeRenderer.AbstractEdgeRenderer {
		protected float dz;
		
		public _ER() {
		}
		
		/**************************
		 * EDGERENDERER IMPLEMENTATION
		 **************************/
		
		public Object createRenderData(Edge edge) {
			return edgeRenderData.get(edge);
		}
		
		@Override
		public void init(GL _gl, GLU _glu, float _zmin, float _zmax) {
			super.init(_gl, _glu, _zmin, _zmax);
			dz = zmax - zmin;
		}
		
		@Override
		public void render(final Edge edge, final Object rdata, final float[] r0, final float[] r1) {
			final _ERData edata = (_ERData) rdata;
			if (! edata.visible) return;
			
			if (edata.active || edata.reactive) {
				final float am = 1.f - (edata.activeFrames / (float) activeVisibleFrames);
				
				final float zm = (r0[2] + r1[2]) / 2.f;
				final float m = (zm - zmin) / dz;
				
				gl.glColor4f(edata.afr, edata.afg, edata.afb, am);
				gl.glLineWidth(E_ACTIVE_SIZE_A + E_ACTIVE_SIZE_B * m + E_ACTIVE_SIZE_C * am);
				
				gl.glBegin(GL.GL_LINES);
					gl.glVertex2f(r0[0], r0[1]);
					gl.glVertex2f(r1[0], r1[1]);
				gl.glEnd();
				
				// Advance the edge state:
				++edata.activeFrames;
				if (edata.active && activeHoldFrames <= edata.activeFrames) {
					advance();
				}
				else if (activeVisibleFrames <= edata.activeFrames) {
					edata.visible = false;
				}
			}
			else {
				final float fa = edata.special ? E_SPECIAL_ALPHA : E_ALPHA;
				
				final float zm = (r0[2] + r1[2]) / 2.f;
				final float m = (zm - zmin) / dz;
				
				gl.glColor4f(edata.fr, edata.fg, edata.fb, fa * m);
				gl.glLineWidth(E_SIZE_A + E_SIZE_B * m);
				
				gl.glBegin(GL.GL_LINES);
					gl.glVertex2f(r0[0], r0[1]);
					gl.glVertex2f(r1[0], r1[1]);
				gl.glEnd();
			}
		}
		
		/**************************
		 * END EDGERENDERER IMPLEMENTATION
		 **************************/
	}
	
	
	
	private int activeHoldFrames = HOLD;
	private int activeVisibleFrames = HOLD * TAIL;
	
	private final InteractiveCube icube;
	private final CubePoint point0;
	private final CubePoint point1;
	private final CubePoint cmid;
	private final float pdx, pdy, pdz;
	
	private EdgeRanker ranker = new EdgeRanker.Null();
	
	private final NodeRenderer nr;
	private final EdgeRenderer er;

	private final Map<Node, _NRData> nodeRenderData;
	private final Map<Edge, _ERData> edgeRenderData;
	
	private CubePoint startPoint = new CubePoint(0, 0, 0);
	
	
	/**************************
	 * PATH STATE
	 **************************/
	
	private Edge[] path = new Edge[0];
	private int pathIndex = -1;
	
	/**************************
	 * END PATH STATE
	 **************************/
	

	public VisualWalker(final InteractiveCube _icube) {
		this.icube = _icube;
		
		final Cube cube = icube.getCube();
		
		point0 = cube.getPoint0();
		point1 = cube.getPoint1();
		pdx = point1.x - point0.x;
		pdy = point1.y - point0.y;
		pdz = point1.z - point0.z;
		cmid = CubePoint.midpoint(point0, point1);
		
		
		nodeRenderData = new HashMap<Node, _NRData>(cube.nodeCount());
		edgeRenderData = new HashMap<Edge, _ERData>(cube.edgeCount());
		// Populate:
		final List<Node> nodes = new ArrayList<Node>(cube.nodeCount());
		final List<Edge> edges = new ArrayList<Edge>(cube.edgeCount());
		cube.getNodes(nodes);
		cube.getEdges(edges);
		for (Node node : nodes) {
			final _NRData ndata = new _NRData();
			final CubePoint point = node.getPoint();
			ndata.point = point;
			ndata.fr = N_COLOR_A + N_COLOR_B * (point.x - point0.x) / pdx;
			ndata.fg = N_COLOR_A + N_COLOR_B * (point.y - point0.y) / pdy;
			ndata.fb = N_COLOR_A + N_COLOR_B * (point.z - point0.z) / pdz;
			nodeRenderData.put(node, ndata);
		}
		for (Edge edge : edges) {
			final _ERData edata = new _ERData();
			final CubePoint point = edge.getMidpoints()[0];
			edata.afr = (point.x - point0.x) / pdx;
			edata.afg = (point.y - point0.y) / pdy;
			edata.afb = (point.z - point0.z) / pdz;
			edata.fr = E_COLOR_A + E_COLOR_B * (point.x - point0.x) / pdx;
			edata.fg = E_COLOR_A + E_COLOR_B * (point.y - point0.y) / pdy;
			edata.fb = E_COLOR_A + E_COLOR_B * (point.z - point0.z) / pdz;
			edgeRenderData.put(edge, edata);
		}
		
		nr = new _NR();
		er = new _ER();
		
		icube.setNodeRenderer(nr);
		icube.setEdgeRenderer(er);
	}
	
	
	public void setRanker(final EdgeRanker _ranker) {
		this.ranker = _ranker;
	}
	
	public void setStartPoint(final CubePoint _startPoint) {
		this.startPoint = _startPoint;
	}
	
	
	public void reset() {
		// 1. build path
		// 2. reset render data (all visible, set edge count on nodes)
		
		final Cube cube = icube.getCube();
		
		// Reset path:
		final List<Edge> pathList = new ArrayList<Edge>(cube.edgeCount());
		final Node startNode = cube.findClosestNode(startPoint);
		EulerUtilities.edgeCircuit(startNode, cube, ranker, pathList);
		
		path = (Edge[]) pathList.toArray(new Edge[pathList.size()]);
		pathIndex = -1;
		
		
		// Reset render data:
		for (Map.Entry<Node, _NRData> entry : nodeRenderData.entrySet()) {
			final Node node = entry.getKey();
			final _NRData ndata = entry.getValue();
			ndata.edgeCount = node.getDegree();
			ndata.visible = true;
		}
		for (Map.Entry<Edge, _ERData> entry : edgeRenderData.entrySet()) {
			final Edge edge = entry.getKey();
			final _ERData edata = entry.getValue();
			edata.active = false;
			edata.reactive = false;
			edata.activeFrames = 0;
			edata.visible = true;
			edata.special = false;
		}
		
		
		advance();
	}
	
	
	/**************************
	 * PATH CONTROL
	 **************************/
	
	private void advance() {
		if (0 <= pathIndex) {
			// POP:
			// 1. set visible = false
			// 2. set active = false
			// 3. decrement edge count on all attached nodes
			//    and update the visible bit of those nodes
			
			final Edge edge = path[pathIndex];
			final _ERData edata = edgeRenderData.get(edge);
			edata.active = false;
			edata.reactive = true;
			//edata.visible = false;
			for (Node node : edge.getEndpointNodes()) {
				final _NRData ndata = nodeRenderData.get(node);
				--ndata.edgeCount;
				if (ndata.edgeCount <= 2) {
					ndata.visible = false;
					for (Edge sedge : node.getEdges()) {
						_ERData sedata = edgeRenderData.get(sedge);
						sedata.special = true;
					}
				}
			}
		}
		++pathIndex;
		if (pathIndex < path.length) {
			// PUSH:
			
			// 1. set render frames
			// 2. set active bit
			

			final Edge edge = path[pathIndex];
			final _ERData edata = edgeRenderData.get(edge);
			edata.active = true;
			edata.reactive = false;
			// Ensure:
			edata.visible = true;
			edata.activeFrames = 0;
			
			// Orient the camera:
			final CubePoint mid = CubePoint.midpoint(edge.getMidpoints());
			icube.setCameraOrientation(new float[]{
				mid.x - cmid.x,
				mid.y - cmid.y,
				mid.z - cmid.z
			}, true);
		}
	}
	
	/**************************
	 * END PATH CONTROL
	 **************************/
	
	
	
	
	
	
	
	public static void main(final String[] in) {
		Cube cube = AtomicCube.atom(
				new CubePoint(0, 0, 0), 
				new CubePoint(1, 1, 1)
			);
		cube = cube.subdivide(2);
		cube = new AgglomerateCube(cube);

		final InteractiveCube icube = new InteractiveCube(cube);
		
		
		final VisualWalker vw = new VisualWalker(icube);
		icube.setPerspective(new Perspective.ExponentialPerspective(1.0027f));
		
		final GLCanvas canvas = new GLCanvas();
//		final GLJPanel canvas = new GLJPanel();
		canvas.addGLEventListener(icube);
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		
		c.setLayout(new BorderLayout());
		c.add(canvas, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		icube.setScale(/*37.f*//*20.f*/35.f);
		icube.start(32.f);
		
		
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
	}
}
