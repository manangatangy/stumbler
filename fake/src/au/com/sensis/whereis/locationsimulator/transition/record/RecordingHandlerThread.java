package au.com.sensis.whereis.locationsimulator.transition.record;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.FileType;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.RecordObtainType;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.RecordProviderType;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;
import au.com.sensis.whereis.locationsimulator.transition.TransitionHandlerThread;

public class RecordingHandlerThread extends TransitionHandlerThread implements LocationListener {

	private static final String TAG = RecordingHandlerThread.class.getName();
	private ToneGenerator toneGenerator;

	private SimSettings simSettings;
	private DisplayInfo displayInfo = new DisplayInfo();
	
	// Resources to be released.
	private LocationWriter writer;
	private LocationManager locationManager;
	
	private boolean recordingIsPaused = true; 
	
	public RecordingHandlerThread(Context context, StatusHandler statusHandler, SimSettings simSettings) {
		super("RecordingHandler", statusHandler);
		this.simSettings = simSettings;
		this.locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		this.toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
		
        Log.i(TAG, "RecordingHandler: simSettings=" + simSettings + ", " 
        		+ "GPS providerEnabled=" + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) 
        		+ "NET providerEnabled=" + locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
	}

	private void exit() {
        Log.i(TAG, "exit: completing RecordingHandler");
		statusHandler.statusChange(SimStatus.STANDBY_STANDBY);
		// Silently release all resources.
		cancelTimer();
		if (writer != null) {
			writer.closeSilently();
		}
		if (simSettings.getRecordObtainType() == RecordObtainType.BY_CALLBACK) {
			locationManager.removeUpdates(this); 
		}
		toneGenerator.release();
		quit();
	}
	
	@Override
	public void handleTransition(Transition transition) {
		try {
			switch (transition) {
			case RECORD:
				if (TextUtils.isEmpty(simSettings.getDirectory()))
					throw new IOException("no directory specified");
				if (TextUtils.isEmpty(simSettings.getFilenameWithoutExtension()))
					throw new IOException("no filename specified");
				
				File outFile = new File(simSettings.getDirectory(), simSettings.getFilename());
				writer = (simSettings.getRecordFileType() == FileType.GPX) 
						? new GxpWriter(outFile) : new JsonWriter(outFile, simSettings);
				displayInfo.filePath = simSettings.getDirectory() + File.separator + simSettings.getFilename();
				
				displayInfo.distanceTravelled = 0;
				displayInfo.locationsCount = 0;		
				startTime = (new Date()).getTime();

				statusHandler.statusChange(SimStatus.RECORDING_PAUSED);
				statusHandler.newDisplayInfo(displayInfo);
				recordingIsPaused = true;
				
				// Ensure the required location provider is enabled.
				String locationProvider 
					= (simSettings.getRecordProviderType() == RecordProviderType.GPS)
					? LocationManager.GPS_PROVIDER
					: LocationManager.NETWORK_PROVIDER;
					
				if (!locationManager.isProviderEnabled(locationProvider)) 
					throw new IOException(locationProvider + " provider not enabled");

				startTimer(simSettings.getRecordPollingPeriod());		// Even if not polling for location (it's simpler).

				if (simSettings.getRecordObtainType() == RecordObtainType.BY_CALLBACK) {
					// Save power by only asking for callbacks if specified.
					locationManager.requestLocationUpdates(locationProvider, 
							simSettings.getRecordMinCallbackTime(), 
							simSettings.getRecordMinCallbackDistance(), this);
				}
				
				break;
			case PAUSE1:
				recordingIsPaused = true;
				statusHandler.statusChange(SimStatus.RECORDING_PAUSED);
				break;
			case RESUME1:
				recordingIsPaused = false;
				statusHandler.statusChange(SimStatus.RECORDING_RECORDING);
				break;
			case STOP1:
			case STOP5:
				cancelTimer();
				writer.close();
		        Log.i(TAG, "closed file with " + displayInfo.locationsCount + " records");
				statusHandler.showToast("saved " + displayInfo.locationsCount + " points to file " + displayInfo.filePath);
				exit();
				break;
			}
		} catch (IOException ioe) {
	        Log.i(TAG, "file i/o error: " + ioe.getMessage());
			statusHandler.showToast("recording failed " + ioe.getMessage());
			ioe.printStackTrace();
			exit();
		}
	}
	
	@Override
	public void onLocationChanged(android.location.Location aLocation) {
		if (!recordingIsPaused) {
	        Log.i(TAG, "onLocationChanged: android.location = " + aLocation);
	        recordIncomingLocation(aLocation);
		}
	}

	@Override
	public long onTimer() {
		if (!recordingIsPaused && simSettings.getRecordObtainType() == RecordObtainType.BY_POLLING) {
			android.location.Location aLocation = 
				(simSettings.getRecordProviderType() == RecordProviderType.GPS)
				? locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
				: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        Log.i(TAG, "onTimer: android.location = " + aLocation);
	        recordIncomingLocation(aLocation);
		}
        return TIMER_PERIOD_UNCHANGED;
	}
	
	private long startTime;								// Time that RECORDING was started, used to calculate elapsedTime.

	private void recordIncomingLocation(android.location.Location aLocation) {

        if (aLocation != null) {
        	if (simSettings.isBeepOnLocationRecorded())
        		toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);		
			
    		Log.d(TAG, String.format("recordIncomingLocation provider:[%s] lat,long:[%s,%s] bearing:[%s] time:[%s] accuracy:[%s]", 
    				aLocation.getProvider(), aLocation.getLatitude(), aLocation.getLongitude(), 
    				aLocation.getBearing(), aLocation.getTime(), aLocation.getAccuracy()));
    		
    		try {
    			writer.write(aLocation);
    			displayInfo.locationsCount = writer.getRecordCount();
    	    	displayInfo.distanceTravelled = (int)writer.getDistance();
    		} catch (IOException ioe) {
    	        Log.i(TAG, "file i/o error: " + ioe.getMessage());
    			statusHandler.showToast("recording failed " + ioe.getMessage());
    			ioe.printStackTrace();
    			exit();
    		}
        }
        // Even if a dud location is received, we tick the clock to show that something is still happening.
    	displayInfo.elapsedTime = (new Date()).getTime() - startTime;
		statusHandler.newDisplayInfo(displayInfo);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

}
