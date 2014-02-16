package org.resisttheb.x.eulercube;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;


public class InteractiveCube implements GLEventListener {
	private static final boolean SHOW_TRAILS 	= true;
	private static final float TRAIL_ALPHA 		= 0.35f;
	
	
	/**************************
	 * INTERNAL STRUCTURES
	 **************************/
	
	private static final class NodeEntry {
		public final Node node;
		public final int index;
		public final float[] coords;
		public final EdgeEntry[] edges;
		public Object rdata = null;
		
		public NodeEntry(final int _index, final Node _node) {
			this.index 	= _index;
			this.node 	= _node;
			coords 		= node.getPoint().coords();
			edges 		= new EdgeEntry[node.getDegree()];
		}
	}
	
	private static final class EdgeEntry {
		public final Edge edge;
		public final int index;
		public final int index0;
		public final int index1;
		public boolean render = true;
		public Object rdata = null;
		
		public EdgeEntry(final int _index, final Edge _edge, 
				final int _index0, final int _index1
		) {
			this.index 		= _index;
			this.edge 		= _edge;
			this.index0 	= _index0;
			this.index1 	= _index1;
		}
	}
	
	/**************************
	 * END INTERNAL STRUCTURES
	 **************************/
	
	
	private int width = 0;
	private int height = 0;
	
	private GLAutoDrawable glad = null;
	private GLU glu;
	
	
	private final Cube cube;
	private final NodeEntry[] nodes;
	private final EdgeEntry[] edges;
	
	private final float[][] vcoords;
	
	private final float[] zbuffer;
	private final int[] indexBuffer;
	private final ParallelListSorter pls;
	
	
	private final float[] cameraOrientation 		= {0, 0, 1};
	private final float[] targetCameraOrientation 	= {0, 0, 1};
	private float minCameraStepDistance 			= 0.001f;
	private float cameraStepFraction 				= 0.04f;
	private float cameraStepDistance				= 1.f / 1000.f;
	private float cameraSteps						= 30.f;
	
	private final float[] center 			= {0, 0, 0};
	private float scale 					= 1.f;
	
	private Perspective perspective 		= null;
	private NodeRenderer nodeRenderer 		= null;
	private EdgeRenderer edgeRenderer 		= null;
	
	private Timer advanceTimer 				= null;
	
	
	
	
	public InteractiveCube(final Cube _cube) {
		this.cube 		= _cube;
		nodes 			= new NodeEntry[cube.nodeCount()];
		edges 			= new EdgeEntry[cube.edgeCount()];
		
		final int N 	= nodes.length;
		vcoords 		= new float[N][3];
		zbuffer 		= new float[N];
		indexBuffer 	= new int[N];
		pls 			= new ParallelListSorter(N);
		
		// Initialize nodes and edges:
		final Map<Node, NodeEntry> cnodeToNodeMap = new HashMap<Node, NodeEntry>(N);
		final Map<Edge, EdgeEntry> cedgeToEdgeMap = new HashMap<Edge, EdgeEntry>(N);
		
		final List<Node> cnodes = new ArrayList<Node>(N);
		cube.getNodes(cnodes);
		
		for (int i = 0; i < N; i++) {
			final Node cnode = cnodes.get(i);
			final NodeEntry node = new NodeEntry(i, cnode);
			nodes[i] = node;
			cnodeToNodeMap.put(cnode, node);
		}
		
		int ci = 0;
		for (NodeEntry node : nodes) {
			final Edge[] cedges = node.node.getEdges();
			for (int i = 0; i < cedges.length; i++) {
				final Edge cedge = cedges[i];
				EdgeEntry edge = cedgeToEdgeMap.get(cedge);
				if (null == edge) {
					final Node cnode0 = cedge.getEndpointNodes()[0];
					final Node cnode1 = cedge.getEndpointNodes()[1];
					final NodeEntry node0 = cnodeToNodeMap.get(cnode0);
					final NodeEntry node1 = cnodeToNodeMap.get(cnode1);
					
					final int index = ci++;
					edge = new EdgeEntry(index, cedge, node0.index, node1.index);
					edges[index] = edge;
					
					cedgeToEdgeMap.put(cedge, edge);
				}
				node.edges[i] = edge;
			}
		}
		assert ci == edges.length;
		
		
		// INIT VIEW:
		setPerspective(new Perspective.ExponentialPerspective(1.0032f));
		
		setNodeRenderer(new NodeRenderer.Simple(new Color(64, 64, 64, 220)));
		setEdgeRenderer(new EdgeRenderer.Simple(new Color(192, 192, 192, 170)));
		
		final CubePoint point0 = cube.getPoint0();
		final CubePoint point1 = cube.getPoint1();
		setCenter(
				(point1.x - point0.x) / 2.f, 
				(point1.y - point0.y) / 2.f, 
				(point1.z - point0.z) / 2.f
		);
	}
	
	
	public Cube getCube() {
		return cube;
	}
	

	public void bind(final GLAutoDrawable _glad) {
		this.glad = _glad;
		
		init0NodeRenderer();
		init0EdgeRenderer();
	}
	
	private void init0NodeRenderer() {
		if (null == glad) return;
		if (null == nodeRenderer) return;
		
		final GL gl = glad.getGL();
		nodeRenderer.init0(gl, glu);
	}
	
	private void init0EdgeRenderer() {
		if (null == glad) return;
		if (null == edgeRenderer) return;
		
		final GL gl = glad.getGL();
		edgeRenderer.init0(gl, glu);
	}
	
	
	// the camera chases this point:
	public void setCameraOrientation(final float[] _targetCameraOrientation, final boolean chase) {
		VectorUtilities.norm(_targetCameraOrientation, targetCameraOrientation);
		if (0 < cameraSteps) {
			final float[] d = new float[3];
			VectorUtilities.diff(targetCameraOrientation, cameraOrientation, d);
			final float m = VectorUtilities.mag(d);
			cameraStepDistance = m / cameraSteps;
		}
		if (! chase) {
			// Immediately catch up:
			System.arraycopy(targetCameraOrientation, 0, cameraOrientation, 0, 3);	
		}
	}
	
	public void setMinCameraStepDistance(final float _minCameraStepDistance) {
		this.minCameraStepDistance = _minCameraStepDistance;
	}
	
	public void setCameraStepFraction(final float _cameraStepFraction) {
		this.cameraStepFraction = _cameraStepFraction;
	}
	
	public void setCameraStepDistance(final float _cameraStepDistance) {
		this.cameraStepDistance = _cameraStepDistance;
	}
	
	public void setCameraSteps(final float _cameraSteps) {
		this.cameraSteps = _cameraSteps;
	}
	
	public void setScale(final float _scale) {
		this.scale = _scale;
	}
	
	public void setCenter(final float x, final float y, final float z) {
		center[0] = x;
		center[1] = y;
		center[2] = z;
	}
	
	public void setPerspective(final Perspective _perspective) {
		this.perspective = _perspective;
	}
	
	public void setNodeRenderer(final NodeRenderer _nodeRenderer) {
		if (null == _nodeRenderer) {
			throw new IllegalArgumentException();
		}
		this.nodeRenderer = _nodeRenderer;
		for (NodeEntry node : nodes) {
			node.rdata = nodeRenderer.createRenderData(node.node);
		}
		
		init0NodeRenderer();
	}
	
	public void setEdgeRenderer(final EdgeRenderer _edgeRenderer) {
		if (null == _edgeRenderer) {
			throw new IllegalArgumentException();
		}
		this.edgeRenderer = _edgeRenderer;
		for (EdgeEntry edge : edges) {
			edge.rdata = edgeRenderer.createRenderData(edge.edge);
		}
		
		init0EdgeRenderer();
	}
	
	
	
	/**************************
	 * ADVANCE
	 **************************/
	
	public void start(final float fps) {
		if (null == advanceTimer) {
			advanceTimer = new Timer(Math.round(1000 / fps),
			new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					advance();
				}
			});
		}
		advanceTimer.restart();
	}
	
	public void stop() {
		advanceTimer.stop();
		advanceTimer = null;
	}
	
	
	
	private void advance() {
		// Catch up camera:
		final float[] d = new float[3];
		VectorUtilities.diff(targetCameraOrientation, cameraOrientation, d);
		final float m = VectorUtilities.mag(d);
		if (0 < m) {
			VectorUtilities.norm(d, d);
			final float sm = 0 < cameraStepDistance 
					? Math.min(m, cameraStepDistance) 
					: (m * cameraStepFraction);
			if (sm <= minCameraStepDistance || m <= sm) {
				VectorUtilities.copy(targetCameraOrientation, cameraOrientation);
			}
			else {
				cameraOrientation[0] += d[0] * sm;
				cameraOrientation[1] += d[1] * sm;
				cameraOrientation[2] += d[2] * sm;
				final float E = 0.01f;
				if (Math.abs(cameraOrientation[0]) < E && 
					Math.abs(cameraOrientation[1]) < E && 
					Math.abs(cameraOrientation[2]) < E
				) {
					cameraOrientation[0] = (0.f == cameraOrientation[0] 
					    ? 1.f : Math.signum(cameraOrientation[0])) * 
						minCameraStepDistance;
					cameraOrientation[1] = (0.f == cameraOrientation[1] 
 					    ? 1.f : Math.signum(cameraOrientation[1])) * 
 						minCameraStepDistance;
					cameraOrientation[2] = (0.f == cameraOrientation[2] 
					    ? 1.f : Math.signum(cameraOrientation[2])) * 
						minCameraStepDistance;
				}
				VectorUtilities.norm(cameraOrientation, cameraOrientation);
			}
		}
		
		if (null != glad) {
			glad.display();
		}
	}
	

	/**************************
	 * END ADVANCE
	 **************************/
	
	
	private float[] createViewMatrix() {
		// we need to rotate the model orientation (0,0,1)
		// to the camera orientation
		
		// if MO || CO, then use the x-axis
		// else
		//     axis of rotation is MO cross CO
		//
		
		// 1 + (1-cos(angle))*(x*x-1)
		// -z*sin(angle)+(1-cos(angle))*x*y
		// y*sin(angle)+(1-cos(angle))*x*z
		
		// z*sin(angle)+(1-cos(angle))*x*y
		//  	1 + (1-cos(angle))*(y*y-1)
		//  	-x*sin(angle)+(1-cos(angle))*y*z
		
		// -y*sin(angle)+(1-cos(angle))*x*z
		//  	x*sin(angle)+(1-cos(angle))*y*z
		//  	1 + (1-cos(angle))*(z*z-1)
		
		final float[] modelOrientation = {0, 0, 1};
		final float angle = -VectorUtilities.angle(modelOrientation, cameraOrientation);
		final float[] axis = new float[3];
		VectorUtilities.cross(modelOrientation, cameraOrientation, axis);
		if (0.f == VectorUtilities.mag(axis)) {
			axis[0] = 1;
			axis[1] = 0;
			axis[2] = 0;
		}
		VectorUtilities.norm(axis, axis);
		
		final float cos = (float) Math.cos(angle);
		final float sin = (float) Math.sin(angle);
		final float x = axis[0];
		final float y = axis[1];
		final float z = axis[2];
		
		return new float[]{
			1 + (1-cos)*(x*x-1),
			-z*sin+(1-cos)*x*y,
			y*sin+(1-cos)*x*z,
			
			z*sin+(1-cos)*x*y,
			1 + (1-cos)*(y*y-1),
			-x*sin+(1-cos)*y*z,
			
			-y*sin+(1-cos)*x*z,
			x*sin+(1-cos)*y*z,
			1 + (1-cos)*(z*z-1)
		};
	}
	
	private void render(final GL gl) {
		final int N = nodes.length;
		final float[] CENTER = {0, 0, 0};
		
		final float[] viewMatrix = createViewMatrix();
		
		for (int i = 0; i < N; i++) {
			final float[] outCoords = vcoords[i];
			final float[] coords = nodes[i].coords;
			
			outCoords[0] =  scale * ( 
				viewMatrix[0] * (coords[0] - center[0]) +
				viewMatrix[1] * (coords[1] - center[1]) +
				viewMatrix[2] * (coords[2] - center[2])
			);
			outCoords[1] =  scale * (
				viewMatrix[3] * (coords[0] - center[0]) +
				viewMatrix[4] * (coords[1] - center[1]) +
				viewMatrix[5] * (coords[2] - center[2])
			);
			outCoords[2] = scale * (
				viewMatrix[6] * (coords[0] - center[0]) +
				viewMatrix[7] * (coords[1] - center[1]) +
				viewMatrix[8] * (coords[2] - center[2])
			);
			
			zbuffer[i] = vcoords[i][2];
			indexBuffer[i] = i;
		}
		
		pls.sort(zbuffer, indexBuffer, N);
		
		for (int i = 0; i < N; i++) {
			perspective.apply(vcoords[i], CENTER, vcoords[i]);
		}
		
		edgeRenderer.init(gl, glu, zbuffer[0], zbuffer[N - 1]);
		nodeRenderer.init(gl, glu, zbuffer[0], zbuffer[N - 1]);
		
//		gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		for (int i = 0; i < N; i++) {
			final int index = indexBuffer[i];
			final NodeEntry node = nodes[index];
			
			// Render edges:
			for (EdgeEntry edge : node.edges) {
				// Render on the second encounter:
				edge.render = !edge.render;
				if (! edge.render) {
					final int index0 = edge.index0 != index ? edge.index0 : edge.index1;
					edgeRenderer.render(edge.edge, edge.rdata,
							// Lower coords:
							vcoords[index0],
							// Upper coords:
							vcoords[index]
//							nodes[index0].rdata,
//							nodes[index].rdata
					);
				}
			}
			
			// Render nodes:
			nodeRenderer.render(node.node, node.rdata,
					vcoords[index]
			);
		}
		
		edgeRenderer.uninit();
		nodeRenderer.uninit();
		
		
		// 1. populate vcoords (perform rotations)
		// 2. map 3d point to 2d, using perspective transform -- modify vcoords
		// 3. populate zbuffer, index for nodes, sort
		// 
		// 
		
		
		// draw:
		// 
		// read node index, back to front
		// for each node, for each attached edge, toggle edge render bit,
		// if edge render but is LOW, render edge
		// render node
		
		
	}
	
	
	
	/**************************
	 * GLEVENTLISTENER IMPLEMENTATION
	 **************************/

	@Override
	public void init(final GLAutoDrawable drawable) {		
		GL gl = drawable.getGL();
        glu = new GLU();
//        gl.glClearColor(1.f, 1.f, 1.f, 1.f);
        gl.glClearColor(0.f, 0.f, 0.f, 1.f);
      
        // We manage our own depth, so disable this test (big perf boost):
        gl.glDisable(GL.GL_DEPTH_TEST);
        // Disable v-sync if we can:
	    gl.setSwapInterval(0);
        
	    gl.glEnable(GL.GL_BLEND);
	    
        bind(drawable);
	}
	
	@Override
	public void reshape(final GLAutoDrawable drawable, 
			final int x, final int y, final int _width, final int _height
	) {
		this.width = _width;
		this.height = _height;
		
		GL gl = drawable.getGL();

		gl.glViewport(x, y, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, (double) width, 0.0, (double) height);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(width / 2.f, height / 2.f, 0.f);
	}
	
	@Override
	public void display(final GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
        
		if (SHOW_TRAILS) {
			final float hw = width / 2.f;
			final float hh = height / 2.f;
			
//			gl.glColor4f(1.f, 1.f, 1.f, TRAIL_ALPHA);
			gl.glColor4f(0.f, 0.f, 0.f, TRAIL_ALPHA);
			gl.glBegin(GL.GL_QUADS);
				gl.glVertex2f(-hw, -hh);
				gl.glVertex2f(hw, -hh);
				gl.glVertex2f(hw, hh);
				gl.glVertex2f(-hw, hh);
			gl.glEnd();
		}
		else {
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		}
		render(gl);
	}
	
	@Override
	public void displayChanged(final GLAutoDrawable drawable, 
			final boolean modeChanged, final boolean deviceChanged
	) {
		// Do nothing
	}
	
	/**************************
	 * END GLEVENTLISTENER IMPLEMENTATION
	 **************************/
	
	
//	/**************************
//	 * JCOMPONENT OVERRIDES
//	 **************************/
//	
//	@Override
//	protected void paintComponent(final Graphics g) {
//		super.paintComponent(g);
//		
//		final Graphics2D g2d = (Graphics2D) g;
//		
//		final int w = getWidth();
//		final int h = getHeight();
//		
//
//		g2d.setPaint(Color.WHITE);
//		g2d.fillRect(0, 0, w, h);
//		
//		final AffineTransform tat = g2d.getTransform();
//		final AffineTransform at = new AffineTransform(tat);
//		at.translate(w / 2.f, h / 2.f);
//		g2d.setTransform(at);
//		
//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
//		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
//		
//		render(g2d);
//		
//		g2d.setTransform(tat);
//	}
//	
//	/**************************
//	 * END JCOMPONENT OVERRIDES
//	 **************************/
	
	
	
	
	public static void main(final String[] in) {
		Cube cube = AtomicCube.atom(
				new CubePoint(0, 0, 0), 
				new CubePoint(1, 1, 1)
			);
		cube = cube.subdivide(2);
		cube = new AgglomerateCube(cube);
		final Cube _cube = cube;
		
		System.out.println(Euler.isEuler(cube));
		
		/*
		final StringBuffer buffer = new StringBuffer(8 * 1024);
		final ArrayList<Node> nodes = new ArrayList<Node>(cube.nodeCount());
		cube.getNodes(nodes);
		for (Node node : nodes) {
			buffer.append(node.toString()).append("\n");
		}
		try {
			final FileWriter w = new FileWriter(new File("c:/temp/node_dump.txt"));
			w.write(buffer.toString());
			w.flush();
			w.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
		final InteractiveCube icube = new InteractiveCube(cube);
		
		/*
		icube.setEdgeRenderer(new EdgeRenderer.AbstractEdgeRenderer() {
			@Override
			public void render(Edge edge, Object rdata, float[] r0, float[] r1) {
				if (! CubePointTuple.isFace(_cube, edge.getEndpoints()))
					return;
				
				g2d.setPaint(new Color(64, 64, 64, 64));
				g2d.draw(new Line2D.Float(
						new Point2D.Float(r0[0], r0[1]), 
						new Point2D.Float(r1[0], r1[1])
					));
			}
		});
		
		icube.setNodeRenderer(new NodeRenderer.AbstractNodeRenderer() {
			@Override
			public void render(Node node, Object rdata, float[] r) {
				if (! CubePoint.isOuter(_cube, node.getPoint()))
					return;
				
				g2d.setPaint(Color.LIGHT_GRAY);
				g2d.fill(new Ellipse2D.Float(r[0] - 3.f, r[1] - 3.f, 6.f, 6.f));
			}
		});
		*/
		
		final CubePoint point0 = cube.getPoint0();
		final CubePoint point1 = cube.getPoint1();
		icube.setNodeRenderer(new NodeRenderer.AbstractNodeRenderer() {
			@Override
			public void render(final Node node, final Object rdata, final float[] r) {
				final CubePoint point = node.getPoint();
				
				final float fr = 0.5f + 0.5f * (point.x - point0.x) / (point1.x - point0.x);
				final float fg = 0.5f + 0.5f * (point.y - point0.y) / (point1.y - point0.y);
				final float fb = 0.5f + 0.5f * (point.z - point0.z) / (point1.z - point0.z);
				
				final float fa = 0.55f;
				
				gl.glColor4f(fr, fg, fb, (r[2] - zmin) / (zmax - zmin) * fa);
				gl.glPointSize(1.f + 6.f * (r[2] - zmin) / (zmax - zmin));
				gl.glBegin(GL.GL_POINTS);
					gl.glVertex2f(r[0], r[1]);
				gl.glEnd();
			}
		});
		icube.setEdgeRenderer(new EdgeRenderer.AbstractEdgeRenderer() {
			@Override
			public void render(final Edge edge, final Object rdata, final float[] r0, final float[] r1) {
				final CubePoint point = edge.getMidpoints()[0];
				
				final float fr = 0.6f + 0.4f * (point.x - point0.x) / (point1.x - point0.x);
				final float fg = 0.6f + 0.4f * (point.y - point0.y) / (point1.y - point0.y);
				final float fb = 0.6f + 0.4f * (point.z - point0.z) / (point1.z - point0.z);
				
				final float fa = 0.3f;
				
				final float zm = (r0[2] + r1[2]) / 2.f;
				gl.glColor4f(fr, fg, fb, (zm - zmin) / (zmax - zmin) * fa);
				gl.glPointSize(1.f + 6.f * (zm - zmin) / (zmax - zmin));
				gl.glBegin(GL.GL_LINES);
					gl.glVertex2f(r0[0], r0[1]);
					gl.glVertex2f(r1[0], r1[1]);
				gl.glEnd();
			}
		});
		
		final GLCanvas canvas = new GLCanvas();
		canvas.addGLEventListener(icube);
		
		final JFrame frame = new JFrame();
		final Container c = frame.getContentPane();
		
		c.setLayout(new BorderLayout());
		c.add(canvas, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);
		
		
		

		final Timer timer = new Timer(1000, 
		new AbstractAction() {
			private float t0 = 0.f;
			private float t1 = 0.f;
			
			
			public void actionPerformed(final ActionEvent e) {
				/*
				final float[] u = {
					(float) (0.5 - Math.random()),
					(float) (0.5 - Math.random()),
					(float) (0.5 - Math.random())
				};
				VectorUtilities.norm(u, u);
				icube.setCameraOrientation(u, true);
				*/
				final float r = 1.f;
				final float dt0 = 0.1f;
				final float dt1 = 0.3f;
				
				t0 += dt0;
				t1 += dt1;
				
				final float[] u = {
					r * (float) (Math.cos(t0) * Math.sin(t1)),
					r * (float) (Math.sin(t0) * Math.sin(t1)),
					r * (float) Math.cos(t1)
				};
				VectorUtilities.norm(u, u);
				icube.setCameraOrientation(u, true);
			}
		});
		timer.start();

		
		final MouseInputListener mil = new MouseInputAdapter() {
			private float[] target0;
			private Point point0;
			
			@Override
			public void mousePressed(MouseEvent e) {
				point0 = e.getPoint();
				target0 = new float[]{
						icube.targetCameraOrientation[0],
						icube.targetCameraOrientation[1],
						icube.targetCameraOrientation[2]
				};
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				final Point point = e.getPoint();
				final float dx = point.x - point0.x;
				final float dy = point.y - point0.y;
				final float a = 1.f / 100;
				icube.setCameraOrientation(new float[]{
						target0[0],
						target0[1] + dy * a,
						target0[2] + dx * a
				}, false);
			}
		};
		
		
//		canvas.addMouseListener(mil);
//		canvas.addMouseMotionListener(mil);
		
		icube.start(24.f);
		
		icube.setCenter(
				(point1.x - point0.x) / 2.f, 
				(point1.y - point0.y) / 2.f, 
				(point1.z - point0.z) / 2.f
		);
		icube.setScale(30.f);
		
		
	}



	
}
