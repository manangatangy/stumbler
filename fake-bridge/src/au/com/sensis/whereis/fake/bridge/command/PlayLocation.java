package au.com.sensis.whereis.fake.bridge.command;

import java.util.Map;

import au.com.sensis.whereis.fake.bridge.AdbClient;

import com.google.gson.Gson;

public class PlayLocation implements Executor {

	/**
	 * Input request: { "cmd":"playLocation", "lat":"60.189165752381086", lon:"14.027822222560644", ... }
	 * Return result:  "played ok"
	 * 
	 * and optionally; 
	 * 		accuracy:"0.0",
	 * 		altitude:"100",
	 * 		bearing:"12.34",
	 * 		speed:"60.0"
	 */
	@Override
	public String execute(AdbClient adbClient, Map<String, String> parms) {
		Gson gson = new Gson();
		String json = gson.toJson(parms);
		return adbClient.sendAndWaitForReply(json, 2000);
	}
}
