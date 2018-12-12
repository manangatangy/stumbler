package au.com.sensis.whereis.locationsimulator.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import au.com.sensis.whereis.locationsimulator.R;
import au.com.sensis.whereis.locationsimulator.SimActivity;
import au.com.sensis.whereis.locationsimulator.ViewSupport;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;

public class SimNotifier implements StatusHandler.StatusChangeListener {

	private static final String TAG = SimNotifier.class.getName();
	private Context context;
	private NotificationManager notificationManager;
	private int NOTIFICATION_ID = R.string.app_name;
	private SimStatus lastSimStatus;
	
	public SimNotifier(Context context) {
		this.context = context;
		notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

//	public void quit() {
//        Log.i(TAG, "quit: cancel notification");
//		notificationManager.cancel(NOTIFICATION_ID);
//	}
	
	@Override
	public void onStatusChange(SimStatus simStatus, DisplayInfo displayInfo) {
        Log.i(TAG, "onStatusChange(" + Thread.currentThread().getName() + "): status: " + simStatus + ", displayInfo: " + displayInfo);
        if (simStatus == SimStatus.STANDBY_STANDBY) {
	        Log.i(TAG, "onStatusChange: cancel notification");
			notificationManager.cancel(NOTIFICATION_ID);
		} else {
			Notification notification = makeNotificationForStatus(simStatus, displayInfo);
			notificationManager.notify(NOTIFICATION_ID, notification);
		}
	}

	public Notification makeNotificationForStatus(SimStatus simStatus, DisplayInfo displayInfo) {
		// Primary ref: http://developer.android.com/guide/topics/ui/notifiers/notifications.html#CustomExpandedView
		// Also http://android-developers.blogspot.com.au/2011/06/deep-dive-into-location.html
		// And http://developer.android.com/reference/android/widget/RemoteViews.html#setOnClickPendingIntent(int, android.app.PendingIntent)
		CharSequence tickerText = null;
		if (lastSimStatus == simStatus) {
			// Since there is no status change, don't show any ticker-text. Animate the icon instead. 
			// TODO - animate icon
		} else {
			tickerText = simStatus.mode + " : " + simStatus.state;
			lastSimStatus = simStatus;
		}

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sim_notification);
    	//remoteViews.setImageViewResource(R.id.iconImage, R.drawable.ic_launcher);
		
		ViewSupport viewSupport = new ViewSupport(context, remoteViews);
		
        viewSupport.addView("textViewState", R.id.textViewStateN);
        viewSupport.addView("textViewElapsed", R.id.textViewElapsedN);
        
        viewSupport.addView("imageViewSlower", R.id.imageViewSlowerN);
        viewSupport.addView("imageViewResume", R.id.imageViewStartN);
        viewSupport.addView("imageViewPause", R.id.imageViewPauseN);
        viewSupport.addView("imageViewStop", R.id.imageViewStopN);
        viewSupport.addView("imageViewFaster", R.id.imageViewFasterN);
        
        viewSupport.addView("textViewCount", R.id.textViewCountN);
        viewSupport.addView("textViewDistance", R.id.textViewDistanceN);
        viewSupport.addView("textViewMode", R.id.textViewModeN);
		
        for (String buttonSuffix : new String[] {
        		"Pause", "Resume", "Stop", "Slower", "Faster"
        }) {
        	String viewName = "imageView" + buttonSuffix;
        	String actionName = buttonSuffix.toUpperCase();
			Transition transition = Transition.getTransitionForStatusAndPrefix(actionName, simStatus);
			if (transition != null) {
		        Log.i(TAG, "makeNotificationForStatus: setTransition for buttonSuffix:" + buttonSuffix + " simStatus=" + simStatus + " transition=" + transition);
				viewSupport.setTransition(viewName, transition, null);			// No need to pass simStatus.
				viewSupport.setVisibility(viewName, View.VISIBLE);
			} else {
				viewSupport.setVisibility(viewName, View.GONE);
			}
        }

    	Notification notification = new Notification(R.drawable.fake, tickerText, System.currentTimeMillis());
    	notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR; 

    	// Default click action is to intent the activity.
    	Intent activityIntent = new Intent(context, SimActivity.class);
    	activityIntent.addFlags(Notification.FLAG_ONGOING_EVENT);
    	notification.contentIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	notification.contentView = remoteViews;
    	
        viewSupport.setTextViewText("textViewState", simStatus.state.name());
        viewSupport.setTextViewText("textViewMode", simStatus.mode.name());
        viewSupport.setTextViewText("textViewElapsed", displayInfo.formattedElapsedTime());
        viewSupport.setTextViewText("textViewCount", displayInfo.locationsCount + " points");
        viewSupport.setTextViewText("textViewDistance", displayInfo.formatted(displayInfo.distanceTravelled) + " m");
    	
    	return notification;
	}
	
	public void setupTransitionAction(RemoteViews remoteViews, Transition transition, int viewId) {
		remoteViews.setTextViewText(viewId, transition.name());
		remoteViews.setOnClickPendingIntent(viewId, makeServiceIntentForTransition(transition));
	}
	
	private PendingIntent makeServiceIntentForTransition(Transition transition) {
    	Intent serviceIntent = SimService.makeServiceIntentForTransition(context, transition);
		PendingIntent servicePendingIntent = PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return servicePendingIntent;
	}
	
}
