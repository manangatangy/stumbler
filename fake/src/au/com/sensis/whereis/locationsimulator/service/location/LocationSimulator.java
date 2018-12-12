package au.com.sensis.whereis.locationsimulator.service.location;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.util.Log;

public class LocationSimulator {

	private static final String TAG = LocationSimulator.class.getName();
	private static final int UPDATE_LOCATION_WAIT_TIME = 1000;
	private LocationManager mLocationManager;
	private String providerName;
	
	public boolean init(Context context, String providerName) {

		this.providerName = providerName;
		if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 0) {
			// Must go into Settings | Applications | Development | Allow Mock Locations and enable it.
			// Add to AndroidManifest (for test-app) <uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" /> 
			return false;
		}

		// Ref http://androidcookbook.com/Recipe.seam;jsessionid=693B87150B6AE2B3442695D66936A30D?recipeId=1229
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		Log.i(TAG, "adding test provider: " + providerName);
		mLocationManager.addTestProvider(providerName, 
				false, //requiresNetwork,
				false, // requiresSatellite,
				false, // requiresCell,
				false, // hasMonetaryCost,
				true, // supportsAltitude,
				true, // supportsSpeed, 
				true, // supportsBearing,
				Criteria.POWER_MEDIUM, // powerRequirement
				Criteria.ACCURACY_FINE); // accuracy

		Log.i(TAG, "enabling test provider: " + providerName);
		mLocationManager.setTestProviderEnabled(providerName, true);
		mLocationManager.setTestProviderStatus(providerName, LocationProvider.AVAILABLE, null, System.currentTimeMillis());    
		return true;
	}
	
	public void finish() {
		Log.i(TAG, "removing test provider: " + providerName);
		try {
			if (mLocationManager != null) {
				mLocationManager.clearTestProviderLocation(providerName);
				mLocationManager.clearTestProviderStatus(providerName);
				mLocationManager.clearTestProviderEnabled(providerName);
				mLocationManager.removeTestProvider(providerName);
			}
		} catch (SecurityException sex) {
			// Ignore - if ACCESS_MOCK_LOCATION is not set it will already have been detected and reported.
		}
	}
	
    public void asynchronousSendLocation(final double latitude, final double longitude) {
    	asynchronousSendLocation(new Location(latitude, longitude, 0));
    }	
    public void synchronousSendLocation(final double latitude, final double longitude) throws InterruptedException {
    	synchronousSendLocation(new Location(latitude, longitude, 0));
    }
	
    public void asynchronousSendLocation(final Location loc) {
    	updateLocation(loc, null);
    }	
    public void synchronousSendLocation(final Location loc) throws InterruptedException {
    	updateLocation(loc, this);
        synchronized (this) {
            wait(UPDATE_LOCATION_WAIT_TIME);
        }
    }

    private static final float DEFAULT_ACCURACY = 1.0f;
    private static final float DEFAULT_ALTITUDE = 1.0f;
    private static final float DEFAULT_BEARING = 1.0f;
    private static final float DEFAULT_SPEED = 1.0f;
    
    /**
     * The minimum fields used from the Location are lat and lon.  If others (altitude, accuracy, 
     * bearing, speed) are present, they will be used, else default altitude, accuracy, bearing 
     * and speed will be used. The Location.getTime() is never used - always the current time is
     * used.
     */
    private void updateLocation(final Location loc, final Object observer) {
        Thread locationUpdater = new Thread() {
            @Override
            public void run() {
            	android.location.Location aloc = new android.location.Location(providerName);
                aloc.setLatitude(loc.getLat());
                aloc.setLongitude(loc.getLon());
                aloc.setAccuracy(loc.hasAccuracy() ? loc.getAccuracy() : DEFAULT_ACCURACY);
                aloc.setAltitude(loc.hasAltitude() ? loc.getAltitude() : DEFAULT_ALTITUDE);
                aloc.setBearing(loc.hasBearing() ? loc.getBearing() : DEFAULT_BEARING);
                aloc.setSpeed(loc.hasSpeed() ? loc.getSpeed() : DEFAULT_SPEED);
                aloc.setTime(java.lang.System.currentTimeMillis());
    			Log.i(TAG, "setting location to: " + loc);
                mLocationManager.setTestProviderLocation(providerName, aloc);
                if (observer != null) {
                    synchronized (observer) {
                        observer.notify();
                    }
                }
            }
        };
        locationUpdater.start();
    }

}
