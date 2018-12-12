package au.com.sensis.whereis.locationsimulator.service;

public enum SimStatus {

	STANDBY_STANDBY		(Mode.STANDBY, 	State.STANDBY),
	REMOTE_DISCONNECTED	(Mode.REMOTE, 	State.DISCONNECTED),
	REMOTE_CONNECTED	(Mode.REMOTE, 	State.CONNECTED),
	RECORDING_RECORDING	(Mode.RECORDING,State.RECORDING),
	RECORDING_PAUSED	(Mode.RECORDING,State.PAUSED),
	PLAYBACK_REPLAYING	(Mode.PLAYBACK,	State.REPLAYING),
	PLAYBACK_PAUSED		(Mode.PLAYBACK,	State.PAUSED);
		
	public enum Mode {
		STANDBY, RECORDING, PLAYBACK, REMOTE 
	};
	public enum State {
		STANDBY, RECORDING, PAUSED, REPLAYING, DISCONNECTED, CONNECTED
	};
	
	public final Mode mode;
	public final State state;
	
	public String toString() {
		return "{mode:" + mode + ", state:" + state + "}";
	}
	
	private SimStatus(Mode mode, State state) {
		this.mode = mode;
		this.state = state;
	}
	
}
