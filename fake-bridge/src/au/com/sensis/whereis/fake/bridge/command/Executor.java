package au.com.sensis.whereis.fake.bridge.command;

import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

public interface Executor {
	
	/**
	 * @return a structure holding results which ends up GSON stringified.
	 */
	public Object execute(AdbClient adbClient, Map<String, String> parms);
	
}
