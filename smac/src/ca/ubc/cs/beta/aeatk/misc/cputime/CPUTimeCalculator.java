package ca.ubc.cs.beta.aeatk.misc.cputime;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;


/**
 * Helper class that computes and keeps track of CPU Time for us
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
class CPUTimeCalculator {

private static final Logger log = LoggerFactory.getLogger(CPUTime.class);
	
	
	/**
	 * JVM won't give us the CPU time of expired threads so we need to keep track of it.
	 */
	private static final ConcurrentHashMap<Long, Long> threadToCPUTimeMap = new ConcurrentHashMap<Long, Long>();
	
	/**
	 * JVM won't give us the User Time of expired threads so we need to keep track of it.
	 */
	private static final ConcurrentHashMap<Long, Long> threadToUserTimeMap = new ConcurrentHashMap<Long, Long>();
	
	private static final AtomicLong cpuTime = new AtomicLong(0);
	
	private static final AtomicLong userTime = new AtomicLong(0);
	
	private static final ScheduledExecutorService execService = Executors.newScheduledThreadPool(1, new SequentiallyNamedThreadFactory("CPU Time Accumulator", true));	
	
	private static final ThreadUpdater threadUpdate = new ThreadUpdater();
	
	/**
	 * For subsequent calls to getCPUTime() and getUserTime() we don't want the same CPUTime or same UserTime to be returned (unless that's what the OS says),
	 * so we essentially put latches in this queue when a caller calls the methods, and then whenever there is an update we will take one latch and release it.
	 * 
	 * Callers will also schedule a ThreadUpdater to run right away anyway so this shouldn't result in too much latency
	 */
	private static final LinkedBlockingQueue<CountDownLatch> latches = new LinkedBlockingQueue<CountDownLatch>();
	
	
	static
	{
		execService.scheduleAtFixedRate(threadUpdate, 500, 1000, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Helper thread that essentially loops over every thread and updates the time in the shared map
	 */
	private static class ThreadUpdater implements Runnable
	{
			@Override
			public synchronized void run()
			{

				try {
						ThreadMXBean b = ManagementFactory.getThreadMXBean();
						try 
						{
							for(long threadID : b.getAllThreadIds())
							{
								long threadTime =  b.getThreadCpuTime(threadID);
								
								if(threadTime == -1)
								{ //This JVM doesn't have CPU time enabled
							      //We check every iteration because some threads (the current thread may give us something other than -1)
									
									log.trace("JVM didn't give us a measurement for thread: {}", threadID);
									continue;
								} else
								{
									threadToCPUTimeMap.put(threadID, threadTime);
								}
								
								long threadUserTime = b.getThreadUserTime(threadID);
								if(threadUserTime == -1)
								{
									log.trace("JVM didn't give us a measurement for usertime of thread: {}", threadID);
									continue;
								} else
								{
									threadToUserTimeMap.put(threadID, threadTime);
								}
				
							}
							
							long currentCPUTime = 0;
							
							for(Entry<Long, Long> values : threadToCPUTimeMap.entrySet())
							{
								currentCPUTime += values.getValue(); 
							}

							cpuTime.set(currentCPUTime);

							long currentUserTime = 0;
							
							for(Entry<Long, Long> values : threadToCPUTimeMap.entrySet())
							{
								currentUserTime += values.getValue(); 
							}
							userTime.set(currentUserTime);
							
						} catch(UnsupportedOperationException e)
						{
							
							log.debug("JVM does not support CPU Time measurements");
							cpuTime.set(0);
							userTime.set(0);
						}
						
					while(latches.peek() != null)
					{
						latches.poll().countDown();
					}
					
				} catch(RuntimeException e)
				{
					log.error("Exception in CPU Time Thread", e);
				}
				
			
			}
			
	};
	
	/**
	 * Returns the total CPU Time for this JVM
	 * 
	 * @return cpu time for this jvm if enabled&supported 0 otherwise
	 */
	static double _getCPUTime()
	{
			CountDownLatch latch = new CountDownLatch(1);
			boolean accepted = latches.offer(latch);

			execService.execute(threadUpdate);

			if(accepted)
			{
				try {
					latch.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			
			double value =  cpuTime.get() / 1000.0 / 1000.0 / 1000.0;
		
			return value;
	}
	
	/**
	 * Returns the total CPU Time for this JVM
	 * 
	 * @return cpu time for this jvm if enabled&supported 0 otherwise
	 */
	static double _getUserTime()
	{
			CountDownLatch latch = new CountDownLatch(1);
			boolean accepted = latches.offer(latch);

			execService.execute(threadUpdate);
			if(accepted)
			{
				try {
					latch.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			double value =  userTime.get() / 1000.0 / 1000.0 / 1000.0;
			return value;
	}
	
	

	
	
	
}
