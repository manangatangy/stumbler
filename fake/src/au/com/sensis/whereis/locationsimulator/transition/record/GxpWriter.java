package au.com.sensis.whereis.locationsimulator.transition.record;

import java.io.File;
import java.io.IOException;

import au.com.sensis.whereis.locationsimulator.service.location.Location;

public class GxpWriter extends LocationWriter {

	private static final String prologue =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?> \n" +
		"<gpx \n" +
		"  xmlns=\"http://www.topografix.com/GPX/1/1\"  \n" +
		"  creator=\"FakeGPS\" version=\"0.1\"  \n" +
		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  \n" +
		"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1  \n" +
		"  http://www.topografix.com/GPX/1/1/gpx.xsd\"> \n" +
		" \n" +
//		"  <metadata> \n" +
//		"    <link href=\"http://www.garmin.com\"> \n" +
//		"      <text>Garmin International</text> \n" +
//		"    </link> \n" +
//		"    <time>2012-09-07T06:09:00Z</time> \n" +
//		"    <bounds maxlat=\"60.201388411223888\" maxlon=\"14.046252015978098\" minlat=\"60.188813209533691\" minlon=\"14.025767566636205\"/> \n" +
//		"  </metadata> \n" +
//		" \n" +
		"  <trk> \n" +
//		"    <name>ACTIVE LOG 004</name> \n" +
//		"    <extensions> \n" +
//		"      <gpxx:TrackExtension xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"> \n" +
//		"        <gpxx:DisplayColor>Transparent</gpxx:DisplayColor> \n" +
//		"      </gpxx:TrackExtension> \n" +
//		"    </extensions> \n" +
		"    <trkseg> \n";

	private static final String epilogue = 
		"    </trkseg> \n" +
		"  </trk> \n" +
		"</gpx> \n";
	
	
	public GxpWriter(File file) throws IOException {
		super(file);
		super.write(prologue);
	}

	@Override
	public void save(Location location) throws IOException {
		String trkpt =
			"      <trkpt lat=\"" + location.getLat() + "\" lon=\"" + location.getLon() + "\"> \n" +
			"        <time>" + location.getTimeFormatted() + "</time> \n" +
			"      </trkpt> \n";
		super.write(trkpt);
		
	}
	
	public void close() throws IOException {
		super.write(epilogue);
		super.close();
	}

}
