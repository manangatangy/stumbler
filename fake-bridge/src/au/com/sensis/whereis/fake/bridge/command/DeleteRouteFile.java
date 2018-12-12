package au.com.sensis.whereis.fake.bridge.command;

import java.io.File;
import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

public class DeleteRouteFile implements Executor {

	/**
	 * Input request: { "cmd":"deleteRouteFile", "fileName":"someFileName.json" }
	 * Return result:  "ok" / "something else"
	 */
	@Override
	public String execute(AdbClient adbClient, Map<String, String> parms) {
		File dir = new File(parms.get("routeDir"));
		String fileName = parms.get("fileName");
		File file = new File(dir, fileName);
		if (!file.exists())
			return "file " + fileName + " doesn't exist";
		if (file.isDirectory())
			return "file " + fileName + " is directory";
		if (!file.canWrite())
			return "file " + fileName + " isn't writeable";
		return file.delete() ? "ok" : "couldn't delete";
	}
}
