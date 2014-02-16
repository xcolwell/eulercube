package org.resisttheb.x.eulercube;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;


// 2d vis of a cut of a cube along some plane
// place is specified by two vectors
//
// e.g.
//  (1,0,0)  and  (0,1,1)
//
// indices are rounded to nearest int
// max(max(x1-x0/ux,...),max(x1-x0/vx, ...))
// as long as non zero
public class CubeCutVis {
	
	
	public static void show(final Cube cube, final float[] c, final float[] u, final float[] v) {
		final JComponent renderc = new JComponent() {
			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				final Graphics2D g2d = (Graphics2D) g;
				
				final AffineTransform tat = g2d.getTransform();
				try {
					final AffineTransform at = new AffineTransform(tat);
					at.translate(25, 25);
					g2d.setTransform(at);
					
					render(g2d, cube, c, u, v);
				}
				finally {
					g2d.setTransform(tat);
				}
			}
		};
		
		final JFrame frame = new JFrame();
		final Container cp = frame.getContentPane();
		cp.setLayout(new BorderLayout());
		
		cp.add(renderc, BorderLayout.CENTER);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(600, 600);
		frame.setVisible(true);
	}

	public static void render(final Graphics2D g2d, final Cube cube, final float[] c, final float[] u, final float[] v) {
		final CubePoint point0 = cube.getPoint0();
		final CubePoint point1 = cube.getPoint1();
		
		final int[] stepCountsu = new int[3];
		final int[] stepCountsv = new int[3];
		for (int i = 0; i < 3; i++) {
			final float d = point1.coords()[i] - point0.coords()[i];
			stepCountsu[i] = 0 == u[i] ? 0 
					: 1 + (int) Math.ceil(d / u[i]);
			stepCountsv[i] = 0 == v[i] ? 0 
					: 1 + (int) Math.ceil(d / v[i]);
		}
		
		final int stepCountu = Math.max(stepCountsu[0], 
				Math.max(stepCountsu[1], stepCountsu[2]));
		final int stepCountv = Math.max(stepCountsv[0], 
				Math.max(stepCountsv[1], stepCountsv[2]));
		
		final Map<CubePoint, int[]> planarCoordsMap = new HashMap<CubePoint, int[]>(stepCountu * stepCountv);
		
		final Set<Node> nodes = new HashSet<Node>(stepCountu * stepCountv);
		final Set<Edge> edges = new HashSet<Edge>(2 * stepCountu * stepCountv);
		for (int ui = 0; ui < stepCountu; ui++) {
			for (int vi = 0; vi < stepCountv; vi++) {
				final CubePoint point = new CubePoint(
					Math.round(c[0] + u[0] * ui + v[0] * vi),
					Math.round(c[1] + u[1] * ui + v[1] * vi),
					Math.round(c[2] + u[2] * ui + v[2] * vi)
				);
				final Node node = cube.findClosestNode(point);
				if (null == node) continue;
				
				nodes.add(node);
				
				planarCoordsMap.put(node.getPoint(), new int[]{ui, vi});
			}
		}
		
		// Populate edges to be within nodes:
		for (Node node : nodes) {
			for (Edge edge : node.getEdges()) {
				if (! nodes.containsAll(Arrays.asList(edge.getEndpointNodes()))) continue;
				
				edges.add(edge);
			}
		}
		
		// Render:
		final float SPACING = 20;
		final float R = 2;
		for (Edge edge : edges) {
			final CubePoint[] ends = edge.getEndpoints();
			
			final int[] planarCoords0 = planarCoordsMap.get(ends[0]);
			final float x0 = planarCoords0[0] * SPACING;
			final float y0 = planarCoords0[1] * SPACING;
			
			final int[] planarCoords1 = planarCoordsMap.get(ends[1]);
			final float x1 = planarCoords1[0] * SPACING;
			final float y1 = planarCoords1[1] * SPACING;
			
			final float r = R;
			final Shape line = new Line2D.Float(x0, y0, x1, y1);
			
			g2d.draw(line);
		}
		for (Node node : nodes) {
			final int[] planarCoords = planarCoordsMap.get(node.getPoint());
			final float cx = planarCoords[0] * SPACING;
			final float cy = planarCoords[1] * SPACING;
			
			final float r = R;
			final Shape dot = new Ellipse2D.Float(cx  - r, cy - r, 2 * r, 2 * r);
			
			g2d.fill(dot);
		}
	}
}
