package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockThreadTracker {

	private final ConcurrentHashMap<Thread, AtomicInteger> readers =new ConcurrentHashMap<Thread, AtomicInteger>();
	private final ConcurrentHashMap<Thread, AtomicInteger> writers = new ConcurrentHashMap<Thread, AtomicInteger>();
	
	private final ReentrantReadWriteLock internalLock = new ReentrantReadWriteLock(true);

	
	private static final boolean LOCK_DEBUG = false;
	
	
	
	public ReadWriteLockThreadTracker()
	{
		
	}
	
	
	public void lockRead()
	{
		try {
			if(LOCK_DEBUG) readers.putIfAbsent(Thread.currentThread(),new AtomicInteger(0));
			internalLock.readLock().lock();
		} catch(RuntimeException e)
		{
			e.printStackTrace();
			throw e;
		} catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(255);
		}
		
		if(LOCK_DEBUG) readers.get(Thread.currentThread()).incrementAndGet();
	}
	
	public void unlockRead()
	{
		try {
			internalLock.readLock().unlock();
		} catch(RuntimeException e)
		{
			e.printStackTrace();
			throw e;
		}  catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(255);
		}
		
		if(LOCK_DEBUG) readers.get(Thread.currentThread()).decrementAndGet();
	}
	
	public void lockWrite()
	{
	
		try {
			if(LOCK_DEBUG) writers.putIfAbsent(Thread.currentThread(),new AtomicInteger(0));
			internalLock.writeLock().lock();
		} catch(RuntimeException e)
		{
			e.printStackTrace();
			throw e;
		}  catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(255);
		}
		
		if(LOCK_DEBUG) writers.get(Thread.currentThread()).incrementAndGet();
	}
	
	public void unlockWrite()
	{
		try {
			internalLock.writeLock().unlock();
		} catch(RuntimeException e)
		{
			e.printStackTrace();
			throw e;
		} catch(Throwable t)
		{
				t.printStackTrace();
				System.exit(255);
		}
		
		if(LOCK_DEBUG) writers.get(Thread.currentThread()).decrementAndGet();
	}

	public String toString()
	{
		return readers.toString() + " W: " + writers.toString();
	}

	
}
