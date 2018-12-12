package au.com.sensis.whereis.locationsimulator;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;
import au.com.sensis.whereis.locationsimulator.ViewSupport.CancelTransitionException;
import au.com.sensis.whereis.locationsimulator.ViewSupport.SettingsRetriever;
import au.com.sensis.whereis.locationsimulator.service.SimService;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;
import au.com.sensis.whereis.locationsimulator.service.SimStatus.Mode;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler.StatusChangeListener;
import au.com.sensis.whereis.locationsimulator.transition.DisplayInfo;
import au.com.sensis.whereis.locationsimulator.transition.Transition;

public class SimActivity extends Activity implements StatusChangeListener, SettingsRetriever {
	
	private static final String TAG = SimActivity.class.getName();
	
	private ViewSupport viewSupport;
	private SimSettings simSettings = new SimSettings();
	private SimStatus currentSimStatus;
	
	private Transition getTransitionForStatus(String transitionPrefix) {
		Transition transition = Transition.getTransitionForStatusAndPrefix(transitionPrefix, currentSimStatus);
		if (transition == null)
			throw new RuntimeException("cannot getTransitionForStatus(" + transitionPrefix + "), currentSimStatus=" + currentSimStatus);
		return transition;
	}
	
	@Override
	public SimSettings getSettingsForTransition(Transition transition) throws CancelTransitionException {
		return simSettings;
	}
	
    private static final int DIALOG_OVERWRITE_FILE = 1;
    private static final int DIALOG_NULL_DIRECTORY = 2;
    
    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
		Builder builder = new AlertDialog.Builder(this);
    	switch (id) {
    	
    	case DIALOG_NULL_DIRECTORY:
    		builder.setMessage("Download directory not defined (media not available)");
    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.cancel();
    			}
    		});
    		break;
    	
    	case DIALOG_OVERWRITE_FILE:
    		builder.setMessage("File " + simSettings.getFilename() + " exists: overwrite it ?");
    		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.cancel();
    			}
    		});
    		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.cancel();
					recordButtonOnClickListener.onClick(viewSupport.getView("buttonRecord"));
    			}
    		});
    		break;
    	}
		return builder.create();
    }

	
	private OnClickListener recordButtonOnClickListener;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // From http://stackoverflow.com/a/2591311
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sim_activity);
        viewSupport = new ViewSupport(this);
        viewSupport.addView("textViewState", R.id.textViewState);
        viewSupport.addView("textViewElapsed", R.id.textViewElapsed);
        
        viewSupport.addView("layoutActions1", R.id.layoutActions1);
        viewSupport.addView("buttonRecord", R.id.buttonRecord);
        viewSupport.addView("buttonPlayback", R.id.buttonPlayback);
        viewSupport.addView("buttonRemote", R.id.buttonRemote);
        
        viewSupport.addView("layoutActions2", R.id.layoutActions2);
        viewSupport.addView("imageButtonSlower", R.id.imageButtonSlower);
        viewSupport.addView("imageButtonResume", R.id.imageButtonStart);
        viewSupport.addView("imageButtonPause", R.id.imageButtonPause);
        viewSupport.addView("imageButtonStop", R.id.imageButtonStop);
        viewSupport.addView("imageButtonFaster", R.id.imageButtonFaster);
        
        viewSupport.addView("textViewCount", R.id.textViewCount);
        viewSupport.addView("textViewDistance", R.id.textViewDistance);
        viewSupport.addView("textViewMode", R.id.textViewMode);
        viewSupport.addView("textViewFile", R.id.textViewFile);
        viewSupport.addView("textViewTotalDistance", R.id.textViewTotalDistance);
        
        viewSupport.addView("buttonExit", R.id.buttonExit);
        viewSupport.addView("buttonSettings", R.id.buttonSettings);

        // The SettingsRetriever is only needed to return SimSettings for the 
        // RECORD, PLAYBACK, and REMOTE transitions; ie those which goto a new mode.
        viewSupport.setTransition("buttonRecord", Transition.RECORD, this);
        viewSupport.setTransition("buttonPlayback", Transition.PLAYBACK, this);
        viewSupport.setTransition("buttonRemote", Transition.REMOTE, this);

        // Now override the onClickListener to check for existing record file.
        recordButtonOnClickListener = viewSupport.getOnClickListener("buttonRecord");
        
        viewSupport.getView("buttonRecord").setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String directory = simSettings.getDirectory();
				if (TextUtils.isEmpty(directory)) {
					showDialog(DIALOG_NULL_DIRECTORY, null);
				} else {
					boolean fileExists = false;
					String fileName = simSettings.getFilename();
					if (!TextUtils.isEmpty(fileName)) {
						File newFile = new File(directory, fileName);
						fileExists = newFile.exists();
					}
					if (fileExists)
						showDialog(DIALOG_OVERWRITE_FILE);
					else 
						recordButtonOnClickListener.onClick(v);
				}
			}
		});
        
//        viewSupport.setTransition("imageButtonSlower", Transition.SLOWER1, null);
//        viewSupport.setTransition("imageButtonFaster", Transition.FASTER1, null);
        for (String buttonSuffix : new String[] {
        		"Slower", "Faster", "Pause", "Resume", "Stop"
        }) {
        	final String buttonName = "imageButton" + buttonSuffix;
        	final String actionName = buttonSuffix.toUpperCase();
            viewSupport.setOnClickListener(buttonName, new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				Transition transition = getTransitionForStatus(actionName);
    				Intent serviceIntent = SimService.makeServiceIntentForTransition(getBaseContext(), transition, null);
    				startService(serviceIntent);
    			}
    		});
        }

        viewSupport.setOnClickListener("buttonExit", new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceIntent = SimService.makeServiceIntentForTransition(getBaseContext(), null, null);	
				stopService(serviceIntent);
				finish();
			}
		});
        viewSupport.setOnClickListener("buttonSettings", new OnClickListener() {
			@Override
			public void onClick(View v) {
    	        Log.i(TAG, "onClick for settings button(" + Thread.currentThread().getName() + "): calling SettingActivity with simSettings: " + simSettings);
		    	Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
		        intent.putExtra(SimService.SETTINGS_KEY, simSettings);
				startActivityForResult(intent, SettingsActivity.SIM_SETTINGS_REQUEST);
			}
		});
        
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if (requestCode == SettingsActivity.SIM_SETTINGS_REQUEST) {
    		if (intent != null) {
        		simSettings = (SimSettings) intent.getSerializableExtra(SimService.SETTINGS_KEY);
    	        Log.i(TAG, "onActivityResult(resultCode=" + resultCode + ", " + Thread.currentThread().getName() + "): assigned new simSettings: " + simSettings);
    		}
    	}
    }
    
	@Override
	public void onStatusChange(SimStatus simStatus, DisplayInfo displayInfo) {
		// Update the view with fresh status info.
        Log.i(TAG, "onStatusChange(" + Thread.currentThread().getName() + "): status: " + simStatus + ", displayInfo: " + displayInfo);
        currentSimStatus = simStatus;
        
        viewSupport.setTextViewText("textViewState", simStatus.state.name());
        viewSupport.setTextViewText("textViewMode", simStatus.mode.name());
        viewSupport.setTextViewText("textViewElapsed", displayInfo.formattedElapsedTime());
        viewSupport.setTextViewText("textViewCount", displayInfo.locationsCount + "");
        viewSupport.setTextViewText("textViewDistance", displayInfo.formatted(displayInfo.distanceTravelled));

        viewSupport.setTextViewText("textViewFile", 		// Indicates the currently processing file.
        		(simStatus.mode == Mode.STANDBY || simStatus.mode == Mode.REMOTE) ? "N/A" : displayInfo.filePath); 
        
        viewSupport.setTextViewText("textViewTotalDistance", 
        		(simStatus.mode == Mode.PLAYBACK) ? displayInfo.formatted(displayInfo.totalRouteDistance) : "N/A"); 
        
        if (simStatus.mode == Mode.STANDBY) {
            viewSupport.setVisibility("layoutActions1", View.VISIBLE);
            viewSupport.setVisibility("layoutActions2", View.GONE);
            viewSupport.setEnabled("buttonSettings", true);
            viewSupport.setEnabled("buttonExit", true);
            
        } else {
            viewSupport.setVisibility("layoutActions1", View.GONE);
            viewSupport.setVisibility("layoutActions2", View.VISIBLE);
            viewSupport.setEnabled("buttonSettings", false);
            viewSupport.setEnabled("buttonExit", false);
        	List<String> actions = Transition.fetchActionCandidates(simStatus, Transition.Type.USER2);
            for (String buttonSuffix : new String[] {
            		"Slower", "Resume", "Pause", "Stop", "Faster"
            }) {
            	String buttonTag = "imageButton" + buttonSuffix;
            	int visibility = (actions.contains(buttonSuffix.toUpperCase())) ? View.VISIBLE : View.GONE;
            	viewSupport.setVisibility(buttonTag, visibility);
            }
        }
	}

    @Override
    public void onResume() {
        Log.i(TAG, "onResume(" + Thread.currentThread().getName() + ")");
        super.onResume();
        doBindService();
        simSettings.setDirectory(checkMediaMountedAndGetPath());
    }
    
    @Override
    public void onPause() {
        Log.i(TAG, "onPause(" + Thread.currentThread().getName() + ")");
    	doUnbindService();
        super.onPause();
    }

	public String checkMediaMountedAndGetPath() {
    	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
       	   Toast.makeText(this, "ExternalStorageMedia not available (!MEDIA_MOUNTED)", Toast.LENGTH_LONG).show();
       	   return null;
      	}
      	final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
      	path.mkdirs();		// Ensure existence.
      	return path.getAbsolutePath();
	}
    
	// Ref http://developer.android.com/reference/android/app/Service.html#LocalServiceSample

    private SimService boundSimService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnected(" + Thread.currentThread().getName() + "), binding service and adding statusChangeListener");
        	boundSimService = ((SimService.LocalBinder)service).getService();
        	boundSimService.getStatusHandler().addStatusChangeListener(SimActivity.this);
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceDisconnected(" + Thread.currentThread().getName() + "), releasing service");
        	boundSimService = null;
        }
    };
    
    private boolean isBound = false;
    
    void doBindService() {
        bindService(new Intent(SimActivity.this, SimService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    void doUnbindService() {
        if (isBound) {
            unbindService(serviceConnection);
            Log.i(TAG, "doUnbindService(" + Thread.currentThread().getName() + "), releasing service");
        	boundSimService = null;
            isBound = false;
        }
    }

}
