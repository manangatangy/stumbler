package au.com.sensis.whereis.fake.bridge.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

import com.google.gson.Gson;

public class AppendRouteFile implements Executor {

	/**
	 * Input request: { "cmd":"appendRouteFile", "fileName":"someFileName.json", 
	 *  				'writeOption':'append/overwrite/failIfExisting'
	 *                  "lon":"147865", "lat": "234567", "speed":"30", "noGPS":"true/false",
	 *                  "text":"2 Cross St, Footscray, VIC 3011" }
	 * Return result:  "someFileName.json appended ok"
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
			Map<String, String> fields = new HashMap<String, String>();
			
			for (String key : new String[]{ "text", "speed", "lat", "lon", "noGPS" }) {
				fields.put(key, parms.get(key));
			}
			
			Gson gson = new Gson();
			String json = gson.toJson(fields);
			writer.write(json);
			writer.newLine();
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
