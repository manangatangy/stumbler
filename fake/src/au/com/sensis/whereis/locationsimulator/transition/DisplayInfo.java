package au.com.sensis.whereis.locationsimulator.transition;


public class DisplayInfo {
	
	/** 
	 * Time in millis since start (for record/replay/remote-serving modes).  The timer  
	 * continues in replay mode while Locations are sent, even if route is paused.
	 */
	public long elapsedTime;
	
	/** 
	 * The locationsCount field is a count of either the location recorded or the locations 
	 * sent depending on which mode.
	 * 
	 * locations-recorded: Number of locations fetched from the android locationManager and 
	 * stored in the file. Index into the route of the currently playing location. Doesn't 
	 * change while paused. Only applies for record mode.
	 * 
	 * locations-sent: The total number of locations sent to the locationManager while in 
	 * replay or remote server mode.  This will continue while locations are sent, even if 
	 * the route playback is paused.
	 */
	public int locationsCount;		
	
//	public int count;
//	public String speed;
	
	// Full path to file being record or replayed
	public String filePath;
	
	/** 
	 * Total distance for the complete route. This is only for replay mode and is determined 
	 * at the start and then fixed. 
	 */
	public int totalRouteDistance;		
	
	/** 
	 * Distance in metres for the recorded/replayed/remote-served route from the start.
	 * Reset upon re-starting (for all modes). This member will pause if the route is paused. 
	 */
	public int distanceTravelled;		
	
	public String toString() {
		return "{elapsedTime:" + elapsedTime 
			+ ", locationsCount:" + locationsCount 
			+ ", filePath:" + filePath 
			+ ", totalRouteDistance:" + totalRouteDistance 
			+ ", distanceTravelled:" + distanceTravelled 
			+ "}";
		
	}
	
	public String formatted(int number) {
		int thousands = number / 1000;
		number = number % 1000;
		if (thousands <= 0)
			return number + "";
		return String.format("%d,%03d", thousands, number);
	}
	
	public String formattedElapsedTime() {
		int r = (int)elapsedTime;
		int hours = r / (1000*60*60);
		r = r % (1000*60*60);
		int mins = r / (1000*60);
		r = r % (1000*60);
		int secs = r / 1000;
		return String.format("%02d:%02d:%02d", hours, mins, secs);
	}
}
