package ca.ubc.cs.beta.aeatk.eventsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.EventHandlerRuntimeExceptionEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.EventManagerShutdownEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.internal.FlushEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.exceptions.EventFlushDeadLockException;
import ca.ubc.cs.beta.aeatk.eventsystem.exceptions.EventManagerPrematureShutdownException;
import ca.ubc.cs.beta.aeatk.eventsystem.exceptions.EventManagerShutdownException;

/**
 * Event Manager class
 * 
 * Can be used as a Mediator to insulate different aspects of your program from each other.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class EventManager {

	
	/**
	 * Stores a map of the handlers for each class
	 */
	private final ConcurrentHashMap<Class<? extends AutomaticConfiguratorEvent>, List<EventHandler<?>>> handlerMap = new ConcurrentHashMap<Class<? extends AutomaticConfiguratorEvent>, List<EventHandler<?>>>();
	
	
	private transient Logger log = LoggerFactory.getLogger(EventManager.class);
	
	/**
	 * Queue that has Runnables for processing each handler notification
	 */
	private final LinkedBlockingQueue<Runnable> asyncRuns = new LinkedBlockingQueue<Runnable>(); 
	
	/**
	 * Stores the thread that is processing the asyncRun runnables
	 */
	private final EventManagementThread eventDispatchThread;
	
	/**
	 * Flag variable that changes to true, if we shutdown
	 */
	private volatile boolean  shutdown = false;
	
	
	/**
	 * Stores the number of hits per event
	 */
	private final ConcurrentHashMap<Class<?>, AtomicInteger> eventNamesCount = (new ConcurrentHashMap<Class<?>, AtomicInteger>());


	/**
	 * Stores the number of outstanding events to be dispatched per event
	 */
	private final ConcurrentHashMap<Class<?>, AtomicInteger> outstandingEventCount = (new ConcurrentHashMap<Class<?>, AtomicInteger>());

	
	/**
	 * Stores the semaphores of pending flush events
	 */
	private final Set<Semaphore> waitingThreads = Collections.newSetFromMap(new ConcurrentHashMap<Semaphore,Boolean>());
	
	/**
	 * Flag variable that ensures that if the dispatch thread is done. 
	 */
	private volatile boolean dispatchThreadDone = false;
	
	
	
	
	public EventManager()
	{
	
		
		this.registerHandler(FlushEvent.class, new EventHandler<FlushEvent>() {

			@Override
			public void handleEvent(FlushEvent event) {
				event.releaseSemaphore();
			}
			
		});
		
		EventManagementThread t = new EventManagementThread(asyncRuns);
		eventDispatchThread = t;
		t.start();		
		
	}
	
	
	/**
	 * Registers a handler 
	 * @param eventClass The class of event to register
	 * @param handler 	 handler to invoke
	 */
	public synchronized void registerHandler(Class< ? extends AutomaticConfiguratorEvent> eventClass, EventHandler<?> handler)
	{
		checkForShutdown();
		
		handlerMap.putIfAbsent(eventClass, new ArrayList<EventHandler<?>>());
		List<EventHandler<?>> handlers = handlerMap.get(eventClass);
		handlers.add(handler);
	}
	
	private AtomicReference<EventFlushDeadLockException> deadLockException = new AtomicReference<EventFlushDeadLockException>();
	
	/**
	 * Fires an event
	 * @param event
	 * @throws EventManagerShutdownException - if the event manager was previous shutdown 
	 */
	public synchronized void fireEvent(AutomaticConfiguratorEvent event)
	{
	
		checkForShutdown();
		
		
		EventFlushDeadLockException exp = deadLockException.get();
		if(exp != null)
		{
			throw new IllegalStateException("Deadlock has previously occurred, event manager is unavailable", deadLockException.get());
		}
		
		
		log.trace("Event requested for dispatch {}",event.getClass().getSimpleName());
		handlerMap.putIfAbsent(event.getClass(), new ArrayList<EventHandler<?>>());
		final List<EventHandler<?>> handlers = handlerMap.get(event.getClass());
		
		eventNamesCount.putIfAbsent(event.getClass(), new AtomicInteger(0));
		eventNamesCount.get(event.getClass()).incrementAndGet();
		
		
		this.outstandingEventCount.putIfAbsent(event.getClass(),new AtomicInteger(0));
		
		for(EventHandler<?> handler : handlers)
		{
			this.outstandingEventCount.get(event.getClass()).incrementAndGet();
			
			final AutomaticConfiguratorEvent event2 = event;
			@SuppressWarnings("rawtypes")
			final EventHandler handler2 = handler;
			
			Runnable run = new Runnable()	{
				
				
				@SuppressWarnings("unchecked")
				public void run()
				{
					try { 
						log.trace("Dispatching event {} to handler: {}", event2.getClass().getSimpleName() + " (0x" + Integer.toHexString(System.identityHashCode(event2)) +")", handler2.getClass().getSimpleName());
						
						outstandingEventCount.get(event2.getClass()).decrementAndGet();
						
						handler2.handleEvent(event2);
					} catch(RuntimeException t)
					{
						
						
						Object[] args = { handler2, event2, t, };
						log.error("Error occured during dispatching of event", t);
						log.error("Event Handler {} while processing event: {}, threw Exception {}",args);
						
						if(!(event2 instanceof EventHandlerRuntimeExceptionEvent))
						{
							EventManager.this.fireEvent(new EventHandlerRuntimeExceptionEvent(t, event2));
							
							return;
						} else
						{
							log.error("Event Handler threw exception while we were processing the {} event, not notifying anything else", event2.getClass());
						}
						
					}
				}
			};
			
			try {
				this.asyncRuns.put(run);
			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();
			}
			
		}
		
		
	}

	/**
	 * Ensures that all previous events have completed
	 */
	public void flush() {
		
		if(shutdown) return;
		  
		checkForDeadLock();
		
		Semaphore wait = new Semaphore(0);
		
		waitingThreads.add(wait);
		
		try {
			this.fireEvent(new FlushEvent(wait));
			
			
			if(dispatchThreadDone)
			{
				log.trace("Flush probably not completed as dispatch thread has terminated");
			} else
			{
				try {
					wait.acquire();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		} finally
		{
			waitingThreads.remove(wait);
		}
		
		
	}
	
	public synchronized boolean isShutdown()
	{
		return shutdown;
	}
	
	/**
	 * This method checks to see if an event is calling flush() as a side effect
	 */
	private void checkForDeadLock()
	{

		if(Thread.currentThread().equals(eventDispatchThread))
		{
			EventFlushDeadLockException e = new EventFlushDeadLockException();
			this.deadLockException.set(e);
			
			log.error("Deadlock detected ", e);
			System.out.flush();
			System.err.flush();
			
			e.printStackTrace();
			System.out.flush();
			System.err.flush();
			
			throw e;
		}
	}
	
	private synchronized void checkForShutdown()
	{
		if(shutdown)
		{
			throw new EventManagerShutdownException();
		}
		
		if(dispatchThreadDone)
		{
			throw new EventManagerPrematureShutdownException();
		}
	}
	
	
	class EventManagementThread extends Thread
	{ 
		private transient Logger log = LoggerFactory.getLogger(EventManager.class);
		private BlockingQueue<Runnable> asyncRuns;
		
		private Semaphore threadDone = new Semaphore(0);
		
		private int highLoadQueueWarningDisplay = 128;
		public EventManagementThread(BlockingQueue<Runnable> asyncRuns)
		{
			this.asyncRuns = asyncRuns;
			setName("Event Manager Dispatch Thread");
			setDaemon(true);
		}
	
		@Override
		public void run()
		{
			try {
				while(true)
				{
					try {
						
						if(Thread.interrupted())
						{
							Thread.currentThread().interrupt();
							return;
						}
						
							asyncRuns.take().run();
						
						
						if(asyncRuns.size() > highLoadQueueWarningDisplay)
						{
							highLoadQueueWarningDisplay *= 2;
							log.warn("Processing Events has {} elements currently waiting, next warning at {} ", asyncRuns.size(), highLoadQueueWarningDisplay);
							
							StringBuilder sb = new StringBuilder();
							for(Entry<Class<?>, AtomicInteger> ent : outstandingEventCount.entrySet())
							{
								sb.append(ent.getKey().getSimpleName()).append("=>(").append(ent.getValue().get()).append("), ");
							}
							
							sb.setCharAt(sb.length() - 2,' ');
							
							log.warn("Outstanding Events are as follows: {}",sb);
						}
						
					} catch(RuntimeException e)
					{
						log.error("Unexpected Exception occured", e);
						
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					} catch(Error e)
					{
						log.error("Error occurred in Event Manager ",e);
						throw e;
					}
				}
			} finally
			{
				dispatchThreadDone = true;
				
				threadDone.release();
				
				int i=0; 
				
				for(Semaphore s : waitingThreads)
				{
					s.release();
					i++;
				}
				
				
				log.debug("Event Manager thread done, released {} pending flushes", i);
				
			}
		}
		
		public void waitForCompletion()
		{
			try {
				try {
					threadDone.acquire();
				} catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return;
				}
				
			} finally
			{
				threadDone.release();
			}
		}

	};
	
	public synchronized void shutdown()
	{
		
		try {
			this.fireEvent(new EventManagerShutdownEvent());
			this.flush();
		} catch(EventManagerPrematureShutdownException e)
		{
			//Ignore this exception
			//As we are cleaning up
		}
		shutdown = true;
		

		eventDispatchThread.interrupt();
		eventDispatchThread.waitForCompletion();	
		
		
		StringBuilder sb = new StringBuilder();
		for(Entry<Class<?>, AtomicInteger> ent : this.eventNamesCount.entrySet())
		{
			sb.append(ent.getKey().getSimpleName()).append("=>(").append(ent.getValue().get()).append("), ");
		}
		sb.setCharAt(sb.length() - 2,' ');
		log.debug("Event Dispatch Name / Counts: {}", sb );
	}
}
