package au.com.sensis.whereis.fake.bridge.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import au.com.sensis.whereis.fake.bridge.AdbClient;

public class AppendGpxFile implements Executor {
	
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

	public static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static Date truncateDate;
	
	/**
	 * Input request: { "cmd":"appendGpxFile", "fileName":"someFileName.gpx", 
	 *  				'writeOption':'append/overwrite/failIfExisting'
	 *                  "lon":"147865", "lat": "234567", 
	 *                  "time":<milliseconds-elapsed> }
	 * Return result:  "someFileName.gpx appended ok"
	 */
	@Override
	public String execute(AdbClient adbClient, Map<String, String> parms) {
		File dir = new File(parms.get("routeDir"));
		String fileName = parms.get("fileName");
		String writeOption = parms.get("writeOption");
		File file = new File(dir, fileName);
		if ("failIfExisting".equals(writeOption) && file.exists()) {
			return "already exists";
		}
		BufferedWriter writer = null;
		try {
			boolean append = "append".equals(writeOption);
			writer = new BufferedWriter(new FileWriter(file, append));

			if (!append) {
				writer.write(prologue);
				writer.newLine();
			}
			
			String lat = parms.get("lat");
			String lon = parms.get("lon");
			String time = parms.get("time");	// Millis elapsed.
			long timeValue = Long.parseLong(time);
			if (!append || truncateDate == null) {
				truncateDate = new Date();		// Done once per file (at the first record).
			}
			String timeString = TIME_FORMAT.format(new Date(truncateDate.getTime() + timeValue));
			String trkpt =
				"      <trkpt lat=\"" + lat + "\" lon=\"" + lon + "\"> \n" +
				"        <time>" + timeString + "</time> \n" +
				"      </trkpt> \n";
			
			writer.write(trkpt);
			writer.newLine();
			
			if (!StringUtils.isEmpty(parms.get("writeEpilogue"))) {
				writer.write(epilogue);
				writer.newLine();
			}
			
			writer.close();
			return "ok";		// Means OK.
		} catch (Exception exc) {
			return "error: " + exc.getMessage();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception excIgnored) {
					excIgnored.printStackTrace();
				}
			}
		}
	}
}
