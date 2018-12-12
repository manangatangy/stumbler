package au.com.sensis.whereis.locationsimulator.service;

import java.io.Serializable;

/** Holds data that may be needed by the Transition HandlerThreads
 */
public class SimSettings implements Serializable {
	
	private static final long serialVersionUID = 0L;

	private String filename;
	private String directory;			// Currently fixed at downloads path.
	
	// Remote mode settings
	private FileType recordFileType = FileType.CUSTOM;
	private RecordProviderType recordProviderType = RecordProviderType.GPS;
	private boolean recordAccuracy = true;
	private boolean recordAltitude = true;
	private boolean recordBearing = true;
	private boolean recordSpeed = true;
	private int recordPollingPeriod = 2000;
	private int recordMinCallbackTime = 1000;
	private int recordMinCallbackDistance = 3;
	
	private RecordObtainType recordObtainType = RecordObtainType.BY_CALLBACK;
	private boolean beepOnLocationRecorded = true;
	
	// Playback mode setting
	private int playbackMinimumPeriod = 1000;
	private boolean playbackInterpolate = true;
	private int playbackInterpolationPeriod = 1000;
	private int playbackPausedPlaybackPeriod = 1000;
	private int skipInterval = 60000;
	private boolean synthesizePlaybackBearings = true;
	
	// Remote mode settings
	private int remoteServerPort = 7100;
	
	public String toString() {
		return "{filename:" + filename + ", recordFileType:" + recordFileType 
			+ ", recordProviderType:" + recordProviderType 
			+ ", recordAccuracy:" + recordAccuracy 
			+ ", recordAltitude:" + recordAltitude 
			+ ", recordBearing:" + recordBearing 
			+ ", recordSpeed:" + recordSpeed + ", recordPollingPeriod:" + recordPollingPeriod 
			+ ", recordMinCallbackTime:" + recordMinCallbackTime 
			+ ", recordMinCallbackDistance:" + recordMinCallbackDistance 
			+ ", recordObtainType:" + recordObtainType 
			+ ", beepOnLocationRecorded:" + beepOnLocationRecorded 
			+ ", playbackMinimumPeriod:" + playbackMinimumPeriod + ", playbackInterpolate:" + playbackInterpolate 
			+ ", playbackInterpolationPeriod:" + playbackInterpolationPeriod 
			+ ", playbackPausedPlaybackPeriod:" + playbackPausedPlaybackPeriod 
			+ ", skipInterval:" + skipInterval 
			+ ", synthesizePlaybackBearings:" + synthesizePlaybackBearings + ", remoteServerPort:" + remoteServerPort 
			+ ", filename:" + filename  
			+ "}";
	}
	
	public enum FileType {
		GPX(".gpx"), 
		CUSTOM(".fake");
		
		private String suffix;
		private FileType(String suffix) {
			this.suffix = suffix;
		}
		
		public String getSuffix() {
			return suffix;
		}
	}

	/** If the filename ends with any of the filetype suffixes, then return the name stripped of that suffix. */
	public String getFilenameWithoutExtension() {
		if (filename == null)
			return null;
		String name = filename;
		int length = name.length();
		int end = length;
    	if (name.endsWith(FileType.GPX.getSuffix()))
    		end -= FileType.GPX.getSuffix().length();
    	else if (name.endsWith(FileType.CUSTOM.getSuffix()))
    		end -= FileType.CUSTOM.getSuffix().length();
    	return name.substring(0, end);
	}
	public enum RecordProviderType {
		GPS, NETWORK,
	}

	public enum RecordObtainType {
		BY_POLLING, BY_CALLBACK
	}
	
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getDirectory() {
		return directory;
	}
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public FileType getRecordFileType() {
		return recordFileType;
	}
	public void setRecordFileType(FileType recordFileType) {
		this.recordFileType = recordFileType;
	}
	public RecordProviderType getRecordProviderType() {
		return recordProviderType;
	}
	public void setRecordProviderType(RecordProviderType recordProviderType) {
		this.recordProviderType = recordProviderType;
	}
	public boolean isRecordAccuracy() {
		return recordAccuracy;
	}
	public void setRecordAccuracy(boolean recordAccuracy) {
		this.recordAccuracy = recordAccuracy;
	}
	public boolean isRecordAltitude() {
		return recordAltitude;
	}
	public void setRecordAltitude(boolean recordAltitude) {
		this.recordAltitude = recordAltitude;
	}
	public boolean isRecordBearing() {
		return recordBearing;
	}
	public void setRecordBearing(boolean recordBearing) {
		this.recordBearing = recordBearing;
	}
	public boolean isRecordSpeed() {
		return recordSpeed;
	}
	public void setRecordSpeed(boolean recordSpeed) {
		this.recordSpeed = recordSpeed;
	}
	public int getRecordPollingPeriod() {
		return recordPollingPeriod;
	}
	public void setRecordPollingPeriod(int recordPollingPeriod) {
		this.recordPollingPeriod = recordPollingPeriod;
	}
	public int getRecordMinCallbackTime() {
		return recordMinCallbackTime;
	}
	public void setRecordMinCallbackTime(int recordMinCallbackTime) {
		this.recordMinCallbackTime = recordMinCallbackTime;
	}
	public int getRecordMinCallbackDistance() {
		return recordMinCallbackDistance;
	}
	public void setRecordMinCallbackDistance(int recordMinCallbackDistance) {
		this.recordMinCallbackDistance = recordMinCallbackDistance;
	}
	public RecordObtainType getRecordObtainType() {
		return recordObtainType;
	}
	public void setRecordObtainType(RecordObtainType recordObtainType) {
		this.recordObtainType = recordObtainType;
	}
	public boolean isBeepOnLocationRecorded() {
		return beepOnLocationRecorded;
	}
	public void setBeepOnLocationRecorded(boolean beepOnLocationRecorded) {
		this.beepOnLocationRecorded = beepOnLocationRecorded;
	}
	public int getPlaybackMinimumPeriod() {
		return playbackMinimumPeriod;
	}
	public void setPlaybackMinimumPeriod(int playbackMinimumPeriod) {
		this.playbackMinimumPeriod = playbackMinimumPeriod;
	}
	public boolean isPlaybackInterpolate() {
		return playbackInterpolate;
	}
	public void setPlaybackInterpolate(boolean playbackInterpolate) {
		this.playbackInterpolate = playbackInterpolate;
	}
	public int getPlaybackInterpolationPeriod() {
		return playbackInterpolationPeriod;
	}
	public void setPlaybackInterpolationPeriod(int playbackInterpolationPeriod) {
		this.playbackInterpolationPeriod = playbackInterpolationPeriod;
	}
	public int getPlaybackPausedPlaybackPeriod() {
		return playbackPausedPlaybackPeriod;
	}
	public void setPlaybackPausedPlaybackPeriod(int playbackPausedPlaybackPeriod) {
		this.playbackPausedPlaybackPeriod = playbackPausedPlaybackPeriod;
	}
	public int getSkipInterval() {
		return skipInterval;
	}
	public void setSkipInterval(int skipInterval) {
		this.skipInterval = skipInterval;
	}
	public int getRemoteServerPort() {
		return remoteServerPort;
	}
	public void setRemoteServerPort(int remoteServerPort) {
		this.remoteServerPort = remoteServerPort;
	}
	public void setSynthesizePlaybackBearings(boolean synthesizePlaybackBearings) {
		this.synthesizePlaybackBearings = synthesizePlaybackBearings;
	}
	public boolean isSynthesizePlaybackBearings() {
		return synthesizePlaybackBearings;
	}
	
}
