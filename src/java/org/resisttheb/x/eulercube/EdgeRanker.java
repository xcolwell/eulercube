package org.resisttheb.x.eulercube;


public interface EdgeRanker {
	public static class Null implements EdgeRanker {
		public Null() {
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
	
	
	public void sort(final Node current, final Edge[] edges);
}
