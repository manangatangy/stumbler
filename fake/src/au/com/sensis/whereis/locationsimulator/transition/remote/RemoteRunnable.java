package au.com.sensis.whereis.locationsimulator.transition.remote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Date;

import org.json.JSONObject;

import android.content.Context;
import android.location.LocationManager;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler;
import au.com.sensis.whereis.locationsimulator.service.location.Location;
import au.com.sensis.whereis.locationsimulator.service.location.LocationSimulator;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;

public class RemoteRunnable implements Runnable {

	private static final String TAG = RemoteRunnable.class.getName();
	private static int SERVER_PORT = 7100;
	
	private Context context;
	private StatusHandler statusHandler;
	
	public RemoteRunnable(Context context, StatusHandler statusHandler) {
		this.context = context;
		this.statusHandler = statusHandler;
	}
	
	@Override
	public void run() {
        Log.i(TAG, "run: starting remote runnable");
        
        String exitMessage = "normally";
        
    	LocationSimulator simulator = null;
        ServerSocketChannel serverChannel = null;
        SocketChannel clientChannel = null;

        DisplayInfo displayInfo = new DisplayInfo();
    	// Time that REMOTE was started, used to calculate elapsedTime.
    	long startTime = (new Date()).getTime();
		displayInfo.locationsCount = 0;	
		displayInfo.elapsedTime = 0;
		displayInfo.filePath = "";
		displayInfo.totalRouteDistance = 0;
		displayInfo.distanceTravelled = 0;
		
		Location previouslyPlayedLocation = null;		// Used to calculate distance travelled so far.
        
        try {
        	
            Log.i(TAG, "run: initializing location simulator");
    		simulator = new LocationSimulator();
    		if (!simulator.init(context, LocationManager.GPS_PROVIDER)) {
    			throw new Exception("can't simulate locations (disabled in Settings)");
    		}
        	
            Log.i(TAG, "run: opening server socket");
            serverChannel = ServerSocketChannel.open();
            SocketAddress port = new InetSocketAddress(SERVER_PORT);
            serverChannel.socket().bind(port);

            while (true) {
                Log.i(TAG, "run: waiting for client connection");
    			statusHandler.statusChange(SimStatus.REMOTE_DISCONNECTED);
            	clientChannel = serverChannel.accept();			// May be interrupted.
//            	if (clientChannel.isBlocking()) {
//                	clientChannel.configureBlocking(false);
//            	}
            	
                Log.i(TAG, "run: connected to client");
    			statusHandler.statusChange(SimStatus.REMOTE_CONNECTED);

            	// Ref: http://technfun.wordpress.com/2009/01/29/networking-in-java-non-blocking-nio-blocking-nio-and-io/
            	
                LineAssembler assembler = new LineAssembler();
                
            	Charset charset = Charset.forName("ISO-8859-1");
            	CharsetDecoder decoder = charset.newDecoder();
            	CharsetEncoder encoder = charset.newEncoder();
            	ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                CharBuffer charBuffer = CharBuffer.allocate(1024);
                
                while ((clientChannel.read(buffer)) != -1) {		// Blocking read. May be interrupted.

                	buffer.flip();
                	decoder.decode(buffer, charBuffer, false);
                	charBuffer.flip();
                	
                	String fragment = charBuffer.toString();
                    Log.i(TAG, "run: received fragment '" + fragment + "'");
                    
                	if (assembler.isCompleteLineAfterAppending(fragment)) {
                		String line = assembler.nextLine();
                        Log.i(TAG, "run: received complete line: " + line);
                        try {
                            JSONObject jsonObject = new JSONObject(line);
                            String cmd = jsonObject.getString("cmd");
                            if ("statusCheck".equals(cmd)) {
                            	clientChannel.write(encoder.encode(CharBuffer.wrap("server ready\n")));
                            } else if ("playLocation".equals(cmd)) {
                                Location location = Location.parse(jsonObject);		// Read remaining fields.
                            	simulator.synchronousSendLocation(location);
                            	clientChannel.write(encoder.encode(CharBuffer.wrap("played ok\n")));
                            	displayInfo.locationsCount++;
                            	displayInfo.distanceTravelled += location.distanceInMetres(previouslyPlayedLocation);
                            	previouslyPlayedLocation = location;
                            } else {
                                Log.i(TAG, "run: error unknown command: " + line);
                            }
                        } catch(Exception exc) {
                            Log.i(TAG, "run: error parsing line: " + exc.getMessage());
                        }
                    	displayInfo.elapsedTime = (new Date()).getTime() - startTime;
            			statusHandler.newDisplayInfo(displayInfo);
                	}
                	buffer.clear();
                	charBuffer.clear();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        	exitMessage = exc.getMessage();
        	if (exitMessage == null) {
        		exitMessage = exc.getClass().getSimpleName();
        	}
			statusHandler.showToast("remote exception " + exitMessage);
        } finally {
        	closeIgnoringExceptions(serverChannel);
        	closeIgnoringExceptions(clientChannel);
        	if (simulator != null) {
        		simulator.finish();
        	}
        }
        
        Log.i(TAG, "run: exiting remote runnable: " + exitMessage);
		statusHandler.statusChange(SimStatus.STANDBY_STANDBY);
	}

	private void closeIgnoringExceptions(AbstractSelectableChannel channel) {
    	if (channel != null) {
    		try {
    			channel.close();
            } catch (IOException ignored) {
            }
    	}
	}
	
}
