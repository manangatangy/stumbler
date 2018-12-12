package au.com.sensis.whereis.fake.bridge.command;

import java.util.Map;

import com.google.gson.Gson;

import au.com.sensis.whereis.fake.bridge.AdbClient;

public class StatusCheck implements Executor {

	/**
	 * Request: { "cmd":"statusCheck" }
	 * Result:  { "isConnected":<is-adb-conected?>, "deviceResponse":<response-from-fake-server> } 
	 */
	@Override
	public StatusCheckResult execute(AdbClient adbClient, Map<String, String> parms) {
		Gson gson = new Gson();
		String json = gson.toJson(parms);
		StatusCheckResult result = new StatusCheckResult();
		result.isConnected = adbClient.isConnected();
		result.deviceResponse = result.isConnected ? adbClient.sendAndWaitForReply(json, 2000) : "";
		return result;
	}
}

class StatusCheckResult {
	boolean isConnected;
	String deviceResponse;
}
