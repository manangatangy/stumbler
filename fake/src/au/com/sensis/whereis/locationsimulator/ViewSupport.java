package au.com.sensis.whereis.locationsimulator;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RemoteViews;
import android.widget.TextView;
import au.com.sensis.whereis.locationsimulator.service.SimService;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.transition.Transition;

public class ViewSupport {

	private boolean isRemote;
	
	private Activity activity;
	private HashMap<String, View> viewObjects;
	
	private Context context;
	private RemoteViews remoteViews;
	private HashMap<String, Integer> viewIds;

	private HashMap<String, Transition> viewTransitions;
	
	public ViewSupport(Activity activity) {
		this.viewTransitions = new HashMap<String, Transition>();
		this.activity = activity;
		this.viewObjects = new HashMap<String, View>();
		this.isRemote = false;
	}
	
	public ViewSupport(Context context, RemoteViews remoteViews) {
		this.viewTransitions = new HashMap<String, Transition>();
		this.context = context;
		this.remoteViews = remoteViews;
		this.viewIds = new HashMap<String, Integer>();
		this.isRemote = true;
	}

	public void addView(String viewTag, int viewId) {
		if (isRemote) {
			viewIds.put(viewTag, viewId);
		} else {
			View view = activity.findViewById(viewId);
			viewObjects.put(viewTag, view);
		}
	}
	
	public void setVisibility(String viewTag, int visibility) {
		if (isRemote) {
			int viewId = viewIds.get(viewTag);
			remoteViews.setInt(viewId, "setVisibility", visibility);
		} else {
			View view = viewObjects.get(viewTag);
			view.setVisibility(visibility);
		}
	}
	
	public void setEnabled(String viewTag, boolean enabled) {
		if (isRemote) {
			int viewId = viewIds.get(viewTag);
			remoteViews.setBoolean(viewId, "setEnabled", enabled);
		} else {
			View view = viewObjects.get(viewTag);
			view.setEnabled(enabled);
		}
	}
	
	public void setTextViewText(String viewTag, CharSequence text) {
		if (isRemote) {
			int viewId = viewIds.get(viewTag);
			remoteViews.setTextViewText(viewId, text);
		} else {
			TextView view = (TextView) viewObjects.get(viewTag);
			view.setText(text);
		}
	}
	
	public View getView(String viewTag) {
		return viewObjects.get(viewTag);
	}
	
	public class CancelTransitionException extends Exception {
		private static final long serialVersionUID = 1L;	
	}
	
	public interface SettingsRetriever {
		/**
		 * Called by the onClick handler (or during setTransition, for remoteViews) to fetch 
		 * a suitable SimSettings which should be passed to the appropriate service method.
		 * Null may be returned if the service is not expecting a SimSettings.  This method
		 * may throw CancelTransitionException, in order to cancel the service method call.
		 */
		SimSettings getSettingsForTransition(Transition transition) throws CancelTransitionException;
	}

	/**
	 * Can only be performed if isRemote is false.
	 */
	public void setOnClickListener(String viewTag, OnClickListener onClickListener) {
		if (isRemote)
			throw new RuntimeException("cannot set OnClickListener for RemoteViews viewTag:" + viewTag);
		View view = viewObjects.get(viewTag);
		view.setOnClickListener(onClickListener);
	}
	
	/**
	 * Associate a service call (to the method corresponding to the specified Transition) with
	 * a click on the specified view.  If the SettingsRetriever is non-null, then it will be
	 * used to fetch a SimSettings instance to pass along to the service, else null will be
	 * passed as the SimSettings value.
	 * Also store the Transition associated with this viewTag, for reference by getTransition.
	 */
	public void setTransition(String viewTag, final Transition transition, final SettingsRetriever settingsRetriever) {
		if (isRemote) {
			try {
				int viewId = viewIds.get(viewTag);
				
				SimSettings simSettings = (settingsRetriever == null) ? null : settingsRetriever.getSettingsForTransition(transition);
				Intent serviceIntent = SimService.makeServiceIntentForTransition(context, transition, simSettings);
				// Note that the requestCode parameter to getService() is not arbitrary.
				// Ref http://www.bogdanirimia.ro/android-widget-click-event-multiple-instances/269
				PendingIntent servicePendingIntent = PendingIntent.getService(context, viewId, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				remoteViews.setOnClickPendingIntent(viewId, servicePendingIntent);
			} catch (CancelTransitionException cteIgnored) {
			}
		} else {
			OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						SimSettings simSettings = (settingsRetriever == null) ? null : settingsRetriever.getSettingsForTransition(transition);
						Intent serviceIntent = SimService.makeServiceIntentForTransition(activity.getBaseContext(), transition, simSettings);
						activity.startService(serviceIntent);
					} catch (CancelTransitionException cteIgnored) {
					}
				}
			};
			onOnClickListeners.put(viewTag, onClickListener);
			setOnClickListener(viewTag, onClickListener);
		}
		viewTransitions.put(viewTag, transition);
	}

	private Map<String, OnClickListener> onOnClickListeners = new HashMap<String, OnClickListener>();
	
	/** Only applies for isRemote == false */
	public OnClickListener getOnClickListener(String viewTag) {
		return onOnClickListeners.get(viewTag);
	}
	
}
