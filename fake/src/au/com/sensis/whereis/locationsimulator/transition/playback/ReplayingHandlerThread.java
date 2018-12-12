package au.com.sensis.whereis.locationsimulator.transition.playback;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.location.LocationManager;
import android.text.TextUtils;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.FileType;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler;
import au.com.sensis.whereis.locationsimulator.service.location.Location;
import au.com.sensis.whereis.locationsimulator.service.location.LocationSimulator;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;
import au.com.sensis.whereis.locationsimulator.transition.TransitionHandlerThread;

public class ReplayingHandlerThread extends TransitionHandlerThread {

	private static final String TAG = ReplayingHandlerThread.class.getName();
	
	private Context context;	
	private SimSettings simSettings;
	private DisplayInfo displayInfo = new DisplayInfo();
	
	// Resources to be released.
	private LocationReader reader;
	private LocationSimulator simulator;
	
	public ReplayingHandlerThread(Context context, StatusHandler statusHandler, SimSettings simSettings) {
		super("ReplayingHandler", statusHandler);
		this.context = context;
		this.simSettings = simSettings;
		this.simulator = new LocationSimulator();
        Log.i(TAG, "ReplayingHandler: simSettings=" + simSettings);
	}

	private void exit() {
        Log.i(TAG, "exit: completing ReplayingHandler");
		statusHandler.statusChange(SimStatus.STANDBY_STANDBY);
		cancelTimer();
    	if (simulator != null)
    		simulator.finish();
    	simulator = null;
		quit();
	}

	@Override
	public void handleTransition(Transition transition) {
		try {
			switch (transition) {
			case PLAYBACK:
				if (TextUtils.isEmpty(simSettings.getDirectory()))
					throw new IOException("no directory specified");
				if (TextUtils.isEmpty(simSettings.getFilenameWithoutExtension()))
					throw new IOException("no filename specified");

				File inFile = new File(simSettings.getDirectory(), simSettings.getFilename());
				reader = (simSettings.getRecordFileType() == FileType.GPX) 
						? new GpxReader(inFile, simSettings) 
						: new JsonReader(inFile, simSettings);
				displayInfo.filePath = simSettings.getDirectory() + File.separator + simSettings.getFilename();
				
				if (!simulator.init(context, LocationManager.GPS_PROVIDER)) {
					throw new Exception("can't simulate locations (disabled in Settings)");
				}
				
				playBackIsPaused = true;
				index = 0;		// Next point to use.
				displayInfo.distanceTravelled = 0;
				displayInfo.locationsCount = 0;		
				previousLocation = null;		// Initialize the distance accumulator.
				
				statusHandler.statusChange(SimStatus.PLAYBACK_PAUSED);
				statusHandler.newDisplayInfo(displayInfo);
				
				startTimer(1000);		// Kick off first location in 1 second.
				break;
			case PAUSE2:
				playBackIsPaused = true;
				statusHandler.statusChange(SimStatus.PLAYBACK_PAUSED);
				break;
			case RESUME2:
				playBackIsPaused = false;
				statusHandler.statusChange(SimStatus.PLAYBACK_REPLAYING);
				break;
			case FASTER1:
			case FASTER2:
				doSkip(true);
				break;
			case SLOWER1:
			case SLOWER2:
				doSkip(false);
				break;
			case STOP2:
			case STOP4:
				cancelTimer();
		        Log.i(TAG, "stopped after playing " + index + " points from file " + displayInfo.filePath);
				statusHandler.showToast("stopped after playing " + index + " points from file " + displayInfo.filePath);
				exit();
				break;
			}
		} catch (Exception exp) {
	        Log.i(TAG, "error: " + exp.getMessage());
			statusHandler.showToast("playback failed " + exp.getMessage());
			exp.printStackTrace();
			exit();
		}
	}
	
	/**
	 * Set a value of the skipRequired flag (which communicates to the timer handler that a skip 
	 * is required).  Since the next call to onTimer() may be some time away (depending on the
	 * inter-location time interval) we can hurry up the process, by bringing the next call on
	 * sooner than it would otherwise occur.
	 */
	public void doSkip(boolean isForward) {
		skipRequired = isForward ? simSettings.getSkipInterval() : - simSettings.getSkipInterval();
		cancelTimer();
		startTimer(250);
	}

	private boolean playBackIsPaused;				// Flag to pause playing.
	private int index;								// Index into the route of the next location to be played. Doesn't change while paused.
	private int skipRequired;						// May be forwards or backwards or zero.
	private Location previousLocation;				// The previously played location is used to calculate distance travelled so far.
	
	@Override
	public long onTimer() {
		
		// Play the next location and adjust the displayInfo.
		Location location = reader.get(index);
    	simulator.asynchronousSendLocation(location);
		Log.i(TAG, "onTimer: play location[" + index + "] = " + location 
				+ ", skipRequired = " + skipRequired + ", paused = " + playBackIsPaused);
		long timeUntilNextCallback = 0;
    	
		// Now determine the index of the next location to be played.
		if (skipRequired != 0) {
			int initialIndex = index;
			boolean goForward = (skipRequired > 0);
	        Log.i(TAG, "skipping " + (goForward ? "FORWARD" : "BACKWARD") + " at least " + skipRequired + " millis from next-location-index " + index);
	        
	        long targetTime = reader.get(index).getTime() + skipRequired;
	        
			if (goForward) {
				// Increment index until the getTime is at least the target time.
				while (reader.get(index).getTime() < targetTime) {
					Log.i(TAG, "skipping FORWARD [" + index + "] " + reader.get(index).getTime() + " < " + targetTime);
					if (index >= reader.getRecordCount() - 1) {
						// Already pointing at last location, can't go forward any more.
				        //Log.i(TAG, "stopped skipping at upper limit of " + (route.length - 1));
						break;
					}
					index++;
					Log.i(TAG, "next [" + index + "] time:" + reader.get(index).getTime());
				}
			} else {
				// Decrement index until the getTime is not greater than the target time.
				while (reader.get(index).getTime() > targetTime) {
					Log.i(TAG, "skipping BACKWARD [" + index + "] " + reader.get(index).getTime() + " > " + targetTime);
					if (index <= 0) {
						// Already pointing at first location, can't go backward any more.
				        //Log.i(TAG, "stopped skipping at lower limit of " + 0);
						break;
					}
					index--;
				}
			}
			long actualSkip = reader.get(index).getTime() - reader.get(initialIndex).getTime();
	        Log.i(TAG, "skipped " + (goForward ? "FORWARD" : "BACKWARD") + " from " + initialIndex + " to " + index);
	        Log.i(TAG, "skipped " + (goForward ? "FORWARD" : "BACKWARD") + " to new next-location-index " + index);
			statusHandler.showToast("skipped " + (goForward ? "FORWARD" : "BACKWARD") + " " + (actualSkip / 1000) + " seconds");

	    	skipRequired = 0;		// Clear flag.

		} else {
			if (!playBackIsPaused) {
				// The normal incrementing is only performed if there is no skipping required.
				// The timeUntilNextCallback
				// We are not skipping and we are not paused, so determine the time for the next callback
				// based on the interval to the next locations time.  For all other conditions (ie, paused
				// or just skipped) use the default minimum callback delay.
				if (++index >= reader.getRecordCount()) {
			        Log.i(TAG, "finished playing " + index + " location from file " + displayInfo.filePath);
					statusHandler.showToast("finished playing " + index + " locations from file " + displayInfo.filePath);
					exit();
					return TIMER_CANCEL;
				}
				timeUntilNextCallback = reader.get(index).getTime() - reader.get(index - 1).getTime();
			}
		}
		
    	displayInfo.locationsCount++;
    	displayInfo.distanceTravelled += location.distanceInMetres(previousLocation);
    	previousLocation = location;
    	displayInfo.elapsedTime = reader.get(index).getTime();		// Displays relative time from start to current location.
		statusHandler.newDisplayInfo(displayInfo);
		
		// The new value for indexOfNextLocation is determined, now calc next timer delay, from that record.
    	if (playBackIsPaused)
   			return simSettings.getPlaybackPausedPlaybackPeriod();
    	
    	if (timeUntilNextCallback < simSettings.getPlaybackMinimumPeriod())
    		timeUntilNextCallback = simSettings.getPlaybackMinimumPeriod();

    	Log.i(TAG, "next callback in " + timeUntilNextCallback);
    		
		return timeUntilNextCallback;
	}

}
