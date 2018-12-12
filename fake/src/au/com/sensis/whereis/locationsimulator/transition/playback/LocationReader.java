package au.com.sensis.whereis.locationsimulator.transition.playback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.location.Location;

public abstract class LocationReader {

	private static final String TAG = LocationReader.class.getName();

	private FileInputStream fileInputStream;
	private Location[] locationsArray;
	private int recordCount = 0;
	private double distance = 0.0;
	
	/**
	 * Read the file for locations (closing the file when done).
	 * New locations may be interpolated between the as-read ones
	 * and the location times will be adjusted to relative to the first location.
	 * The locations will then be available from get(index), and
	 * the distance and recordCount values will also be available. 
	 */
	protected void read(File file, SimSettings simSettings) throws IOException {
		
		try {
			fileInputStream = new FileInputStream(file);
			List<Location> locationList = readLocations(fileInputStream);
			close();
			
		    if (simSettings.isPlaybackInterpolate())
		    	interpolate(locationList, simSettings.getPlaybackInterpolationPeriod());
		    
		    locationsArray = locationList.toArray(new Location[locationList.size()]);
		    adjustLocations(simSettings.isSynthesizePlaybackBearings());
		    
			recordCount = locationsArray.length;
	        Log.i(TAG, "read: distance = " + distance + ", recordCount = " + recordCount);
		} finally {
			closeSilently();
		}
	}

	public Location get(int index) {
		return locationsArray[index];
	}
	public int getRecordCount() {
		return recordCount;
	}
	public double getDistance() {
		return distance;
	}
	
	/** Read all the locations from the file and return as a list for further processing. */
	protected abstract List<Location> readLocations(FileInputStream fileInputStream) throws IOException;
	
	private void close() throws IOException {
		if (fileInputStream != null) {
			fileInputStream.close();
			fileInputStream = null;		// Protect from multiple closings.
		}
	}

	private void closeSilently() {
		if (fileInputStream != null) {
			try {
				close();
			} catch (IOException ioeIgnored) {
			}
			fileInputStream = null;		// Protect from multiple closings.
		}
	}

	private void interpolate(List<Location> locationList, int interpolationInterval) throws IOException {
	    if (interpolationInterval < 1000)
	    	throw new IOException("playback interpolation period too low " + interpolationInterval);
	    
	    Log.i(TAG, "interpolate: interpolating route using interval " + interpolationInterval);
	    // Start at the end and work back to the first entry as it eases the insertion logic.
	    int totalExtras = 0;
	    for (int i = locationList.size() - 1; i > 0; i--) {
	    	Location interpStart = locationList.get(i - 1);
	    	Location interpFinish = locationList.get(i);
	    	List<Location> interpExtra = interpolateBetween(interpStart, interpFinish, interpolationInterval);
	    	if (interpExtra == null || interpExtra.size() == 0)
	    		continue;
	    	// Insert these new locations, last one first.
	    	for (int interp = interpExtra.size() - 1; interp >= 0; interp--) {
	    		Location extra = interpExtra.get(interp);
	    		locationList.add(i, extra);
	    		totalExtras++;
	    	}
	    }
	    Log.i(TAG, "interpolate: interpolating has added " + totalExtras + " new Locations");
	}

	private List<Location> interpolateBetween(Location start, Location finish, int period) {
		long startTime = start.getTime();
		long finishTime = finish.getTime();
		long duration = finishTime - startTime;
		int newLocationsCount = (int)(duration / period) - 1;
		if (startTime == 0 || finishTime == 0 || newLocationsCount < 1)
			return null;

		List<Location> interps = new ArrayList<Location>();
        Log.i(TAG, "interpolateBetween: period=" + period + ", newCount=" + newLocationsCount);

		double factor = (double)duration / (double)period;
		double latInterval = (finish.getLat() - start.getLat()) / factor;
		double lonInterval = (finish.getLon() - start.getLon()) / factor;
		
		double lat = start.getLat();
		double lon = start.getLon();
		long time = start.getTime();
		
		for (int i = 0; i < newLocationsCount; i++) {
			lat += latInterval;
			lon += lonInterval;
			time += period;
			Location newLocation = new Location(lat, lon, time);
			if (start.hasAccuracy() && finish.hasAccuracy())
				newLocation.setAccuracy(start.getAccuracy());
			if (start.hasAltitude() && finish.hasAltitude())
				newLocation.setAltitude(start.getAltitude());
			if (start.hasBearing() && finish.hasBearing())
				newLocation.setBearing(start.getBearing());
			if (start.hasSpeed() && finish.hasSpeed())
				newLocation.setSpeed(start.getSpeed());
			interps.add(newLocation);
		}
		return interps;
	}
	
	/**
	 * Change the time member of the route Locations from absolute to relative time from the start.
	 * Calculate the total rout distance.   If bearings are required to be synthesized, then do so.
	 */
	private void adjustLocations(boolean doSynthesizePlaybackBearings) throws IOException {
		// Adjust the time values to be intervals, and calculate the total route distance.
		if (locationsArray.length < 1)
			throw new IOException("route has zero locations");
		distance = 0;
		Location previousLocation = locationsArray[0];

		long datumTime = locationsArray[0].getTime();
        Log.i(TAG, "adjustLocations: datumTime = " + datumTime);
		for (int i = 0; i < locationsArray.length; i++) {
			Location loc = locationsArray[i];
			loc.adjustTimeToRelative(datumTime);
			distance += previousLocation.distanceInMetres(loc);
			previousLocation = loc;
		}
        
        if (doSynthesizePlaybackBearings) {
        	int bearingsSetCount = 0;
        	for (int i = 0; i < locationsArray.length - 1; i++) {
        		if (!locationsArray[i].hasBearing()) {
        			locationsArray[i].setBearingTo(locationsArray[i + 1]);
        			bearingsSetCount++;
        		}
        	}
            Log.i(TAG, "adjustLocations: set bearing in " + bearingsSetCount + " locations");
        }
	}

}
