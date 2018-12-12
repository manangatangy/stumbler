package au.com.sensis.whereis.locationsimulator;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import au.com.sensis.whereis.locationsimulator.service.SimService;
import au.com.sensis.whereis.locationsimulator.service.SimSettings;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.FileType;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.RecordObtainType;
import au.com.sensis.whereis.locationsimulator.service.SimSettings.RecordProviderType;

public class SettingsActivity extends Activity {

	public static final int SIM_SETTINGS_REQUEST = 1;
	private static final String TAG = SimActivity.class.getName();
	
	private SimSettings simSettings;

	private EditText editTextFilename;
	private EditText editTextRecordPollingPeriod;
	private EditText editTextPlaybackMinPeriod;
	private EditText editTextPlaybackInterpolationPeriod;
	private EditText editTextPlaybackPeriodWhilePaused;
	private EditText editTextPlaybackSkipInterval;
	private EditText editTextRemoteServerPort;
	private EditText editTextRecordMinCallbackTime;
	private EditText editTextRecordMinCallbackDistance;
	
	private TextView textViewRecordAccuracy;
    private CheckBox checkboxRecordAccuracy;
    private CheckBox checkboxRecordAltitude;
    private CheckBox checkboxRecordBearing;
    private CheckBox checkboxRecordSpeed;
    private CheckBox checkboxBeepOnLocationRecorded;
	
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // From http://stackoverflow.com/a/2591311
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sim_settings);
        
    	simSettings = (SimSettings) getIntent().getSerializableExtra(SimService.SETTINGS_KEY);
        Log.i(TAG, "onCreate simSettings = " + simSettings);
    	
    	editTextFilename = (EditText) findViewById(R.id.editTextFilename);
    	editTextRecordPollingPeriod = (EditText) findViewById(R.id.editTextRecordPollingPeriod);
    	editTextPlaybackMinPeriod = (EditText) findViewById(R.id.editTextPlaybackMinPeriod);
    	editTextPlaybackInterpolationPeriod = (EditText) findViewById(R.id.editTextPlaybackInterpolationPeriod);
    	editTextPlaybackPeriodWhilePaused = (EditText) findViewById(R.id.editTextPlaybackPeriodWhilePaused);
    	editTextPlaybackSkipInterval = (EditText) findViewById(R.id.editTextPlaybackSkipInterval);
    	editTextRecordMinCallbackTime = (EditText) findViewById(R.id.editTextRecordMinCallbackTime);
    	editTextRecordMinCallbackDistance = (EditText) findViewById(R.id.editTextRecordMinCallbackDistance);

    	editTextRemoteServerPort = (EditText) findViewById(R.id.editTextRemoteServerPort);

    	textViewRecordAccuracy = (TextView) findViewById(R.id.textViewRecordAccuracy);
    	checkboxRecordAccuracy = (CheckBox) findViewById(R.id.checkboxRecordAccuracy);
    	checkboxRecordAltitude = (CheckBox) findViewById(R.id.checkboxRecordAltitude);
    	checkboxRecordBearing = (CheckBox) findViewById(R.id.checkboxRecordBearing);
    	checkboxRecordSpeed = (CheckBox) findViewById(R.id.checkboxRecordSpeed);
    	checkboxBeepOnLocationRecorded = (CheckBox) findViewById(R.id.checkboxBeepOnLocationRecorded);
    	
    	TextWatcher integerFieldWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable editable) {
				for (int i = editable.length() - 1; i >= 0; i--) {
					if (!Character.isDigit(editable.charAt(i))) {
						editable.delete(i, i - 1);
					}
					if (editable.length() <= 0)
						editable.append('0');
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
    	};

    	editTextRecordPollingPeriod.addTextChangedListener(integerFieldWatcher);
    	editTextPlaybackMinPeriod.addTextChangedListener(integerFieldWatcher);
    	editTextPlaybackInterpolationPeriod.addTextChangedListener(integerFieldWatcher);
    	editTextPlaybackPeriodWhilePaused.addTextChangedListener(integerFieldWatcher);
    	editTextPlaybackSkipInterval.addTextChangedListener(integerFieldWatcher);
    	editTextRemoteServerPort.addTextChangedListener(integerFieldWatcher);
    	editTextRecordMinCallbackTime.addTextChangedListener(integerFieldWatcher);
    	editTextRecordMinCallbackDistance.addTextChangedListener(integerFieldWatcher);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	editTextFilename.setText(simSettings.getFilename());
    	editTextRecordPollingPeriod.setText(simSettings.getRecordPollingPeriod() + "");
    	editTextPlaybackMinPeriod.setText(simSettings.getPlaybackMinimumPeriod() + "");
    	editTextPlaybackInterpolationPeriod.setText(simSettings.getPlaybackInterpolationPeriod() + "");
    	editTextPlaybackPeriodWhilePaused.setText(simSettings.getPlaybackPausedPlaybackPeriod() + "");
    	editTextPlaybackSkipInterval.setText(simSettings.getSkipInterval() + "");
    	editTextRemoteServerPort.setText(simSettings.getRemoteServerPort() + "");
    	editTextRecordMinCallbackTime.setText(simSettings.getRecordMinCallbackTime() + "");
    	editTextRecordMinCallbackDistance.setText(simSettings.getRecordMinCallbackDistance() + "");

    	((RadioButton) findViewById(R.id.radioProviderTypeGps)).setChecked(simSettings.getRecordProviderType() == RecordProviderType.GPS);
    	((RadioButton) findViewById(R.id.radioProviderTypeNetwork)).setChecked(simSettings.getRecordProviderType() == RecordProviderType.NETWORK);
    	
    	((RadioButton) findViewById(R.id.radioFileTypeGpx)).setChecked(simSettings.getRecordFileType() == FileType.GPX);
    	((RadioButton) findViewById(R.id.radioFileTypeCustom)).setChecked(simSettings.getRecordFileType() == FileType.CUSTOM);
    	setEnabledExtraCheckBoxes(simSettings.getRecordFileType() == FileType.CUSTOM);

    	((RadioButton) findViewById(R.id.radioRecordByPoll)).setChecked(simSettings.getRecordObtainType() == RecordObtainType.BY_POLLING);
    	((RadioButton) findViewById(R.id.radioRecordByCallback)).setChecked(simSettings.getRecordObtainType() == RecordObtainType.BY_CALLBACK);
    	
    	checkboxRecordAccuracy.setChecked(simSettings.isRecordAccuracy());
    	checkboxRecordAltitude.setChecked(simSettings.isRecordAltitude());
    	checkboxRecordBearing.setChecked(simSettings.isRecordBearing());
    	checkboxRecordSpeed.setChecked(simSettings.isRecordSpeed());
    	checkboxBeepOnLocationRecorded.setChecked(simSettings.isBeepOnLocationRecorded());

    	((CheckBox) findViewById(R.id.checkboxPlaybackInterpolate)).setChecked(simSettings.isPlaybackInterpolate());
    	((CheckBox) findViewById(R.id.checkboxPlaybackSynthesizeBearings)).setChecked(simSettings.isSynthesizePlaybackBearings());
    	
    	// Could also use View.performClick()
    	editTextPlaybackInterpolationPeriod.setEnabled(simSettings.isPlaybackInterpolate());
    	setEnabledPollOrCallback(simSettings.getRecordObtainType() == RecordObtainType.BY_POLLING);
    }
    
    private void setEnabledPollOrCallback(boolean enabledPolling) {
    	editTextRecordPollingPeriod.setEnabled(enabledPolling);
    	editTextRecordMinCallbackTime.setEnabled(!enabledPolling);
    	editTextRecordMinCallbackDistance.setEnabled(!enabledPolling);
    }
    
    @Override
    public void onBackPressed() {
    	returnSettingsResult();
    }
    
    public void returnSettingsResult() {
    	String fileName = editTextFilename.getText().toString().trim();
    	if (!fileName.endsWith(FileType.GPX.getSuffix()) && !fileName.endsWith(FileType.CUSTOM.getSuffix())) {
    		fileName = fileName + simSettings.getRecordFileType().getSuffix();
    	}
    	simSettings.setFilename(fileName);
    	
    	simSettings.setRecordPollingPeriod(parseInteger(editTextRecordPollingPeriod.getText().toString()));
    	simSettings.setPlaybackMinimumPeriod(parseInteger(editTextPlaybackMinPeriod.getText().toString()));
    	simSettings.setPlaybackInterpolationPeriod(parseInteger(editTextPlaybackInterpolationPeriod.getText().toString()));
    	simSettings.setPlaybackPausedPlaybackPeriod(parseInteger(editTextPlaybackPeriodWhilePaused.getText().toString()));
    	simSettings.setSkipInterval(parseInteger(editTextPlaybackSkipInterval.getText().toString()));
    	simSettings.setRecordMinCallbackDistance(parseInteger(editTextRecordMinCallbackDistance.getText().toString()));
    	simSettings.setRecordMinCallbackTime(parseInteger(editTextRecordMinCallbackTime.getText().toString()));
    	
    	simSettings.setRemoteServerPort(parseInteger(editTextRemoteServerPort.getText().toString()));
    	
        Log.i(TAG, "returnSettingsResult simSettings = " + simSettings);
    	Intent intent = new Intent(getBaseContext(), SimActivity.class);
        intent.putExtra(SimService.SETTINGS_KEY, simSettings);
    	setResult(RESULT_OK, intent);
    	finish();
    }

    private int parseInteger(String field) {
    	return Integer.parseInt(field);
    }
    
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
        case R.id.radioProviderTypeGps:
        	simSettings.setRecordProviderType(RecordProviderType.GPS);
        	break;
        case R.id.radioProviderTypeNetwork:
        	simSettings.setRecordProviderType(RecordProviderType.NETWORK);
        	break;
        case R.id.radioFileTypeGpx:
        	simSettings.setRecordFileType(FileType.GPX);
        	setEnabledExtraCheckBoxes(!checked);
        	substituteFilenameSuffix(FileType.CUSTOM.getSuffix(), FileType.GPX.getSuffix());
        	break;
        case R.id.radioFileTypeCustom:
        	simSettings.setRecordFileType(FileType.CUSTOM);
        	setEnabledExtraCheckBoxes(checked);
        	substituteFilenameSuffix(FileType.GPX.getSuffix(), FileType.CUSTOM.getSuffix());
        	break;
        case R.id.radioRecordByPoll:
        	simSettings.setRecordObtainType(RecordObtainType.BY_POLLING);
        	setEnabledPollOrCallback(true);
        	break;
        case R.id.radioRecordByCallback:
        	simSettings.setRecordObtainType(RecordObtainType.BY_CALLBACK);
        	setEnabledPollOrCallback(false);
        	break;
        }    	
    }

    private void substituteFilenameSuffix(String target, String replacement) {
    	String fileName = editTextFilename.getText().toString().trim();
    	if (fileName.endsWith(target)) {
    		fileName = fileName.replace(target, replacement);
    	} else {
    		fileName = fileName + replacement;
    	}
		editTextFilename.setText(fileName);
    }
    
    private void setEnabledExtraCheckBoxes(boolean enabled) {
    	textViewRecordAccuracy.setEnabled(enabled);
    	checkboxRecordAccuracy.setEnabled(enabled);
    	checkboxRecordAltitude.setEnabled(enabled);
    	checkboxRecordBearing.setEnabled(enabled);
    	checkboxRecordSpeed.setEnabled(enabled);
    }
    
    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
        case R.id.checkboxRecordAccuracy:
        	simSettings.setRecordAccuracy(checked);
        	break;
        case R.id.checkboxRecordAltitude:
        	simSettings.setRecordAltitude(checked);
        	break;
        case R.id.checkboxRecordBearing:
        	simSettings.setRecordBearing(checked);
        	break;
        case R.id.checkboxRecordSpeed:
        	simSettings.setRecordSpeed(checked);
        	break;
        case R.id.checkboxPlaybackInterpolate:
        	simSettings.setPlaybackInterpolate(checked);
        	editTextPlaybackInterpolationPeriod.setEnabled(checked);
        	break;
        case R.id.checkboxPlaybackSynthesizeBearings:
        	simSettings.setSynthesizePlaybackBearings(checked);
        	break;
        case R.id.checkboxBeepOnLocationRecorded:
        	simSettings.setBeepOnLocationRecorded(checked);
        	break;
        }
    }
    
    private static final int DIALOG_EMPTY_DIRECTORY = 1;
    private static final int DIALOG_FILES_LIST = 2;
    private static final int DIALOG_NULL_DIRECTORY = 3;
    
    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
		Builder builder = new AlertDialog.Builder(this);
    	switch (id) {
    	case DIALOG_EMPTY_DIRECTORY:
    	case DIALOG_NULL_DIRECTORY:
    		builder.setMessage((id == DIALOG_EMPTY_DIRECTORY) ? "File list is empty" : "Directory not defined (media not available)");
    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.cancel();
    			}
    		});
    		break;
    	case DIALOG_FILES_LIST:
    		final String[] files = args.getStringArray("FILES_LIST");
    		builder.setTitle("Select file");
    		builder.setItems(files, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    		        editTextFilename.setText(files[item]);
    		    }
    		});
    		break;    		
    	}
		return builder.create();
    }

	/**
	 * Pops a dialog asking the user to select one file (with the specified prefix)from the 
	 * download directory. If there are no candidate files, pops a dialog advising of same.
	 */
    // TODO - check out  http://stackoverflow.com/questions/5819847/write-in-file-permission-in-android
	public void onBrowseButtonClicked(View view) {
		String directory = simSettings.getDirectory();
		if (TextUtils.isEmpty(directory)) {
			showDialog(DIALOG_NULL_DIRECTORY, null);
		} else {
			File downLoadDir = new File(directory);
			final String[] files = downLoadDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(simSettings.getRecordFileType().getSuffix());
				}
			});
	        Log.i(TAG, "chooseFile: " + files.length + " files with suffix: " 
	        		+ simSettings.getRecordFileType().getSuffix() + ", in directory: " + simSettings.getDirectory());
	        
			if (files.length == 0) {
				showDialog(DIALOG_EMPTY_DIRECTORY, null);
			} else {
		        Bundle bundle = new Bundle();
		        bundle.putSerializable("FILES_LIST", files);
		        removeDialog(DIALOG_FILES_LIST);
		        showDialog(DIALOG_FILES_LIST, bundle);
			}
		}
	}

}
