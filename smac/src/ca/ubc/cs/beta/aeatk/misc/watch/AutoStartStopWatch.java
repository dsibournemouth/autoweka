package ca.ubc.cs.beta.aeatk.misc.watch;

/**
 * {@link StopWatch} that is already started
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class AutoStartStopWatch extends StopWatch {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1024405932333437402L;

	/**
	 * Creates the StopWatch
	 */
	public AutoStartStopWatch()
	{
		super();
		start();
	}

}
