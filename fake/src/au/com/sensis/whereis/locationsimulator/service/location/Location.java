package au.com.sensis.whereis.locationsimulator.service.location;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to represent an android.location.Location, for setting to the LocationManager via the Test provider api.
 * The lat and lon fields are mandatory and the remaining ones are optional.  In particular, the time member
 * may have different meanings depending on the context in which this class is used.
 */
public class Location {
	public static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	private double lat;
	private double lon;
	private long time;
	private float accuracy;
	private double altitude;
	private float bearing;
	private float speed;
	private boolean hasTime;
	private boolean hasAccuracy;
	private boolean hasAltitude;
	private boolean hasBearing;
	private boolean hasSpeed;
	
//	public static DateFormat getTimeFormat() {
//		return TIME_FORMAT;
//	}

	public Location(double lat, double lon, long time) {
		this.lat = lat;
		this.lon = lon;
		this.time = time;
		hasTime = true;
	}
	
	private Location() {}
	
	/**
	 * Expected syntax: { 
	 * 		lat:"60.189165752381086", 
	 * 		lon:"14.027822222560644", 
	 * 		time:"2012-09-06T12:36:22Z",
	 * 		accuracy:"0.0",
	 * 		altitude:"100",
	 * 		bearing:"12.34",
	 * 		speed:"60.0"
	 * }
	 * Only lat and lon are mandatory.
	 */
	public static Location parse(String source) throws Exception {
		JSONObject jsonObj = new JSONObject(source);
		return parse(jsonObj);
	}
	
	public static Location parse(JSONObject jsonObj) throws Exception {
		Location location = new Location();
		location.lat = jsonObj.getDouble("lat");
		location.lon = jsonObj.getDouble("lon");
		if (jsonObj.has("time")) {
			location.time = TIME_FORMAT.parse(jsonObj.getString("time")).getTime();
			location.hasTime = true;
		}
		if (jsonObj.has("accuracy")) {
			location.accuracy = (float) jsonObj.getDouble("accuracy");
			location.hasAccuracy = true;
		}
		if (jsonObj.has("altitude")) {
			location.altitude = jsonObj.getDouble("altitude");
			location.hasAltitude = true;
		}
		if (jsonObj.has("bearing")) {
			location.bearing = (float) jsonObj.getDouble("bearing");
			location.hasBearing = true;
		}
		if (jsonObj.has("speed")) {
			location.speed = (float) jsonObj.getDouble("speed");
			location.hasSpeed = true;
		}
		return location;
	}
	
	/**
	 * Reset time from time-since-the-epoch to relative time from the datum.
	 * This method makes no check that hasTime is true, and also there is no 
	 * check here to prevent zero or negatative time-since-previous values.
	 */
	public void adjustTimeToRelative(long datum) {
		time = time - datum;
	}
	
//	/**
//	 * Reset time from time-since-the-epoch to an interval-since-the-previous.
//	 * The original time setting is returned, to assist with the traversal.
//	 * This method makes no check that hasTime is true, and also there is no 
//	 * check here to prevent zero or negatative time-since-previous values.
//	 */
//	public long adjustTimeToInterval(long previous) {
//		long originalTime = time;
//		time = time - previous;
//		return originalTime;
//	}
	
	public String toString() {
		StringBuffer text = new StringBuffer("{ lat:" + lat + ", lon:" + lon);
		if (hasTime) {
			text.append(", time:");
			// Assume that if time is less than a day, then it must be an
			// interval rather than an absolute time, and format accordingly.
			if (time < 1000*60*60*24)
				text.append(time);
			else 
				text.append(getTimeFormatted());
		}
		if (hasAccuracy)
			text.append(", accuracy:").append(accuracy);
		if (hasAltitude)
			text.append(", altitude:").append(altitude);
		if (hasBearing)
			text.append(", bearing:").append(bearing);
		if (hasSpeed)
			text.append(", speed:").append(speed);
		return text.append(" }").toString();
	}

	public double getLat() {
		return lat;
	}
	public double getLon() {
		return lon;
	}
	
	public boolean hasTime() {
		return hasTime;
	}
	public boolean hasAccuracy() {
		return hasAccuracy;
	}
	public boolean hasAltitude() {
		return hasAltitude;
	}
	public boolean hasBearing() {
		return hasBearing;
	}
	public boolean hasSpeed() {
		return hasSpeed;
	}

	/**
	 * This value will be either time-since-the-epoch or interval-since-the-previous
	 * (if adjustTimeToInterval() has been executed).  Note that the value will be
	 * zero, if hasTime() is false.
	 */
	public long getTime() {
		return time;
	}
	/** Assumes a value value of the time member. */
	public String getTimeFormatted() {
		return TIME_FORMAT.format(new Date(time));
	}
	
	public float getAccuracy() {
		return accuracy;
	}
	public double getAltitude() {
		return altitude;
	}
	public float getBearing() {
		return bearing;
	}
	public float getSpeed() {
		return speed;
	}

	public void setAccuracy(float accuracy) {
		this.hasAccuracy = true;
		this.accuracy = accuracy;
	}
	public void setAltitude(double altitude) {
		this.hasAltitude = true;
		this.altitude = altitude;
	}
	public void setBearing(float bearing) {
		this.hasBearing = true;
		this.bearing = bearing;
	}
	public void setSpeed(float speed) {
		this.hasSpeed = true;
		this.speed = speed;
	}
	
	/**
	 * Returns distance in metres from this location to that location, or zero if that is null.
	 */
    public float distanceInMetres(Location that) {
    	if (that == null)
    		return 0;
    	float[] results = new float[1];
    	android.location.Location.distanceBetween(this.getLat(), this.getLon(), that.getLat(), that.getLon(), results);
    	return results[0];
    }
    
    /**
     * Returns the initial bearing (also called forward azimuth) (in radians) 
     * from this to that.  Ref http://www.movable-type.co.uk/scripts/latlong.html
     * and http://www.ig.utexas.edu/outreach/googleearth/latlong.html
     * Also sets the bearing value in this object.
     */
    public double setBearingTo(Location that) {
    	double dLon = that.getLon() - this.getLon();
    	double lat1 = this.getLat();
    	double lat2 = that.getLat();
    	
    	double y = Math.sin(dLon) * Math.cos(lat2);
    	double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
    	double brng = Math.atan2(y, x);			// in the range -pi ... +pi
    	brng = brng * 180 / Math.PI;			// to degrees
    	brng = (brng + 360) % 360;				// Compass bearing East of North.
    	
		this.bearing = (float) brng;
		this.hasBearing = true;
    	
    	return brng;
    }

    public static Location makeLocation(android.location.Location androidLocation) {
    	Location location = new Location();
    	location.lat = androidLocation.getLatitude();
    	location.lon = androidLocation.getLongitude();
    	// It seems that androidLocation.getTime() doesn't necessarily return a changing time. 
    	location.time = (new Date().getTime());
    	
    	if (androidLocation.hasAccuracy()) {
    		location.accuracy = androidLocation.getAccuracy();
			location.hasAccuracy = true;
    	}
    	if (androidLocation.hasAltitude()) {
    		location.altitude = androidLocation.getAltitude();
			location.hasAltitude = true;
    	}
    	if (androidLocation.hasBearing()) {
    		location.bearing = androidLocation.getBearing();
			location.hasBearing = true;
    	}
    	if (androidLocation.hasSpeed()) {
    		location.speed = androidLocation.getSpeed();
			location.hasSpeed = true;
    	}
    	return location;
    }
}
