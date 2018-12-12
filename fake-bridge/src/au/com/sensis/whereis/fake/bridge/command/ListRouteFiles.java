package au.com.sensis.whereis.fake.bridge.command;

import java.io.File;
import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

public class ListRouteFiles implements Executor {

	/**
	 * Input request: { "cmd":"listRouteFiles" }
	 * Return result:  { ["file1.json", "file2.json", "file3.json"] }
	 */
	@Override
	public String[] execute(AdbClient adbClient, Map<String, String> parms) {
		File dir = new File(parms.get("routeDir"));
		final String[] files = dir.list();
		return files;
	}
}
