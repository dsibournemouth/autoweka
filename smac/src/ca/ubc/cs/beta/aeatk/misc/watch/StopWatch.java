package ca.ubc.cs.beta.aeatk.misc.watch;

import java.io.Serializable;

import net.jcip.annotations.NotThreadSafe;

/**
 * Allows measuring wallclock time between calls to {@link StopWatch#start()} and {@link StopWatch#stop()}
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@NotThreadSafe
public class StopWatch implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9116898201991111651L;
	private long startTime = -1;
	private long endTime = Long.MAX_VALUE;
	private long lastLaps;
	/**
	 * Default constructor
	 */
	public StopWatch()
	{
		
	}
	
	/**
	 * Starts the watch
	 * @return start time in ms
	 */
	public long start()
	{
		if(startTime >= 0)
		{
			throw new IllegalStateException("Watch already started");
		}
		
		startTime = System.currentTimeMillis();
		lastLaps = startTime;
		return startTime;
	}
	
	/**
	 * Stops the watch
	 * @return duration in ms
	 */
	public long stop()
	{
		if (startTime < 0)
		{
			throw new IllegalStateException("Watch hasn't been started");
		} 
			
		endTime = System.currentTimeMillis();
		return endTime - startTime;
	}
	
	/**
	 * Gets the time reading from the watch (either since start, if not stopped, or till stopped
	 * @return duration in ms
	 */
	public long time()
	{
		if(startTime < 0)
		{
			return 0;
		}
		return Math.min(endTime, System.currentTimeMillis()) - startTime;
	}
	 
	
	public long laps()
	{
		long cTime = System.currentTimeMillis();
		long lapTime = cTime - lastLaps;
		lastLaps = cTime;
		return lapTime;
	}
	
	
}
