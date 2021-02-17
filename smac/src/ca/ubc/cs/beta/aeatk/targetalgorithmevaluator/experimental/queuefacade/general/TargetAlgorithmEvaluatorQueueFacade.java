package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.general;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException;

/**
 * Facade for Target Algorithm Evaluators that simplifies Thread Safety management for clients using asynchronous execution
 * <p>
 * <b>Note:</b>This class essentially calls the runs asynchronously and then puts the results in a Queue, that can be read 
 * by the client. Provided that nothing is modifying the context object in between this should be thread safe.
 * By synchronizing all methods we are trying to ensure memory visibility guarantees. 
 * <p>
 * The related class {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue} is more basic and supplies a good
 * 
 * default context object that lets you retrieve the results 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 * @see ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue
 * @param <K> Type of context object to store
 */
@ThreadSafe
public class TargetAlgorithmEvaluatorQueueFacade<K extends TargetAlgorithmEvaluatorQueueResultContext> {

	private final TargetAlgorithmEvaluator tae;
	private final LinkedBlockingQueue<K> queue = new LinkedBlockingQueue<K>();
	private final boolean throwExceptions;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AtomicInteger outstandingRuns = new AtomicInteger(0);
	private final AtomicInteger queuedAndOutstandingRuns = new AtomicInteger(0);

	public TargetAlgorithmEvaluatorQueueFacade(TargetAlgorithmEvaluator tae, boolean throwExceptions)
	{
		this.tae = tae;
		this.throwExceptions = throwExceptions;
	}
	/**
	 * Retrieves a reference to the Target Algorithm Evaluator
	 * @return
	 */
	public synchronized TargetAlgorithmEvaluator getTargetAlgorithmEvaluator()
	{
		return tae;
	}
	
	/**
	 * Evaluate a single run
	 * 
	 * @param context 		context result that will be populated with the answer
	 * @param runConfig		runConfig to run
	 */
	public synchronized void evaluateRunAsync(K context, AlgorithmRunConfiguration runConfig)
	{
		evaluateRunAsync(context, Collections.singletonList(runConfig));
	}
	
	/**
	 * Evaluate many runs
	 * 
	 * @param context 		context result that will be populated with the answer
	 * @param runConfigs	a list of runConfigs to run
	 */
	public synchronized void evaluateRunAsync(K context, List<AlgorithmRunConfiguration> runConfigs)
	{
		evaluateRunAsync(context, runConfigs, null);
	}
	
	/**
	 * Evaluate many runs
	 * 
	 * @param context 		context result that will be populated with the answer
	 * @param runConfigs	a list of runConfigs to run
	 * @param obs			observer to monitor
	 */
	public synchronized void evaluateRunAsync(K context, List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs)
	{
		context.setRunConfigs(runConfigs);
		outstandingRuns.incrementAndGet();
		queuedAndOutstandingRuns.incrementAndGet();
		try {
			tae.evaluateRunsAsync(runConfigs, new QueueingTargetAlgorithmEvaluatorCallback(context), obs);
		} catch(RuntimeException e)
		{
			outstandingRuns.decrementAndGet();
			queuedAndOutstandingRuns.decrementAndGet();
			throw e;
		}
	}
	
	/**
	 * Retrieves and removes an element from the set of completed results or <code>null</code> if there are none 
	 * @return 	context or <code>null</code> if none available
	 */
	public synchronized K poll()
	{
		K context = (queue.poll());
		
		if(context != null)
		{
			this.queuedAndOutstandingRuns.decrementAndGet();
		} 
		
		return checkForException(context);
	}
	
	/**
	 * Returns a completed element when available
	 * @return
	 * @throws InterruptedException
	 */
	public K take() throws InterruptedException
	{
		
		synchronized(this)
		{
			
		}
		
		K context = (queue.take());
		
		synchronized (this) 
		{
			if(context != null)
			{
				this.queuedAndOutstandingRuns.decrementAndGet();
			} 
			return checkForException(context);
		}
		
	}
	
	/**
	 * Returns a completed element when available or null if it times out
	 * @param timeout	the amount of time to wait
	 * @param unit		the unit for the amount of time
	 * @return			a completed context or <code>null</code>
	 * @throws InterruptedException
	 */
	public synchronized K poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		K context = queue.poll(timeout, unit);
		
		if(context != null)
		{
			this.queuedAndOutstandingRuns.decrementAndGet();
		} 
		
		return checkForException(context);
	}

	/**
	 * Retrieves but does not remove an element from the set of completed results or <code>null</code> if there are none 
	 * @return 	context or <code>null</code> if none available
	 */
	public synchronized K peek()
	{
		return checkForException(queue.peek());
	}
	
	/**
	 * Retrieves an approximate number of outstanding runs (that is runs that were evaluated but are not queued)
	 * 
	 * @return approximate number of outstanding runs
	 */
	public synchronized int getApproximateNumberOutstandingRuns()
	{
		return this.outstandingRuns.get();
	}
	
	/**
	 * Retrieves an approximate number of runs in the queue (Note: This can be an O(n) operation)
	 * 
	 * @return approxmiate number of queued runs
	 */
	public synchronized int getApproximateNumberOfQueuedRuns()
	{
		return this.queue.size();
	}
	
	/**
	 * Returns the number of queued and outstanding runs (the number of runs executing in the TAE + the number of results in the queue)
	 * @return total runs submitted but not retrieved
	 */
	public synchronized int getNumberOfOutstandingAndQueuedRuns()
	{
		return this.queuedAndOutstandingRuns.get();
	}
	
	private K checkForException(K context)
	{
		if(context == null)
		{
			return context;
		}
		
		if(throwExceptions)
		{
			if(context.getRuntimeException() != null)
			{
				throw context.getRuntimeException();
			}
		}
		return context;
	}
	
	private class QueueingTargetAlgorithmEvaluatorCallback implements TargetAlgorithmEvaluatorCallback
	{
		private final K context;
		
		public QueueingTargetAlgorithmEvaluatorCallback(K context)
		{
			this.context = context;
		}

		@Override
		public void onSuccess(List<AlgorithmRunResult> runs) {
			context.setAlgorithmRuns(runs);
			try {
				queue.put(context);
				outstandingRuns.decrementAndGet();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				
				this.onFailure(new TargetAlgorithmEvaluatorShutdownException(e));
				
				
			}
		}

		@Override
		public void onFailure(RuntimeException e) {
			context.setAlgorithmRuns(null);
			context.setRuntimeException(e);
			
			try {
				queue.put(context);
				outstandingRuns.decrementAndGet();
			} catch(InterruptedException e2)
			{
				try {
					queue.put(context);
					
				} catch (InterruptedException e1) {
					log.warn("Interrupted Exception again this result will be swallowed", e);
					e1.printStackTrace();
				}
				//We are done
				outstandingRuns.decrementAndGet();
				Thread.currentThread().interrupt();
				return;
			}
			
		}
		
	}
	
	
	
}
