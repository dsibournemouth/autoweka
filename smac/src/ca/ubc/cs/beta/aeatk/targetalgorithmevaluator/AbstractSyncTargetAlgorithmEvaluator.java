package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.concurrent.ReducableSemaphore;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;



/**
 * Abstract TargetAlgorithmEvaluator that implements a basic form of asynchronous execution.
 * <br>
 * <b>Note:</b> Calls will be processed in another thread. Additionally the bounding guarantees
 * are not very good, we will submit a run when some semaphore is available, even if not enough 
 * runs are available, implements or {@link #evaluateRun(List, TargetAlgorithmEvaluatorRunObserver)} need to do there own
 * internal synchronization.
 * 
 *  
 * If you need something better see {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator}.
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public abstract class AbstractSyncTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluator {

    
    private final ExecutorService execService;
	
    /**
     * 
     */
    private final ReducableSemaphore semaphore;
	
    public AbstractSyncTargetAlgorithmEvaluator() {
		this(false);
		
		
	}
    
	public AbstractSyncTargetAlgorithmEvaluator(boolean unlimitedThreads) {
		super();
		if(unlimitedThreads)
		{
			execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory(this.getClass().getSimpleName() + " Abstract Blocking TAE Async Processing Thread (Cached)"));
			semaphore = new ReducableSemaphore(Integer.MAX_VALUE, true);
		} else
		{
			int procs = Runtime.getRuntime().availableProcessors();
			execService = Executors.newFixedThreadPool(procs ,(new SequentiallyNamedThreadFactory(this.getClass().getSimpleName() + " Abstract Blocking TAE Async Processing Thread (Available Processors " + Runtime.getRuntime().availableProcessors() + ")")));
			semaphore = new ReducableSemaphore(procs,true);
		}
		
	}
	
	/**
	 * Construct an abstract synchronous target algorithm evaluator, limiting the number of threads ever created to given limit.
	 * @param aThreads - limit on threads ever created.
	 */
	public AbstractSyncTargetAlgorithmEvaluator(int aThreads)
	{
	    super();
	    if(aThreads <= 0)
	    {
	    	throw new IllegalArgumentException("Number of threads must be greater than 0");
	    }
	    execService = Executors.newFixedThreadPool(aThreads, new SequentiallyNamedThreadFactory(this.getClass().getSimpleName() + " Abstract Blocking TAE Async Processing Thread (Explicit " + aThreads + ")"));
	    semaphore = new ReducableSemaphore(aThreads,true);
	    
	    
	}

	@Override
	public  void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		if(runConfigs.size() == 0)
		{
			obs.currentStatus(Collections.<AlgorithmRunResult> emptyList());
			handler.onSuccess(Collections.<AlgorithmRunResult> emptyList());
			return;
		}
		
		
		try {
			//Acquired one permit
			semaphore.acquire();
			
			//Acquire N-1 more
			semaphore.reducePermits(runConfigs.size() - 1);
			
			
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			handler.onFailure(new IllegalStateException(e1));
			return;
		}
		
		
		Runnable run = new Runnable()
		{

			@Override
			public void run() {
				try
				{
					
					try {
						List<AlgorithmRunResult> runs = AbstractSyncTargetAlgorithmEvaluator.this.evaluateRun(runConfigs, obs);
						
						handler.onSuccess(runs);
					} catch(RuntimeException e)
					{
						handler.onFailure(e);
					} catch(Throwable t)
					{
						
						handler.onFailure(new IllegalStateException("Unexpected throwable detected",t));
						
						if(t instanceof Error)
						{
							throw t;
						}
					}
				} finally
				{
					//Release N permits
					semaphore.release(runConfigs.size());
				}
			}
			
		};
		
		
		
		if(this.areRunsPersisted())
		{
			//Need to ensure that the runs get checked for being done.
			//I don't remember why this case is here and I don't think anything ever
			//returns true that implements this.
			run.run();
		} else
		{
			execService.execute(run);
		}

	}

	/**
	 * Template method for ensuring subtype gets notified. 
	 */
	protected abstract void subtypeShutdown();
	
	
	/**
	 * We must be notified of the shutdown, so we will prevent subtypes from overriding this method.
	 */
	@Override
	public final void notifyShutdown()
	{
		execService.shutdown();
		Logger log = LoggerFactory.getLogger(this.getClass());
		try 
		{
			
			try {
				boolean shutdown = execService.awaitTermination(120, TimeUnit.SECONDS);
				if(!shutdown)
				{
					log.warn("Outstanding evaluations on Target Algorithm Evaluator did not complete within 120 seconds, will try to interrupt currently executing tasks.");
				} 
			} catch (InterruptedException e) {
				
				Thread.currentThread().interrupt();
				log.warn("Interrupted while waiting for TAE shutdown");
			}
			
			execService.shutdownNow();
				
			boolean shutdown;
				
			try {
				shutdown = execService.awaitTermination(120, TimeUnit.SECONDS);
				if(!shutdown)
				{
					LoggerFactory.getLogger(this.getClass()).warn("Outstanding evaluations on Target Algorithm Evaluator did not complete within 120 seconds, even after interruption");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while waiting for TAE shutdown after interruption");
			
			}
				
		} finally
		{
			this.subtypeShutdown();
		}
	}
}
