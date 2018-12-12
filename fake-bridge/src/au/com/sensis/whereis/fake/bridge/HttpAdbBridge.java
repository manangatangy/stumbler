package au.com.sensis.whereis.fake.bridge;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;

import com.sun.net.httpserver.HttpServer;

public class HttpAdbBridge {
	
	/**
	 * usage <http-port>5100 <adb-port>6100 <fake-port>7100
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		int httpPort = 5100;
		int adbPort = 6100;
		int fakePort = 7100;
		
		// If any of the args are non-numeric, or we have less than 2 args, then exit with usage msg.
		if (httpPort == 0 || adbPort == 0)
			exitWith("usage: HttpAdbBridge <http-port> <adb-port> [<fake-port>]\n" +
					 " e.g., HttpAdbBridge 5100 6100 [7100]\n" +
					 " where the <fake-port> causes adb port forwarding to be started"
					 , 1);

		String adbCommand = makeAdbCommand(adbPort, fakePort);		// Only used if fakePort is specified.
		
		AdbClient adbClient = new AdbClient(adbPort, adbCommand, fakePort);

		HttpAdbBridge httpAdbBridge = new HttpAdbBridge(httpPort, adbClient);
		
	}

	private static final String androidHomeKey = "ANDROID_HOME";
	
	/**
	 * If either of the ports are zero, returns null.  Else
	 * use the port numbers to format the adb port forwarding 
	 * command and check that the adb exe is available.
	 */
	public static String makeAdbCommand(int adbPort, int fakePort) {
		if (adbPort == 0 || fakePort == 0)
			return null;
		String androidHome = System.getenv(androidHomeKey);
        //androidHome = "/home/david/tools/android-sdk-linux";
		if (StringUtils.isEmpty(androidHome))
			exitWith("can't run adb: no value for " + androidHomeKey, 1);
		String exe = androidHome + File.separatorChar + "platform-tools" + File.separatorChar + "adb.exe";
		if (! (new File(exe)).canExecute() ){
			exe = androidHome + File.separatorChar + "platform-tools" + File.separatorChar + "adb";
		}
		if (! (new File(exe)).canExecute() ){
			exitWith("can't run adb: file " + exe + " or adb.exe aren't executable", 1);
		}
			
		return exe + " forward tcp:" + adbPort + " tcp:" + fakePort;
	}		
	
	public static void exitWith(String message, int exitCode) {
		System.out.println(message);
		System.exit(exitCode);
	}
	
	private HttpServer httpServer;
	
	private HttpAdbBridge(int httpPort, final AdbClient adbClient) throws IOException {
		
		// Ref: http://stackoverflow.com/a/1186372  (discusses Sun's HttpServer).
		httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
		httpServer.createContext("/", new HttpHandler(adbClient));
		httpServer.setExecutor(Executors.newCachedThreadPool());
		httpServer.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				adbClient.terminate();		// Interrupt the AdbClient's worker thread.
				httpServer.stop(5);			// Close listening socket, disallow new exchanges, exit background threads.
			}
		});
		
		System.out.println("HttpServer is listening on port: " + httpPort);
	}
	
}

