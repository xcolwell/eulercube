package org.resisttheb.x.eulercube;
import java.util.Collection;


public interface Cube {
	public static abstract class AbstractCube implements Cube {
		protected final CubePoint point0;
		protected final CubePoint point1;
		
		public AbstractCube(final CubePoint _point0, final CubePoint _point1) {
			this.point0 = _point0;
			this.point1 = _point1;
		}
		
		/**************************
		 * CUBE IMPLEMENTATION
		 **************************/
		
		public CubePoint getPoint0() {
			return point0;
		}
		
		public CubePoint getPoint1() {
			return point1;
		}
		
		/**************************
		 * END CUBE IMPLEMENTATION
		 **************************/
	}
	
	
	public CubePoint getPoint0();
	public CubePoint getPoint1();
	
	public int nodeCount();
	public int edgeCount();
	
	public void getNodes(final Collection<Node> nodes);
	public void getEdges(final Collection<Edge> edges);
	
	public Node findClosestNode(final CubePoint point);
	public Edge findClosestMidpoint(final CubePoint point);
	
	public Cube subdivide(final int iterations);
	public int getDivisions();
}
