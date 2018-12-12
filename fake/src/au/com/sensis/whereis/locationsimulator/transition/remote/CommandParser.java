package au.com.sensis.whereis.locationsimulator.transition.remote;

import java.text.ParseException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.location.Location;

public class CommandParser {
	private static final String TAG = CommandParser.class.getName();
	
	private double lat;
	private double lon;
	private long timeInMillis;

	public double getLat() {
		return lat;
	}
	public double getLon() {
		return lon;
	}
	public long getTimeInMillis() {
		return timeInMillis;
	}

	/**
	 * Expected syntax: "{ lat:"60.189165752381086", lon:"14.027822222560644", time:"2012-09-06T12:36:22Z" }"
	 * Time tag is optional.  Returns true if lat and lon (at least) are provided.
	 */
	public boolean parseTrackpoint(String source) {
		
		try {
			JSONObject obj = new JSONObject(source);
			lat = obj.getDouble("lat");
			lon = obj.getDouble("lon");
			
			timeInMillis = 0;
			String time = null;
			try {
				time = obj.getString("time");
				timeInMillis = Location.TIME_FORMAT.parse(time).getTime();
			} catch (ParseException pe) {
		        Log.i(TAG, "parseTrackpoint: error parsing time in trackpoint (" + time + "): " + pe.getMessage());
			} catch(JSONException jsone) {
				// No "time" tag; use default of zero.
			}
		} catch (JSONException jsone) {
	        Log.i(TAG, "parseTrackpoint: error parsing trackpoint (" + source + "): " + jsone.getMessage());
	        return false;
		}
		
		return true;
	}
}
