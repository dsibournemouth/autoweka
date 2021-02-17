package ca.ubc.cs.beta.aeatk.concurrent;

import java.util.concurrent.Semaphore;

import net.jcip.annotations.ThreadSafe;

/**
 * A standard semaphore that exposes the reducePermits() method so that resources can be
 * taken
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class ReducableSemaphore extends Semaphore
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1594134901120931617L;

	public ReducableSemaphore(int permits) {
		super(permits);

	}
	
	public ReducableSemaphore(int permits, boolean fair) {
		super(permits, fair);

	}

	/**
	 * Reduces the permits available by one
	 */
	public void reducePermits()
	{
		super.reducePermits(1);
	}

	
	/**
	 * Reduces the permits available by the number specified
	 * @param reduction  number of permits to take
	 */
	public void reducePermits(int reduction)
	{
		//NOTE WE SIMPLY WANT TO ENSURE THAT 0 doesn't cause an error, otherwise if the user supplies us garbage we will 
		//let the actual semaphore decide.
		if(reduction != 0)
		{
			super.reducePermits(reduction);
		} 
	
	}
	

}
