package au.com.sensis.whereis.fake.bridge.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

import com.google.gson.Gson;

public class ReadFileContents implements Executor {

	/**
	 * Input request: { "cmd":"readFileContents", "fileName":"some file name.json" }
	 * Return result:  { 'error':null, 'pointDataList' : {[ <pointData>]} }
	 * Where each pointData = {'text':'3 wakanui st northcote', 'speed':'30', 'lon':'34567', 'lat':'gsgsd', 'noGPS':'true/false'}
	 */
	@Override
	public FileReadResult execute(AdbClient adbClient, Map<String, String> parms) {

		FileReadResult fileReadResult = new FileReadResult();
		
		File dir = new File(parms.get("routeDir"));
		String fileName = parms.get("fileName");
		File file = new File(dir, fileName);
		FileInputStream fis = null;
		
		try {
			ArrayList<PointData> pointDatas = new ArrayList<PointData>();
			fis = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			for (int lineNumber = 1; ; lineNumber++) {
				String line = reader.readLine();
				if (line == null)
					break;
				try {
					Gson gson = new Gson();
					PointData pointData = gson.fromJson(line, PointData.class);
					pointDatas.add(pointData);
				} catch (Exception exc) {
					throw new IOException("error parsing line# " + lineNumber + " : '" + line + "'" + exc.getMessage());
				}
			}
			fileReadResult.pointDataList = new PointData[pointDatas.size()];
			for (int i = 0; i < pointDatas.size(); i++) {
				fileReadResult.pointDataList[i] = pointDatas.get(i);
			}
			fis.close();
			fis = null;
		} catch (Exception exc) {
			fileReadResult.error = "error: " + exc.getMessage();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception excIgnored) {
					excIgnored.printStackTrace();
				}
			}
		}
		return fileReadResult;
	}
}

class PointData {
	String text;
	double speed;
	double lon;
	double lat;
	boolean noGPS;
}

class FileReadResult {
	String error;		// Null if all good.
	PointData[] pointDataList;
}
