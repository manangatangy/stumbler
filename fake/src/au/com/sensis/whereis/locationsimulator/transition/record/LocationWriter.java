package au.com.sensis.whereis.locationsimulator.transition.record;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.sensis.whereis.locationsimulator.service.location.Location;

public abstract class LocationWriter {

	private BufferedWriter writer;
	private Location previouslyLocation;		// Used to calculate distance travelled so far.
	private int recordCount = 0;
	private double distance = 0.0;
	
	public LocationWriter(File file) throws IOException {
		writer = new BufferedWriter(new FileWriter(file));
	}
	
	protected abstract void save(Location location) throws IOException;
		
	/** Convert from android to whereis location, pass it to save(), increment count and distance. */ 
	public void write(android.location.Location aLocation) throws IOException {
		Location location = Location.makeLocation(aLocation);
		save(location);
		recordCount++;
		distance += location.distanceInMetres(previouslyLocation);
		previouslyLocation = location;
	}
	
	protected void write(String text) throws IOException {
		writer.write(text);
	}
	
	protected void close() throws IOException {
		if (writer != null) {
			writer.close();
			writer = null;		// Protect from multiple closings.
		}
	}

	public void closeSilently() {
		if (writer != null) {
			try {
				close();
			} catch (IOException ioeIgnored) {
			}
			writer = null;		// Protect from multiple closings.
		}
	}
	
	public int getRecordCount() {
		return recordCount;
	}
	public double getDistance() {
		return distance;
	}
}
