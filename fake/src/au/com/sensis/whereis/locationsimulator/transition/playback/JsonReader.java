package au.com.sensis.whereis.locationsimulator.transition.playback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.location.Location;

public class JsonReader extends LocationReader {

	public JsonReader(File file, SimSettings simSettings) throws IOException {
		read(file, simSettings);
	}

	@Override
	protected List<Location> readLocations(FileInputStream fileInputStream) throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
		List<Location> locationList = new ArrayList<Location>();
		try {
			for (int lineNumber = 1; ; lineNumber++) {
				String line = reader.readLine();
				if (line == null)
					break;
				try {
					Location location = Location.parse(line);
					locationList.add(location);
				} catch (Exception exc) {
					throw new IOException("error parsing line# " + lineNumber + " : '" + line + "'" + exc.getMessage());
				}
			}
		} finally {
			try {
				reader.close();
			} catch (IOException ioeIgnored) {
			}
		}
		return locationList;
	}

}
