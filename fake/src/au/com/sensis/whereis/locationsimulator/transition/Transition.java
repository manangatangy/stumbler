package au.com.sensis.whereis.locationsimulator.transition;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.SimStatus;

/**
 * List all the user initiated transitions.
 */
public enum Transition {
	
	// name		type			nType	initMode			initState		state-change
	//
	RECORD		(Type.USER1,	true,	SimStatus.STANDBY_STANDBY),		// --> recording
	PAUSE1		(Type.USER2,	true,	SimStatus.RECORDING_RECORDING),	// --> paused
	RESUME1		(Type.USER2,	true,	SimStatus.RECORDING_PAUSED),	// --> recording
	REAL_GPS	(Type.AUTO,		false,	SimStatus.RECORDING_RECORDING),	// --> gps --> recording
	STOP1		(Type.USER2,	true,	SimStatus.RECORDING_RECORDING),	// --> standby
	STOP5		(Type.USER2,	true,	SimStatus.RECORDING_PAUSED),	// --> standby
	
	PLAYBACK	(Type.USER1,	true,	SimStatus.STANDBY_STANDBY),		// --> playback	
	SLOWER1		(Type.USER2,	true,	SimStatus.PLAYBACK_REPLAYING),	// --> skip-backward --> replaying
	SLOWER2		(Type.USER2,	true,	SimStatus.PLAYBACK_PAUSED),		// --> skip-backward --> paused
	PAUSE2		(Type.USER2,	true,	SimStatus.PLAYBACK_REPLAYING),	// --> paused
	RESUME2		(Type.USER2,	true,	SimStatus.PLAYBACK_PAUSED),		// --> replaying 
	FASTER1		(Type.USER2,	true,	SimStatus.PLAYBACK_REPLAYING),	// --> skip-forward --> replaying
	FASTER2		(Type.USER2,	true,	SimStatus.PLAYBACK_PAUSED),		// --> skip-forward --> paused
	OUT_GPS		(Type.AUTO,		false,	SimStatus.PLAYBACK_REPLAYING),	// --> gps --> replaying
	STOP2		(Type.USER2,	true,	SimStatus.PLAYBACK_REPLAYING),	// --> standby
	STOP4		(Type.USER2,	true,	SimStatus.PLAYBACK_PAUSED),		// --> standby
	
	REMOTE		(Type.USER1,	true,	SimStatus.STANDBY_STANDBY),		// --> disconnected	
	CONNECT		(Type.AUTO,		true,	SimStatus.REMOTE_DISCONNECTED),	// --> connected
	IN_GPS		(Type.AUTO,		false,	SimStatus.REMOTE_CONNECTED),	// --> gps --> connected
	PAUSE3		(Type.USER2,	true,	SimStatus.REMOTE_CONNECTED),	// --> disconnected  (was called DISCONNECT)
	STOP3		(Type.USER2,	true,	SimStatus.REMOTE_DISCONNECTED);	// --> standby
	
	// There are several "loop-transitions" which result in no state change and are
	// only used to communicated a request to the service.  
	// The lower case transitions are only initiated by software and are only listed
	// above for completeness.
	
	private static final String TAG = Transition.class.getName();
	
	private Type type;
	private final boolean isFullNotification;
	private final SimStatus initStatus;
	
	Transition(Type type, boolean isFullNotification, SimStatus initStatus) {
		this.type = type;
		this.isFullNotification = isFullNotification;
		this.initStatus = initStatus;
	}

	public String toString() {
		return "{name:" + name() + ", type:" + type + ", fullNote:" + isFullNotification + ", init:" + initStatus + "}";
	}
	
	public boolean isFullNotification() { return isFullNotification; }
	
	public String actionName() {
		int last = this.name().length() - 1;
		return Character.isDigit(this.name().charAt(last)) ? this.name().substring(0, last) : this.name();
	}
	
	/**
	 * For the given transition type (USR1 or USR2), return all possible transitions which 
	 * can occur from the specified starting simStatus.
	 */
	private static List<Transition> fetchCandidates(SimStatus simStatus, Transition.Type type) {
        Log.i(TAG, "fetchCandidates(" + Thread.currentThread().getName() + "): type: " + type + ", status: " + simStatus);
		List<Transition> transitions = new ArrayList<Transition>();
		for (Transition t : Transition.values()) {
			if (t.type == type) {
				if (t.initStatus == simStatus) {
					transitions.add(t);
			        Log.i(TAG, "fetchCandidates: allowed transition: " + t);
				}
			}
		}
		return transitions;
	}
	
	public static List<String> fetchActionCandidates(SimStatus simStatus, Transition.Type type) {
        Log.i(TAG, "fetchActionCandidates(" + Thread.currentThread().getName() + "): type: " + type + ", status: " + simStatus);
		List<String> actions = new ArrayList<String>();
		for (Transition t : fetchCandidates(simStatus, type)) {
			actions.add(t.actionName());
		}
		return actions;
	}
	
	public static Transition getTransitionForStatusAndPrefix(String transitionPrefix, SimStatus startingSimStatus) {
		for (Transition transition : Transition.values()) {
			if (transition.actionName().equals(transitionPrefix)) {
				if (transition.getInitStatus() == startingSimStatus)
					return transition;
			}
		}
		return null;
	}
	
	public SimStatus getInitStatus() {
		return initStatus;
	}
	
	public enum Type {
		AUTO,
		/** First-level actions (shown on activity view). */
		USER1,	
		/** Second-level actions (shown on notification views). */
		USER2
	}

}
