package org.resisttheb.x.eulercube;
import java.util.Comparator;
import java.util.Map;


public class MapComparator<A, B extends Comparable<B>> implements Comparator<A> {
	public static enum Direction {
		ASCENDING,
		DESCENDING
	}
	
	
	private final Direction dir;
	private final Map<A, B> map;
	
	public MapComparator(final Direction _dir, final Map<A, B> _map) {
		if (null == _map)
			throw new IllegalArgumentException();
		this.dir = _dir;
		this.map = _map;
	}
	
	/**************************
	 * COMPARATOR IMPLEMENTATION
	 **************************/
	
	public int compare(final A a0, final A a1) {
		final B b0 = map.get(a0);
		final B b1 = map.get(a1);
		if (null == b0 && null == b1)
			return 0;
		else if (null == b0)
			return -1;
		else if (null == b1)
			return 1;
		final int d = b0.compareTo(b1);
		return dir == Direction.ASCENDING ? d : -d;
	}
	
	/**************************
	 * END COMPARATOR IMPLEMENTATION
	 **************************/
}
