package au.com.sensis.whereis.locationsimulator.transition.playback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.location.Location;

/**
 * Reads a GPX file, containing track-points with fields for lat, lon, and optionally time.
 * <trkpt lat="-37.81007404" lon="144.96260925">
 *     <time>2012-09-17T13:22:14Z</time> 
 * </trkpt> 
 * If there is no time tag in the element, then the corresponding Location will be created
 * with a time of now.
 */
public class GpxReader extends LocationReader {
	
	public GpxReader(File file, SimSettings simSettings) throws IOException {
		read(file, simSettings);
	}

	@Override
	public List<Location> readLocations(FileInputStream fileInputStream) throws IOException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();
			
			parser.parse(fileInputStream, new DefaultHandler() {
				private StringBuffer buf = new StringBuffer();
				
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					buf.setLength(0);
					start(qName, attributes);
				}
				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					end(buf.toString(), qName);
				}
				@Override
				public void characters(char[] chars, int start, int length) throws SAXException {
					buf.append(chars, start, length);
				}
			});
			return routeList;
		} catch (ParserConfigurationException pce) { 
			throw new IOException(pce.getMessage());
		} catch (SAXException saxe) {
			throw new IOException(saxe.getMessage());
		}
	}

	private List<Location> routeList = new ArrayList<Location>();

	private double lat;
	private double lon;
	private Date time;
	private double ele;			// May not be needed.
	private Date nowTime = new Date();

	public void start(String qName, Attributes attributes) {
		if (qName.equals("trkpt")) {
		    lat = Double.parseDouble(attributes.getValue("lat"));
		    lon = Double.parseDouble(attributes.getValue("lon"));
		}
	}

	public void end(String buf, String qName) throws SAXException {
		if (qName.equals("trkpt")) {
			if (lat == 0)
				throw new SAXException("Missing lat attribute in GPX input");
			if (lon == 0)
				throw new SAXException("Missing lon attribute in GPX input");
			routeList.add(new Location(lat, lon, ((time != null) ? time : nowTime).getTime()));
			lat = 0;
			lon = 0;
			time = null;
		} else if (qName.equals("ele")) {
			ele = Double.parseDouble(buf);
		} else if (qName.equals("time")) {
			try {
				time = Location.TIME_FORMAT.parse(buf);
			} catch (ParseException e) {
				throw new SAXException("Invalid time " + buf);
			}
		}
	}
	
}
