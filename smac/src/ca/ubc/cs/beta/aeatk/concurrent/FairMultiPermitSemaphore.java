package ca.ubc.cs.beta.aeatk.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.ThreadSafe;

/**
 * This basically allows multiple permits to be grabbed at the same time,
 * and to get all available permits without blocking, but block if none are available.
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class FairMultiPermitSemaphore {

	private final Semaphore semaphore;
	private final AtomicInteger permits;
	
	
	/**
	 * Lock object used to make sure increments are mutually exclusive
	 */
	private final ReentrantLock mutexLock= new ReentrantLock();
	
	public FairMultiPermitSemaphore(int numberOfPermits)
	{
		
		this.semaphore = new Semaphore(numberOfPermits);
		this.permits = new AtomicInteger(numberOfPermits);
	}
	
	/***
	 * Gets up to N Permits
	 * <p>
	 * Implementation Note: We only want one thread at a time aquiring permits, so they are all blocked
	 * outside of this method. Then we want to ensure that nothing touches our permits and semaphore data structure
	 * so we mutexLock that. The loop is overkill at this point (as this method is synchronized) but it's a compare and swap on the values to make sure it didn't change.
	 *  
	 * @param N - number of permits to attempt to aquire.
	 * @return 1 or more permits when available
	 * @throws InterruptedException
	 */
	public synchronized int getUpToNPermits(int N) throws InterruptedException
	{
		if(N < 0) throw new IllegalArgumentException("Must get a non-negative amount of permits");
		if(N == 0) return 0;
		//System.out.println("A1:" + permits.get() + "," + semaphore.availablePermits() + "," + N);
		semaphore.acquire();
		
		int availablePermits;
		int grabbedPermits;
		int remainingPermits;
		try
		{
			//System.out.println("A2:" + permits.get() + "," + semaphore.availablePermits() + "," + N);
			this.mutexLock.lock();
			//Lock to ensure that I see a consistent number of availablePermits and semaphores can only increase
			do {
				availablePermits = permits.get();
				grabbedPermits = Math.min(N, availablePermits);
				
				remainingPermits = availablePermits - grabbedPermits;
			} while(!permits.compareAndSet(availablePermits, remainingPermits));
			
			if(semaphore.availablePermits() < grabbedPermits - 1) //We already grabbed 1 permit
			{
				throw new IllegalStateException("Concurrency Bug, at this point I should be able to grab the remaining permits " + semaphore.availablePermits() + " but I have allegedly grabbed " + (grabbedPermits - 1));
			}
		} finally
		{
			//System.out.println("A3:" + permits.get() + "," + semaphore.availablePermits() + "," + N);
			this.mutexLock.unlock();
		}
		
		//Semaphores can only increase when we unlock and now.
		semaphore.acquire(grabbedPermits - 1);
		//System.out.println("A4:" + permits.get() + "," + semaphore.availablePermits() + "," + N);
		return grabbedPermits;
	}
	
	/***
	 * Releases N permits back to the pool
	 * 
	 * @param N - releases N permits back to the pool
	 */
	public void releasePermits(int N)
	{
		if(N < 0)
		{
			throw new IllegalArgumentException("You cannot release a negative number of permits");
		}
		if( N == 0)
		{
			return;
		}
		this.mutexLock.lock();
		try {
			//System.out.println("R-:" +permits.get() + "," + semaphore.availablePermits() + "," + N);
			permits.addAndGet(N);
			semaphore.release(N);
			//System.out.println("R+:" +permits.get() + "," + semaphore.availablePermits() + "," + N);
		} finally {
			this.mutexLock.unlock();
		}
	}
	
	public int availablePermits()
	{
		return this.semaphore.availablePermits();
	}

	public void drainPermits() {
		 this.semaphore.drainPermits();		
	}
}
