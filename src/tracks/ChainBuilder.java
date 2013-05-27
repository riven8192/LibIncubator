package tracks;

public class ChainBuilder {
	public static TrackUnitPair create(Track track, int... lengths) {
		TrackUnit prevUnit = new TrackUnit(track);
		TrackUnitPair firstConn = null, prevConn = null;

		for (int length : lengths) {
			TrackUnit currUnit = new TrackUnit(track);
			TrackUnitPair currConn = new TrackUnitPair(prevUnit, currUnit, length);
			if (prevConn != null) {
				prevConn.next = currConn;
			}
			if (firstConn == null) {
				firstConn = currConn;
			}
			prevUnit = currUnit;
			prevConn = currConn;
		}

		return firstConn;
	}
}
