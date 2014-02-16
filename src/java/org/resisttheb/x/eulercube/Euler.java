package org.resisttheb.x.eulercube;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;



// walk the euler cycle for the given cube
public class Euler {
	/**************************
	 * UTILITIES
	 **************************/
	
	public static boolean isEuler(final Cube cube) {
		final List<Node> nodes = new ArrayList<Node>(32);
		cube.getNodes(nodes);
		for (Node node : nodes) {
			final int d = node.getDegree();
			if (0 != d % 2) {
				return false;
			}
		}
		return true;
	}
	
	/**************************
	 * END UTILITIES
	 **************************/

	
	// TODO:   walk that the camera follows. renderer highlights current edge
	
	
	public static void main(final String[] in) {
		
		// DETERMINISTIC EULER WALK
		// ALWAYS WALK TOWARDS THE CORNERS
		
		// TODO: need to create an agglomerate cube from a Cube
		
		Cube cube = AtomicCube.atom(
				new CubePoint(0, 0, 0), 
				new CubePoint(1, 1, 1)
			);

		cube = cube.subdivide(2);
		
		/*
		{
			final List<Node> nodes = new ArrayList<Node>(cube.nodeCount());
			cube.getNodes(nodes);
			final Set<CubePoint> pnodes = new HashSet<CubePoint>(4);
			for (Node node : nodes) {
				pnodes.add(node.getPoint());
			}
			System.out.println(pnodes.size());
		}
		*/
		
		// removed duplicate faces.
		// does an internal correction to maintain euler graph
		cube = new AgglomerateCube(cube);
		
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
		
		final Node startNode = cube.findClosestNode(new CubePoint(
				(point0.x + point1.x) / 2.f, 
				(point0.y + point1.y) / 2.f,
				(point0.z + point1.z) / 2.f
				));
		final List<Node> path = new LinkedList<Node>();
		circuit(startNode, new HashSet<Edge>(16), pdr, path);
		for (Node node : path) {
			System.out.println(node.getPoint());
		}
		
		
		// COLOR walk
		showColorWalk(point0, point1, path);
		renderColorWalk(new File("c:/temp/colorwalk.png"), point0, point1, path);
		
		
		CubeCutVis.show(cube, new float[]{0, 0, 9}, new float[]{1, 0, 0}, new float[]{0, 1, 0});
		
		
		
		final List<Node> nodes = new ArrayList<Node>(cube.nodeCount());
		final List<Edge> edges = new ArrayList<Edge>(cube.edgeCount());
		
		cube.getNodes(nodes);
		cube.getEdges(edges);
		
		System.out.println(nodes.size());
		System.out.println(edges.size());
		
		System.out.println(isEuler(cube));
		
		
		// TODO: cube point tuple
		
		// TODO: criss-cross eulerization algorithm:
		// 
		// (x=0/1, connect 0<y<1, 0<z<1 where y0=z0 and y1=z1 and floor[(y0-1)/2] = floor[(y1-1)/2])
		// OR y0+1=z0 and y1=z1+1 and same grouping as above
		
		// (0,1,1) <-> (0,2,2)
		// (0,3,3) <-> (0,4,4)
		
		// (0,1,2) <-> (0,2,1)
		// (0,3,4) <-> (0,4,3)
		
		// we just throw this edges in the stew at the end
		
		
	}
	
	
	
	public static interface EdgeRanker {
		public void sort(final Node current, final Edge[] edges);
	}
	
	public static class PrincipalDirectionRanker implements EdgeRanker {
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
				final Node opposite = opposite(current, edge);
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
	
	public static class NullRanker implements EdgeRanker {
		public NullRanker() {
		}
		
		/**************************
		 * EDGERANKER IMPLEMENTATION
		 **************************/
		
		public void sort(final Node current, final Edge[] edges) {
			// Do nothing
		}
		
		/**************************
		 * END EDGERANKER IMPLEMENTATION
		 **************************/
	}
	
	
	private static final class Frame {
		public static final Frame create(final Node node, final EdgeRanker ranker) {
			final Edge[] edges = node.getEdges();
			ranker.sort(node, edges);
			return new Frame(node, edges);
		}
		
		
		public int i = 0;
		public final Edge[] edges;
		public final Node node;
		
		public Frame(final Node _node, final Edge[] _edges) {
			this.node = _node;
			this.edges = _edges;
		}
	}
	
	
	// considers the first two endpoints;
	private static Node opposite(final Node node, final Edge edge) {
		for (Node enode : edge.getEndpointNodes()) {
			if (node != enode) return enode;
		}
		return null;
	}
	
	public static void circuit(
			final Node start, 
			final Set<Edge> explored, 
			final EdgeRanker ranker, 
			final List<Node> path
	) {
		final List<Frame> stack = new LinkedList<Frame>();
		
		stack.add(Frame.create(start, ranker));
		
		while (! stack.isEmpty()) {
			final Frame frame = stack.get(0);
			for (; stack.get(0) == frame && frame.i < frame.edges.length; frame.i++) {
				final Edge edge = frame.edges[frame.i];
				if (explored.contains(edge)) continue;
				explored.add(edge);
				stack.add(0, Frame.create(opposite(frame.node, edge), ranker));
			}
			if (stack.get(0) == frame) {
				path.add(0, frame.node);
				stack.remove(0);
			}
		}
	}

	
	
	
	
	
	
	private static void renderColorWalk(final File file, final CubePoint point0, final CubePoint point1, final List<Node> path) {
		final BufferedImage buffer = new BufferedImage(2000, 40, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = (Graphics2D) buffer.getGraphics();
		renderColorWalk(g2d, point0, point1, path);
		g2d.dispose();
		try {
			ImageIO.write(buffer, "png", file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void renderColorWalk(final Graphics2D g2d, final CubePoint point0, final CubePoint point1, final List<Node> path) {
		final float W = 0.25f;
		final float H = 40.f;
		
		int i = 0;
		for (Node node : path) {
			final CubePoint point = node.getPoint();
			
			final float ur = (point.x - point0.x) / (point1.x - point0.x);
			final float ug = (point.y - point0.y) / (point1.y - point0.y);
			final float ub = (point.z - point0.z) / (point1.z - point0.z);
			
			final Color color = new Color(ur, ug, ub);
			
			g2d.setPaint(color);
			final Shape shape = new Rectangle2D.Float(i * W, 0, W, H);
			g2d.fill(shape);
			i++;
		}
	}
	
	public static void showColorWalk(final CubePoint point0, final CubePoint point1, final List<Node> path) {
		
		
		final JComponent colorc = new JComponent() {
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				final AffineTransform tat = g2d.getTransform();
				final AffineTransform at = new AffineTransform(tat);
				at.translate(20, 20);
				g2d.setTransform(at);
				
				renderColorWalk(g2d, point0, point1, path);
				
				g2d.setTransform(tat);
			}
		};
		
		
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(colorc, BorderLayout.CENTER);
		
		frame.setSize(500, 500);
		frame.setVisible(true);
	}
}
