package au.com.sensis.whereis.locationsimulator.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler.StatusChangeListener;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;
import au.com.sensis.whereis.locationsimulator.transition.TransitionHandlerThread;
import au.com.sensis.whereis.locationsimulator.transition.playback.ReplayingHandlerThread;
import au.com.sensis.whereis.locationsimulator.transition.record.RecordingHandlerThread;
import au.com.sensis.whereis.locationsimulator.transition.remote.RemoteHandlerThread;

public class SimService extends Service {
	
	public static final String TRANSITION_KEY = "TRANSITION";
	public static final String SETTINGS_KEY = "SETTINGS";
//	public static final String DIRECTORY_KEY = "DIRECTORY";
//	public static final String FILENAME_KEY = "FILENAME";
	private static final String TAG = SimService.class.getName();
	
	private final IBinder binder = new LocalBinder();
	// Initialise with standby status, so that listeners are updated.
	private StatusHandler statusHandler = new StatusHandler(this, SimStatus.STANDBY_STANDBY);
	
	private SimNotifier simNotifier;
	
	private TransitionHandlerThread transitionHandlingThread;
	
	public class LocalBinder extends Binder {
		public SimService getService() {
            return SimService.this;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public StatusHandler getStatusHandler() {
		return statusHandler;
	}
	
	@Override
	public void onCreate() {
        Log.i(TAG, "onCreate: " + threadStats());
        simNotifier = new SimNotifier(this);
        // We need to know when the thread has quit, in order to release the reference.
        statusHandler.addStatusChangeListener(new StatusChangeListener() {
			@Override
			public void onStatusChange(SimStatus simStatus, DisplayInfo displayInfo) {
				if (simStatus == SimStatus.STANDBY_STANDBY) {
			        Log.i(TAG, "onStatusChange(" + Thread.currentThread().getName() + "): " + threadStats() + ", releasing thread");
					transitionHandlingThread = null;
				}
			}
		});
        statusHandler.addStatusChangeListener(simNotifier);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The following guard seems necessary because Griffo has reported NullPointerExceptions here.
		if (intent == null) {
	        Log.i(TAG, "onStartCommand(" + Thread.currentThread().getName() + "): " + threadStats() + ", intent == null");
	        return START_STICKY;
		}
		if (intent.getExtras() == null) {
	        Log.i(TAG, "onStartCommand(" + Thread.currentThread().getName() + "): " + threadStats() + ", intent.getExtras() == null");
	        return START_STICKY;
		}
		if (intent.getExtras().getSerializable(TRANSITION_KEY) == null) {
	        Log.i(TAG, "onStartCommand(" + Thread.currentThread().getName() + "): " + threadStats() + ", intent.getExtras().getSerializable(TRANSITION_KEY) == null");
	        return START_STICKY;
		}
		
    	Transition transition = (Transition) intent.getExtras().getSerializable(TRANSITION_KEY);
        Log.i(TAG, "onStartCommand(" + Thread.currentThread().getName() + "): " + threadStats() + ", transition: " + transition);
        // Examine the mostRecentSimStatus and work out what to ask the serviceThread to do.
    	if (transitionHandlingThread == null) {
    		SimSettings simSettings = (SimSettings) intent.getExtras().getSerializable(SETTINGS_KEY);
    		// Create new thread giving it the handler on which to callback statusChange(). 
    		switch(transition) {
    		case RECORD:
    			transitionHandlingThread = new RecordingHandlerThread(this, statusHandler, simSettings);
    			break;
    		case PLAYBACK:
    			transitionHandlingThread = new ReplayingHandlerThread(this, statusHandler, simSettings);
    			break;
    		case REMOTE:
    			transitionHandlingThread = new RemoteHandlerThread(this, statusHandler);
    			break;
    		}
    		if (transitionHandlingThread != null) {
    			transitionHandlingThread.start();
    			transitionHandlingThread.waitUntilReady();
    	        Log.i(TAG, "onStartCommand(" + Thread.currentThread().getName() + "): " + threadStats() + ", started new thread");
    		}
    		
    	}
       	if (transitionHandlingThread == null) {
	        Log.i(TAG, "onStartCommand: no thread to handle transition:" + transition);
       	} else {
    		// Pass transition to the thread which may callback the StatusHandler.
    		transitionHandlingThread.doTransition(transition);
       	}
		return START_STICKY;
	}
	
	// onStopCommand
	
	@Override
	public void onDestroy() {
        Log.i(TAG, "onDestroy: " + threadStats());
        statusHandler.removeStatusChangeListener(simNotifier);
	}

	public String threadStats() {
		String message = "handlerThread is null";
		if (transitionHandlingThread != null) {
			message = "handlerThread(" + transitionHandlingThread.getName() + ") is " + (transitionHandlingThread.isAlive() ? "alive" : "dead");
		}
		return message;
	}

	public static Intent makeServiceIntentForTransition(Context context, Transition transition) {
    	Intent serviceIntent = new Intent(context, SimService.class);
        serviceIntent.putExtras(makeBundle(transition));
        return serviceIntent;
	}

	private static Bundle makeBundle(Transition transition) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SimService.TRANSITION_KEY, transition);
        return bundle;
	}
	
	public static Intent makeServiceIntentForTransition(Context context, Transition transition, SimSettings simSettings) {
    	Intent serviceIntent = new Intent(context, SimService.class);
    	Bundle bundle = makeBundle(transition);
        bundle.putSerializable(SimService.SETTINGS_KEY, simSettings);
        serviceIntent.putExtras(bundle);
        return serviceIntent;
	}

}
