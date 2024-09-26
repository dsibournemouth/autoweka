package ca.ubc.cs.beta.aeatk.watchdog;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.*;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;

@ThreadSafe
public class LiveThreadWatchDog<K extends AutomaticConfiguratorEvent> implements ThreadWatchDog<K>
{

	
	private volatile long lastEventTimeStamp = Long.MIN_VALUE;
	
	private final Set<Thread> threads = Collections.synchronizedSet(new LinkedHashSet<Thread>());
	
	private final ScheduledExecutorService watchDogThread = Executors.newScheduledThreadPool(2,new SequentiallyNamedThreadFactory("Watch Dog Thread", true));
	
	private volatile boolean init = false;
	
	/**
	 * Amount of time that can go by since we saw the last event
	 */
	private final long lastEventDelayInMS;

	private int delayBetweenChecks; 
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private int delayBeforeShutdownInMS;
	
	
	private final int logThreadActivityFrequencyInSeconds;
	/***
	 * Creates a watchdog that looks at registered threads
	 * 
	 * @param lastEventDelayInSeconds 			how long after we recieve the last event should we wait before suspecting a problem
	 * @param delayBetweenChecksInSeconds		how often we should check if there is a problem
	 * @param delayBeforeShutdownInSeconds		how long after we know there is a problem before we force an exit
	 */
	public LiveThreadWatchDog(long lastEventDelayInSeconds, int delayBetweenChecksInSeconds, int delayBeforeShutdownInSeconds, int logThreadActivityFrequencyInSeconds)
	{
		if(delayBeforeShutdownInSeconds < 0) throw new IllegalArgumentException("Delay Before Shutdown must be non-negative");
		if(lastEventDelayInSeconds < 0 ) throw new IllegalArgumentException("Last Event Delay in Seconds must be non-negative");
		if(delayBetweenChecksInSeconds < 0 ) throw new IllegalArgumentException("Delay Between CHecks in Seconds must be positive");
		if(logThreadActivityFrequencyInSeconds < 0) throw new IllegalArgumentException("LogThreadActivityMust be greater than zero");
		
		this.lastEventDelayInMS = lastEventDelayInSeconds  * 1000;
		this.delayBetweenChecks = delayBetweenChecksInSeconds;
		this.delayBeforeShutdownInMS = delayBeforeShutdownInSeconds;
		this.logThreadActivityFrequencyInSeconds = logThreadActivityFrequencyInSeconds;
		
	}
	
	
	@Override
	public synchronized void handleEvent(K event) 
	{
		init();
		lastEventTimeStamp = System.currentTimeMillis();
		
	}

	
	
	@Override
	public synchronized void registerCurrentThread()
	{
		init();
		registerThread(Thread.currentThread());
	}
	
	
	@Override
	public synchronized void registerThread(Thread t)
	{
		init();
		threads.add(t);
	}
	
	
	
	private void init()
	{
		if(init) return;
		init = true;
		//Schedule this outside of the constructor
		watchDogThread.scheduleAtFixedRate(new Runnable()
		{

			@Override
			public void run() {
				
				//So that we get clean reads of the class
				synchronized(LiveThreadWatchDog.this)
				{
				
					if(System.currentTimeMillis() - lastEventTimeStamp < lastEventDelayInMS)
					{
						return;
					} else
					{
						
						
						synchronized(threads)
						{
							boolean deadThread = false;
							for(Thread t : threads)
							{
								deadThread = deadThread || !t.isAlive();
							}
							

							
							if(!deadThread)
							{

								return;
							} else
							{
								log.error("Dead Thread Detected");
								
								
								for(Thread t : threads)
								{
									StringBuilder sb = new StringBuilder();
									for(StackTraceElement el : t.getStackTrace())
									{
										sb.append(el.toString() + "\n");
									}
									Object[] args = { t.getName() , t.getId() , t.getState() , t.isInterrupted(), sb.toString() };
									log.error("Thread: {} [{}] State: {} Interrupted: {}\nStack Trace:\n {}", args);
									
									t.interrupt();
								}
								
								ThreadMXBean b = ManagementFactory.getThreadMXBean();
								
								
								ThreadInfo[] infos = b.getThreadInfo(b.getAllThreadIds(), true, true);
									
								log.error("Dumping all thread info");
								for(ThreadInfo info : infos)
								{
									log.error("ThreadInfo: " , info);
								}
								
								try 
								{
									if(delayBeforeShutdownInMS > 0)
									{
										Thread.sleep(delayBeforeShutdownInMS);									
									}
								} catch(InterruptedException e)
								{
									Thread.currentThread().interrupt();
									return;
								}
								
								
								if(Thread.currentThread().isInterrupted() )
								{
									return;
								} else
								{
									System.exit(AEATKReturnValues.DEADLOCK_DETECTED);
								}
								
								
							}
						
							
							
							
							
							
						}
					}
				}
				
			}
			
		}, delayBetweenChecks, delayBetweenChecks, TimeUnit.SECONDS);
		
		
		watchDogThread.scheduleAtFixedRate(new Runnable()
		{

			@Override
			public void run() {
				
				//So that we get clean reads of the class
				synchronized(LiveThreadWatchDog.this)
				{
					synchronized(threads)
					{
						
						for(Thread t : threads)
						{
							StringBuilder sb = new StringBuilder();
							for(StackTraceElement el : t.getStackTrace())
							{
								sb.append("\t").append(el.toString()).append("\n");
							}
							Object[] args = { t.getName() , t.getId() , t.getState() , t.isInterrupted(), sb.toString() };

							log.debug("Thread: {} [{}] State: {} Interrupted: {}\nStack Trace:\n{}", args);
							
							t.interrupt();
						}
						
						ThreadMXBean b = ManagementFactory.getThreadMXBean();
						
						
						ThreadInfo[] infos = b.getThreadInfo(b.getAllThreadIds(), true, true);
						
			            
						log.debug("Dumping all thread info");
						StringBuilder info = new StringBuilder();
						for(ThreadInfo threadInfo : infos)
						{
							info.append("Thread: ").append(threadInfo.getThreadName()).append(" [").append(threadInfo.getThreadId()).append("]");
							info.append(" State: ").append(threadInfo.getThreadState()).append("\n");
							info.append("Lock Name:").append(threadInfo.getLockName()).append("  Owner: ").append(threadInfo.getLockOwnerName()).append( " [").append(threadInfo.getLockOwnerId()).append("]\n");
				            StringBuilder sb = new StringBuilder();
							for(StackTraceElement el : threadInfo.getStackTrace())
							{
								sb.append("\t").append(el.toString()).append("\n");
							}
							
				            info.append("Stack Trace:\n").append(sb); 
							info.append("\n");
						}

						log.debug("Thread Dump {}:",info);
					
					}
				
				}
				
			}
			
		}, logThreadActivityFrequencyInSeconds, logThreadActivityFrequencyInSeconds,TimeUnit.SECONDS);
		
		
		
	}
	
}
