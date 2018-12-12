package au.com.sensis.whereis.locationsimulator.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;

/**
 * There are two types of data passed over this channel; the SimStatus (which may be used to affect a transition
 * and the DisplayInfo (which is only for notification use).  They are sent by the client in separate calls to
 * the StatusHandler, but a single onChange listener delivers them both wrapped in the one call.
 * @author w80589
 *
 */
public class StatusHandler extends Handler {

	private static final String TAG = StatusHandler.class.getName();

	private Context context;
	private SimStatus mostRecentSimStatus = SimStatus.STANDBY_STANDBY;
	private DisplayInfo mostRecentDisplayInfo = new DisplayInfo();
	
	public interface StatusChangeListener {
		void onStatusChange(SimStatus simStatus, DisplayInfo displayInfo);
	}

	private List<StatusChangeListener> statusChangeListenerList = new ArrayList<StatusChangeListener>();
	
	public StatusHandler(Context context, SimStatus mostRecentSimStatus) {
		this.context = context;
		this.mostRecentSimStatus = mostRecentSimStatus;
	}

//	public SimStatus getMostRecentSimStatus() {
//		return mostRecentSimStatus;
//	}
//	public DisplayInfo getMostRecentDisplayInfo() {
//		return mostRecentDisplayInfo;
//	}
	
	/**
	 * Add this listeners to the list of registered listeners and callback
	 * its listener with the most recent SimStatus (if we have one).
	 * @param statusChangeListener
	 */
	public void addStatusChangeListener(StatusChangeListener statusChangeListener) {
		statusChangeListenerList.add(statusChangeListener);
		if (mostRecentSimStatus != null) {
			statusChangeListener.onStatusChange(mostRecentSimStatus, mostRecentDisplayInfo);			
		}
	}
	public void removeStatusChangeListener(StatusChangeListener statusChangeListener) {
		if (statusChangeListenerList.contains(statusChangeListener)) {
			statusChangeListenerList.remove(statusChangeListener);
		}
	}
	
	private static final int MSG_SIMSTATUS = 1;
	private static final int MSG_DISPLAYINFO = 2;
	private static final int MSG_SHOWTOAST = 3;
	
	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_SIMSTATUS:
			mostRecentSimStatus = (SimStatus)msg.obj;
			break;
		case MSG_DISPLAYINFO:
			mostRecentDisplayInfo = (DisplayInfo)msg.obj;
			break;
		case MSG_SHOWTOAST:
			Toast.makeText(context, (String)msg.obj, Toast.LENGTH_SHORT).show();
		}
		Log.i(TAG, "handleMessage(" + Thread.currentThread().getName() + "): simStatus: " + mostRecentSimStatus + ", displayInfo: " + mostRecentDisplayInfo);
		for (StatusChangeListener statusChangeListener : statusChangeListenerList) {
			statusChangeListener.onStatusChange(mostRecentSimStatus, mostRecentDisplayInfo);
		}
	}
	
	public void statusChange(SimStatus simStatus) {
		Log.i(TAG, "statusChange(" + Thread.currentThread().getName() + "): simStatus: " + simStatus);		
		Message message = Message.obtain(this, MSG_SIMSTATUS, 0, 0, simStatus);
		sendMessage(message);
	}
	public void newDisplayInfo(DisplayInfo displayInfo) {
		Log.i(TAG, "newDisplayInfo(" + Thread.currentThread().getName() + "): displayInfo: " + displayInfo);		
		Message message = Message.obtain(this, MSG_DISPLAYINFO, 0, 0, displayInfo);
		sendMessage(message);
	}
	public void showToast(String toastText) {
		Log.i(TAG, "showToast(" + Thread.currentThread().getName() + "): toastText: " + toastText);		
		Message message = Message.obtain(this, MSG_SHOWTOAST, 0, 0, toastText);
		sendMessage(message);
	}

}
