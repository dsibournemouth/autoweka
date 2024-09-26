package ca.ubc.cs.beta.aeatk.concurrent.threadfactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class SequentiallyNamedThreadFactory implements ThreadFactory {

	private final String name;
	private final AtomicLong threadIds = new AtomicLong(0);
	private boolean daemonThreads;
	
	//In case duplicate names are created we will always tag a global id on the end
	private static final AtomicLong threadFactoryId = new AtomicLong(0);
	
	public SequentiallyNamedThreadFactory(String name)
	{
		this(name, false);
	}
	
	
	public SequentiallyNamedThreadFactory(String name, boolean daemonThreads) {
		this.name = name + " (" + threadFactoryId.incrementAndGet();
		this.daemonThreads = daemonThreads;
	}


	@Override
	public Thread newThread(Runnable r) {

		Thread t = new Thread(r);
		t.setName(name + "-" + threadIds.incrementAndGet() + ")");
		t.setDaemon(daemonThreads);
		return t;
	}



}
