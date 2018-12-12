package au.com.sensis.whereis.locationsimulator.transition;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import au.com.sensis.whereis.locationsimulator.service.StatusHandler;

public abstract class TransitionHandlerThread extends HandlerThread {
	// Ref http://stackoverflow.com/a/4855788
	
	private static final String TAG = TransitionHandlerThread.class.getName();
	
	protected StatusHandler statusHandler;		// Owned in creating thread.
	private Handler transitionHandler;			// Owned in this thread.
	
	private Handler timerHandler;				

	public abstract void handleTransition(Transition transition);
	
	protected static final long TIMER_PERIOD_UNCHANGED = 0;
	protected static final long TIMER_CANCEL = -1;
	
	
	/** Return a indication of when the next timer event should occur.
	 * Returning TIMER_PERIOD_UNCHANGED causes the next timed invocation
	 * to occur in the same interval as the original call to startTimer.
	 * Returning TIMER_CANCEL causes no more invocations, and returning any
	 * other value will arrange for the next call in that many millisecs.
	 */
	public abstract long onTimer();			
	
	public synchronized void waitUntilReady() {
		transitionHandler = new Handler(getLooper(), new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Transition transition = (Transition)msg.obj;
				Log.i(TAG, "handleMessage(" + Thread.currentThread().getName() + "): transition: " + transition);		
				handleTransition(transition);
				return false;
			}
		});
	}

	public TransitionHandlerThread(String name, StatusHandler statusHandler) {
		super(name);
		this.statusHandler = statusHandler;
		Log.i(TAG, "created TransitionHandlingThread(" + Thread.currentThread().getName() + ")");		
	}
	
	/**
	 * Called in the old thread (which called ctor) to pass transition to the new thread.
	 */
	public void doTransition(Transition transition) {
		Log.i(TAG, "doTransition(" + Thread.currentThread().getName() + "): transition: " + transition);		
		Message message = Message.obtain(transitionHandler, 0, 0, 0, transition);
		transitionHandler.sendMessage(message);
	}
	
	@Override
	public boolean quit() {
		Log.i(TAG, "quit(" + Thread.currentThread().getName() + ")");		
		return super.quit();
	}
	
	/**
	 * Arrange for the onTimer() method to be called in initialTimerPeriod millisecs.
	 * Each invocation of onTimer() can use its return value to determine when or if
	 * another timed invocation should be arranged.
	 */
	protected void startTimer(final int initialTimerPeriod) {
        Log.i(TAG, "startTimer initialTimerPeriod=" + initialTimerPeriod);
        timerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				long nextTimerPeriod = onTimer();
				if (nextTimerPeriod != TIMER_CANCEL) {
					if (nextTimerPeriod != TIMER_PERIOD_UNCHANGED) {
				        Log.i(TAG, "handleMessage timer period changed to " + nextTimerPeriod);
					} else
						nextTimerPeriod = initialTimerPeriod;
					sendEmptyMessageDelayed(0, nextTimerPeriod);
				}
			}
		};
		timerHandler.sendEmptyMessageDelayed(0, initialTimerPeriod);
	}

	/**
	 * Cancel any pending calls to onTimer().
	 */
	protected void cancelTimer() {
		if (timerHandler != null) {
			timerHandler.removeMessages(0);
	        Log.i(TAG, "cancelTimer");
		}
		timerHandler = null;
	}
	
}
