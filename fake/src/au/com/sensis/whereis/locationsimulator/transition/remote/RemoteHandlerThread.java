package au.com.sensis.whereis.locationsimulator.transition.remote;

import android.content.Context;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler.StatusChangeListener;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;
import au.com.sensis.whereis.locationsimulator.transition.TransitionHandlerThread;

public class RemoteHandlerThread extends TransitionHandlerThread implements StatusChangeListener {

	private static final String TAG = RemoteHandlerThread.class.getName();

	private Context context;
	private Thread thread;
	private boolean doRestartUponThreadFinish;
	
	public RemoteHandlerThread(Context context, StatusHandler statusHandler) {
		super("RemoteHandler", statusHandler);
		this.context = context;
		statusHandler.addStatusChangeListener(this);
	}

	/**
	 * De-register the startChangeListener and exit this HandlingThread.
	 */
	private void exit() {
        Log.i(TAG, "exit: completing RemoteHandler");
		statusHandler.removeStatusChangeListener(this);
		thread = null;
		quit();
	}
	
	@Override
	public void handleTransition(Transition transition) {
		switch (transition) {
		case REMOTE:
			if (thread == null)
				startNewThread();
			break;
		case PAUSE3:
			doRestartUponThreadFinish = true;
		case STOP3:
			// The only way to control the RemoteRunnable thread is to interrupt it, after which
			// it will perform a statusChange notification, which we then handle in this class,
			// to determine if the thread should be restarted (and go back to disconnected ) or
			// released completely. 
			if (thread != null) {
		        Log.i(TAG, "handleTransition: interrupting server thread");
				thread.interrupt();
			}
			break;
		}
	}
	
	@Override
	public void onStatusChange(SimStatus simStatus, DisplayInfo displayInfo) {
		switch (simStatus) {
		case STANDBY_STANDBY:
			if (doRestartUponThreadFinish)
				startNewThread();
			else
				exit();
			break;
		}
	}

	private void startNewThread() {
        Log.i(TAG, "startNewThread: starting server thread");
		RemoteRunnable runnable = new RemoteRunnable(context, statusHandler);
        thread = new Thread(runnable, "RemoteRunnable");
    	thread.start();
    	doRestartUponThreadFinish = false;
	}
	
	@Override
	public long onTimer() {
		return TIMER_CANCEL;
	}
}
