package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.forking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractAsyncTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;

/**
 * Forks the asynchronous evaluation of runs between a master TAE and a slave TAE (not blocking on the slave TAE).
 * Kill run executions in either TAEs if one of them terminates the runs first.
 * 
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>, Steve Ramage <seramage@cs.ubc.ca>
 * 
 */
public class ForkingTargetAlgorithmEvaluatorDecorator extends AbstractAsyncTargetAlgorithmEvaluatorDecorator {
	
	private final static Logger log = LoggerFactory.getLogger(ForkingTargetAlgorithmEvaluatorDecorator.class);
	
	private final ExecutorService fSlaveSubmitterThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()+1,new SequentiallyNamedThreadFactory("Forking TAE Slave Submitter",true));
	
	private final TargetAlgorithmEvaluator fSlaveTAE;

	private final ForkingTargetAlgorithmEvaluatorDecoratorPolicyOptions fOptions;
	
	
	
	private final AtomicLong masterSolvesFirst = new AtomicLong(0);
	private final AtomicLong slaveSolvesFirst = new AtomicLong(0);
	private final AtomicLong slaveSubmits = new AtomicLong(0);
	
	private final AtomicLong requests = new AtomicLong(0);
	public ForkingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator aMasterTAE, TargetAlgorithmEvaluator aSlaveTAE, ForkingTargetAlgorithmEvaluatorDecoratorPolicyOptions fOptions) {
		super(aMasterTAE);
		fSlaveTAE = aSlaveTAE;
		this.fOptions = fOptions;
		
		if(fOptions.fPolicy == null)
		{
			throw new ParameterException("If you are using the --fork-to-tae option you must also set the --fork-to-tae-policy option");
		}
	}
	
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer)
	{
		final AtomicBoolean fForkCompletionFlag = new AtomicBoolean(false);
		
		
		requests.addAndGet(runConfigs.size());
		
		final TargetAlgorithmEvaluatorCallback masterForkCallback = new TargetAlgorithmEvaluatorCallback() {
			
			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				if(fForkCompletionFlag.compareAndSet(false, true))
				{
					masterSolvesFirst.addAndGet(runConfigs.size());
					callback.onSuccess(runs);
				}
			}
			
			@Override
			public void onFailure(RuntimeException e) {
				if(fForkCompletionFlag.compareAndSet(false, true))
				{
					masterSolvesFirst.addAndGet(runConfigs.size());
					callback.onFailure(e);
				}
				else
				{
					log.error("Received run failures after callback already notified.",e);
				}
			}
		};
		
		final List<AlgorithmRunConfiguration> slaveRunConfigurations;
		
		final Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> newToOldRunConfigurationMap = new ConcurrentHashMap<>();
		
		switch(fOptions.fPolicy)
		{
			case DUPLICATE_ON_SLAVE:
				slaveRunConfigurations = runConfigs;
				break;
			case DUPLICATE_ON_SLAVE_QUICK:
				slaveRunConfigurations = new ArrayList<>();
				for(AlgorithmRunConfiguration rc : runConfigs)
				{
					AlgorithmRunConfiguration newRC = new AlgorithmRunConfiguration(rc.getProblemInstanceSeedPair(), Math.min(fOptions.duplicateOnSlaveQuickTimeout, rc.getCutoffTime()), rc.getParameterConfiguration(), rc.getAlgorithmExecutionConfiguration());
					newToOldRunConfigurationMap.put(newRC, rc);
					slaveRunConfigurations.add(newRC);
				}
				break;
			default:
				throw new IllegalStateException("Unexpected policy implemented, which is not supported: " + fOptions.fPolicy);
			
		}
		
		final TargetAlgorithmEvaluatorCallback slaveForkCallback = new TargetAlgorithmEvaluatorCallback() {
			
			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				
				switch(fOptions.fPolicy)
				{
					case DUPLICATE_ON_SLAVE:
						
						if(fForkCompletionFlag.compareAndSet(false, true))
						{
							slaveSolvesFirst.addAndGet(runConfigs.size());
							callback.onSuccess(runs);
						}
						
						break;
						
					case DUPLICATE_ON_SLAVE_QUICK:
					    
						List<AlgorithmRunResult> fixedRuns = new ArrayList<>(runs.size());
						for(AlgorithmRunResult run : runs)
						{
							if(run.isCensoredEarly())
							{
								return;
							}
							
							AlgorithmRunConfiguration oldRC = newToOldRunConfigurationMap.get(run.getAlgorithmRunConfiguration());
							
							fixedRuns.add(new ExistingAlgorithmRunResult(oldRC,run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime()));
						}
						
						
						if(fForkCompletionFlag.compareAndSet(false, true))
						{
							slaveSolvesFirst.addAndGet(runConfigs.size());
							callback.onSuccess(fixedRuns);
						}
						
						break;
					default:
						throw new IllegalStateException("Unexpected policy implemented, which is not supported: " + fOptions.fPolicy);
				}
				
				
			}
			
			@Override
			public void onFailure(RuntimeException e) {
				if(fForkCompletionFlag.compareAndSet(false, true))
				{
					slaveSolvesFirst.addAndGet(runConfigs.size());
					callback.onFailure(e);
				}
				else
				{
					log.error("Received run failures after callback already notified.",e);
				}
			}
		};
		
		
		
		final TargetAlgorithmEvaluatorRunObserver forkObserver = new TargetAlgorithmEvaluatorRunObserver() {
			
			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
				if(fForkCompletionFlag.get())
				{
					for(AlgorithmRunResult run : runs)
					{
						run.kill();
					}
				}
				//TODO Perform observation.
			}
		};
		
		//Submit the job to the forked slave TAE in another thread to avoid any kind of blocking.
		fSlaveSubmitterThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				
				if(!fForkCompletionFlag.get())
				{ //Only submit if the job isn't done.
					slaveSubmits.addAndGet(runConfigs.size());
					fSlaveTAE.evaluateRunsAsync(slaveRunConfigurations, slaveForkCallback, forkObserver);
				}
				
			}
		});
		
		
		
		//Do this here and not in another thread to honor any (blocking) contracts the master TAE has.
		this.tae.evaluateRunsAsync(runConfigs, masterForkCallback, forkObserver);
	}

	
	/**
	 * Both TAEs runs must be final for their forked runs to be final.
	 */
	@Override
	public boolean isRunFinal()
	{
		//Return AND of both TAEs.
		return this.tae.isRunFinal() && fSlaveTAE.isRunFinal();
	}
	
	/**
	 * Both TAEs runs must be persisted for their forked runs to be persisted.
	 */
	@Override
	public boolean areRunsPersisted()
	{
		//Return AND of both TAEs.
		return this.tae.areRunsPersisted() && fSlaveTAE.areRunsPersisted();
	}
	
	@Override
	//TODO Support observation.
	public boolean areRunsObservable()
	{
		return false;
	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//Shutdown the forked slave TAE submitter.
		try
		{
			fSlaveSubmitterThreadPool.shutdownNow();
			try {
				
				
				if(!fSlaveSubmitterThreadPool.awaitTermination(1, TimeUnit.MINUTES))
				{
					log.warn("Trying to shutdown slave Target Algorithm Evaluator, no response after 1 minute will wait up to two hours before continuing");
				}
				
				if(!fSlaveSubmitterThreadPool.awaitTermination(59, TimeUnit.MINUTES))
				{
					log.warn("Slave TAE submitter thread pool did not terminate after one hour, will wait one more hour");
				}
				
				if(!fSlaveSubmitterThreadPool.awaitTermination(60, TimeUnit.MINUTES))
				{
					log.error("Slave TAE submitter thread pool did not terminate in 120 minutes.");
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			//Shutdown the forked slave TAE.
			try 
			{
				fSlaveTAE.notifyShutdown();
			} catch(RuntimeException e)
			{
				log.warn("Slave Target Algorithm Evaluator did not shutdown correctly, this may or may not be because of outstanding runs still executing. This message may possibly be benign", e);
			}
		} finally
		{
			//log.info("Fork Target Algorithm Evaluator Statistics: Requests: {}, Master Solves First: {}, Slave Solves First: {}, Slave Submits: {}", this.requests.get(), this.masterSolvesFirst.get(), this.slaveSolvesFirst.get(), this.slaveSubmits.get());
		}
		
	}
	
	/*
	 * {@see AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator}
	 */
	
	public void throwException()
	{
		 throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting/observing/reporting the number of outstanding evaluations. This is because this Target Algorithm Evaluator may schedule runs internally that should not be "
				+ "apparent to outside observers. You should rewrap this class with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class.getCanonicalName() );
		
	}
	/**
	 * We need to throw this now because even if the lower level supplies it, we may break it.
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public void waitForOutstandingEvaluations()
	{
		throwException();
	}
	
	/**
	 * We need to throw this now because even if the lower level supplies it, we may break it.
	 */
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		throwException();
		return -1;
	}
	
	@Override
	public int getNumberOfOutstandingRuns()
	{
		throwException();
		return -1;
	}
	

	
	@Override
	public int getNumberOfOutstandingBatches() {
		throwException();
		return -1;
	}

	

	@Override
	public int getRunCount() {
		throwException();
		return -1;
	}
	
	/*
	 * Thanks Ramage ;)
	 */
	
	
}
