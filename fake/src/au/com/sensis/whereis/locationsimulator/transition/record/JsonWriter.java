package au.com.sensis.whereis.locationsimulator.transition.record;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.location.Location;

public class JsonWriter extends LocationWriter {
	
	private boolean recordAccuracy;
	private boolean recordAltitude;
	private boolean recordBearing;
	private boolean recordSpeed;

	public JsonWriter(File file, SimSettings simSettings) throws IOException {
		super(file);
		this.recordAccuracy = simSettings.isRecordAccuracy();
		this.recordAltitude = simSettings.isRecordAltitude();
		this.recordBearing = simSettings.isRecordBearing();
		this.recordSpeed = simSettings.isRecordSpeed();
	}

	@Override
	public void save(Location location) throws IOException {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("lat", location.getLat());
			jsonObj.put("lon", location.getLon());
			jsonObj.put("time", location.getTimeFormatted());
			if (recordAccuracy && location.hasAccuracy())
				jsonObj.put("accuracy", location.getAccuracy());
			if (recordAltitude && location.hasAltitude())
				jsonObj.put("altitude", location.getAltitude());
			if (recordBearing && location.hasBearing())
				jsonObj.put("bearing", location.getBearing());
			if (recordSpeed && location.hasSpeed())
				jsonObj.put("speed", location.getSpeed());
		} catch (JSONException jsone) {
			throw new IOException("error encoding location:" + location + ", " + jsone.getMessage());
		}
		// { lat:"60.189165752381086", lon:"14.027822222560644", time:"2012-09-06T12:36:22Z" }
		super.write(jsonObj.toString() + " \n");
	}
	
	public void close() throws IOException {
		super.close();
	}

}
