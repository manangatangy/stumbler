package au.com.sensis.whereis.fake.bridge;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import au.com.sensis.whereis.fake.bridge.command.*;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpHandler implements com.sun.net.httpserver.HttpHandler {
	
	private AdbClient adbClient;
	
	public HttpHandler(AdbClient adbClient) {
		this.adbClient = adbClient;
	}
	
	private void log(String message) {
        System.out.println("HttpHandler: " + message);
	}
	
	private Map<String, String> getQueryParameters(String queryString) throws UnsupportedEncodingException {
		//log("completeQueryString: " + queryString);
		Map<String, String> parms = new HashMap<String, String>();
		if (!StringUtils.isEmpty(queryString)) {
			String[] queries = queryString.split("&");
			for (String query : queries) {
				//log("query: " + query);
				String decodedQuery = URLDecoder.decode(query, "UTF-8");
				//log("decodedQuery: " + decodedQuery);
				String[] words = decodedQuery.split("=");
				if (words.length > 0) {
					String key = words[0];
					String val = (words.length > 1) ? words[1] : null;
					//log("key: " + key);
					//log("val: " + val);
					parms.put(key, val);
				}
			}
		}
		return parms;
	}
	
	enum CommandType {
		statusCheck(new StatusCheck()),
		playLocation(new PlayLocation()),
		listRouteFiles(new ListRouteFiles()),
		readFileContents(new ReadFileContents()),
		appendRouteFile(new AppendRouteFile()),
		deleteRouteFile(new DeleteRouteFile()),
		appendGpxFile(new AppendGpxFile()),
        index(new Index()),

		unknown(new Executor() {
			@Override
			public Object execute(AdbClient adbClient, Map<String, String> parms) {
				return "unknown command";
			}
		});
		
		/**
		 * Catches all names other than Enums and matches them as unknown.
		 * Never returns null or throws exception.
		 */
		public static CommandType parse(String c) {
			CommandType commandType = unknown;
			try {
				commandType = CommandType.valueOf(c);
			} catch (Exception excIgnored) {
			}
			return commandType;
		}
		
		private Executor executor;
		private CommandType(Executor executor) {
			this.executor = executor;
		}
		public Object execute(AdbClient adbClient, Map<String, String> parms) {
			return executor.execute(adbClient, parms);
		}
	}
	
	/**
	 * Examines the parms for a "cmd" string and matches that against a CommandType
	 * which then executes the parms and returns a result object, which is added to
	 * the response structure, which is returned as a JSON formatted string.  Note
	 * that the original "cmd" is also placed in the returned response structure,
	 */
	private String processParameters(Map<String, String> parms) {
		String cmdString = parms.get("cmd");
		CommandType commandType = CommandType.parse(cmdString);
		
		// Pass the 'routeDir' in to all executors.
		parms.put("routeDir", "routes");
		Object result = commandType.execute(adbClient, parms);

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("cmd", cmdString);
		response.put("result", result);
		//return response;
		
		Gson gson = new Gson();
		String responseText = gson.toJson(response);
		log("response: " + responseText);
		return responseText;
	}

	/**
	 * Check that this callback function name is a valid javascript identifier and not an attack script.
	 * Ref: http://stackoverflow.com/questions/3128062/is-this-safe-for-providing-jsonp#3128948
	 */
	private boolean validCallback(String callback) {
		// Assume it's valid for now.
		return true;
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		
		if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
			
			URI requestUri = exchange.getRequestURI();
			log("GET " + requestUri.toString());
			
			Map<String, String> parms = getQueryParameters(requestUri.getQuery());

			int responseCode = 200;
            String body = null;
            if (exchange.getRequestHeaders().getFirst("Accept").toString().toLowerCase().contains("script")) {
                body = processParameters(parms);

                String callback = null;			// Names the js callback function, or null if not jsonp.
                if (parms.containsKey("callback")) {
                    callback = parms.get("callback");
                    if (validCallback(callback)) {
                        body = callback + "(" + body + ")";
                    } else {
                        responseCode = 400;
                        body = "bad callback name";
                    }
                }

                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "application/" + (callback != null ? "javascript" : "json") + "; charset=utf-8");
                exchange.sendResponseHeaders(responseCode, 0);
            } else {
                body = CommandType.index.execute(adbClient,parms).toString();

                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/html");
                exchange.sendResponseHeaders(responseCode, 0);
            }

			OutputStream os = exchange.getResponseBody();
			os.write(body.getBytes());
            os.flush();
			os.close();
		}
		
	}

    private URI getRootUri() {
        try {
            return new URI("/");
        } catch (URISyntaxException e) {
            throw new RuntimeException("unable to set up basic uri!");
        }
    }
}
