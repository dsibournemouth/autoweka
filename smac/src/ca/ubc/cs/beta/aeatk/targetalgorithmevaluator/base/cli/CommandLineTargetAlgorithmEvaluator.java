package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractAsyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.algorithmrunner.ConcurrentAlgorithmRunner;

/**
 * Evalutes Given Run Configurations
 *
 */
public class CommandLineTargetAlgorithmEvaluator extends AbstractAsyncTargetAlgorithmEvaluator {
	
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	
	private final int observerFrequency;
	
	private final CommandLineTargetAlgorithmEvaluatorOptions options;
	
	private final BlockingQueue<Integer> executionIDs; 
	
	private final ExecutorService asyncExecService;
	
	private final ExecutorService commandLineAlgorithmRunExecutorService;
	private final ExecutorService observerExecutorService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("CLI TAE Observer Threads", true));
	
	private final ExecutorService callbackExecutorService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("CLI TAE Callback Threads", true));
	
	
	private final Semaphore asyncExecutions;
	
	private final AtomicBoolean shutdown = new AtomicBoolean(false);
	
	
	/**
	 * Constructs CommandLineTargetAlgorithmEvaluator
	 * @param execConfig 			execution configuration of the target algorithm
	 * @param options	<code>true</code> if we should execute algorithms concurrently, <code>false</code> otherwise
	 */
	CommandLineTargetAlgorithmEvaluator(CommandLineTargetAlgorithmEvaluatorOptions options)
	{
		
		this.observerFrequency = options.observerFrequency;
		if(observerFrequency < 50) throw new ParameterException("Observer Frequency can't be less than 50 ms");
		
		this.options = options;
		
		executionIDs = new ArrayBlockingQueue<Integer>(options.cores);
		this.asyncExecService = Executors.newFixedThreadPool(options.cores, new SequentiallyNamedThreadFactory("CLI TAE Asynchronous Request Processing"));
		
		this.commandLineAlgorithmRunExecutorService = Executors.newFixedThreadPool(options.cores,new SequentiallyNamedThreadFactory("CLI TAE Master Dispatch Thread", true));
		
		
		this.asyncExecutions = new Semaphore(options.cores,true);
		
		int numCPUs = Runtime.getRuntime().availableProcessors();

		if(options.cores > numCPUs)
		{
			log.warn("Number of cores requested is seemingly greater than the number of available cores. This may affect runtime measurements");
		}
		
		for(int i = 0; i < options.cores ; i++)
		{
			executionIDs.add(Integer.valueOf(i));
		}
	}
	

	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,final  TargetAlgorithmEvaluatorCallback taeCallback, final TargetAlgorithmEvaluatorRunObserver runStatusObserver) 
	{
		
		if(runConfigs.size() == 0)
		{
			taeCallback.onSuccess(Collections.<AlgorithmRunResult> emptyList());
			return;
		}
		
		
		try {
			this.asyncExecutions.acquire();
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			taeCallback.onFailure(new IllegalStateException("Request interrupted", e));
			return;
		}
		
		
		try 
		{
			this.asyncExecService.execute(new Runnable()
			{
	
				@Override
				public void run() {
					
	
					ConcurrentAlgorithmRunner runner =null;
					
					List<AlgorithmRunResult> runs = null;
					
					try 
					{
						try {
							runner = getAlgorithmRunner(runConfigs,runStatusObserver);
							runs =  runner.run(commandLineAlgorithmRunExecutorService);
						} finally
						{
							asyncExecutions.release();	
						}
					} catch(RuntimeException e)
					{
						taeCallback.onFailure(e);
						return;
					} catch(Throwable e)
					{
						taeCallback.onFailure(new IllegalStateException("Unexpected Throwable:", e));
						if(e instanceof Error)
						{
							throw e;
						}
						
						return;
					}
					
					addRuns(runs);
					
					final List<AlgorithmRunResult> finalSolutions = runs;
					
					Runnable callbackInvokingRunnable = new Runnable()
					{

						@Override
						public void run() {
							try {
								if(runStatusObserver != null)
								{
									runStatusObserver.currentStatus(finalSolutions);
								}
								taeCallback.onSuccess(finalSolutions);
							} catch(RuntimeException e)
							{
								taeCallback.onFailure(e);
							} catch(Throwable e)
							{
								taeCallback.onFailure(new IllegalStateException("Unexpected Throwable:", e));
								
								if(e instanceof Error)
								{
									throw e;
								}
							}
							
						}
						
					};
					
					callbackExecutorService.execute(callbackInvokingRunnable);
					
					
					
					
					
				}
				
			});
		} catch(RuntimeException e)
		{
			log.error("Got exception on " + AlgorithmRunConfiguration.class.getSimpleName() + " submission to " + this.getClass().getSimpleName(), e);
			asyncExecutions.release();
			throw e;
		}
		
	}
	
	
	/**
	 * Helper method which selects the AlgorithmRunner to use
	 * @param runConfigs 	runConfigs to evaluate
	 * @return	AlgorithmRunner to use
	 */
	private ConcurrentAlgorithmRunner getAlgorithmRunner(List<AlgorithmRunConfiguration> runConfigs,TargetAlgorithmEvaluatorRunObserver obs)
	{
		int cores = options.cores;
		if(!options.concurrentExecution)
		{
			cores = 1;
		}
		return new ConcurrentAlgorithmRunner(runConfigs, cores, obs, options,executionIDs, this.observerExecutorService);
	}

	
	@Override
	public boolean isRunFinal() {
		return false;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	public boolean areRunsObservable() {
		return true;
	}


	@Override
	public void notifyShutdown() {
		
		
		try {
			this.asyncExecService.shutdown();
			this.commandLineAlgorithmRunExecutorService.shutdown();
			this.observerExecutorService.shutdown();
			this.callbackExecutorService.shutdown();
			
			log.debug("Awaiting Termination of existing command line algorithm runs");

			boolean terminated = this.asyncExecService.awaitTermination(10, TimeUnit.SECONDS);
			
			while(!terminated)
			{
				log.warn("Termination of target algorithm evaluator failed, outstanding runs must still exist [asyncExecService]");
				terminated = this.asyncExecService.awaitTermination(10, TimeUnit.MINUTES);
			}
			
			
			terminated = this.commandLineAlgorithmRunExecutorService.awaitTermination(10, TimeUnit.SECONDS);
			
			while(!terminated)
			{
				log.warn("Termination of target algorithm evaluator failed (CLI Runs), outstanding runs must still exist [commandLineAlgorithmRunExecutorService]");
				terminated = this.commandLineAlgorithmRunExecutorService.awaitTermination(10, TimeUnit.MINUTES);
			}
			
			terminated = this.observerExecutorService.awaitTermination(10, TimeUnit.SECONDS);
			
			while(!terminated)
			{
				log.warn("Termination of target algorithm evaluator failed (CLI Runs), outstanding runs must still exist [observerExecutorService]");
				terminated = this.observerExecutorService.awaitTermination(10, TimeUnit.MINUTES);
			}
			
			terminated = this.callbackExecutorService.awaitTermination(10, TimeUnit.SECONDS);
			
			while(!terminated)
			{
				log.warn("Termination of target algorithm evaluator failed (CLI Runs), outstanding runs must still exist [callbackExecutorService]");
				terminated = this.callbackExecutorService.awaitTermination(10, TimeUnit.MINUTES);
			}
			
		} catch (InterruptedException e) {
			log.warn("notifyShutdown() called on " + this.getClass().getSimpleName() + " but was interrupted");
			Thread.currentThread().interrupt();
			this.asyncExecService.shutdownNow();
			this.commandLineAlgorithmRunExecutorService.shutdownNow();
			this.observerExecutorService.shutdownNow();
			this.callbackExecutorService.shutdownNow();
			return;
			
		}
	}


	
	
	


}
