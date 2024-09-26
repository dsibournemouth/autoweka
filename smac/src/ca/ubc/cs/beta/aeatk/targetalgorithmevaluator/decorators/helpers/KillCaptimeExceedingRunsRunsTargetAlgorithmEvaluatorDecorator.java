package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

@ThreadSafe
/***
 * If runs ignore there cutoff time will eventually kill them, runs that are killed this way will be marked as crashed.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	private final double scalingFactor;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, double scalingFactor) {
		super(tae);
		if(scalingFactor <= 1.0)
		{
			throw new ParameterException("Scaling Factor for killing cannot be less than or equal to 1.0");
		}
		
		if(scalingFactor < 2.0)
		{
			log.warn("Scaling factors less than 2.0 are STRONGLY discouraged, as the runtime observations we make are only very approximate.");
		}
		
		this.scalingFactor = scalingFactor;
	}

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		KillingTargetAlgorithmEvaluatorRunObserver kObs = new KillingTargetAlgorithmEvaluatorRunObserver(obs);
		
		List<AlgorithmRunResult> runs = tae.evaluateRun(runConfigs, kObs);
		
		return fixAlgorithmRunResults(kObs, runs);
	}

	/**
	 * Transforms the results of Killed runs that we requested to CRASHED runs.
	 * @param kObs
	 * @param runs
	 */
	public List<AlgorithmRunResult> fixAlgorithmRunResults(KillingTargetAlgorithmEvaluatorRunObserver kObs,	List<AlgorithmRunResult> runs) {
		
		if(kObs.killedRuns.size() > 0)
		{
			runs = new ArrayList<AlgorithmRunResult>(runs);
			
			for(int i=0; i < runs.size(); i++)
			{
				AlgorithmRunResult run = runs.get(i);
				if(run.getRunStatus().equals(RunStatus.KILLED) && kObs.killedRuns.contains(run.getAlgorithmRunConfiguration()))
				{
					runs.set(i, new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), RunStatus.CRASHED, run.getRuntime(), run.getRunLength(), run.getQuality(), run.getResultSeed(),"Run Exceeded Captime -- Treating as " + RunStatus.CRASHED + ";" + run.getAdditionalRunData() , run.getWallclockExecutionTime()));
				}
			}
		}
		return runs;
	}
	
	
	
	@Override
	public final void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback oHandler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
	final KillingTargetAlgorithmEvaluatorRunObserver kObs = new KillingTargetAlgorithmEvaluatorRunObserver(obs);
		
		TargetAlgorithmEvaluatorCallback myCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				oHandler.onSuccess(fixAlgorithmRunResults(kObs, runs));
			}

			@Override
			public void onFailure(RuntimeException e) {
				oHandler.onFailure(e);
			}
			
		};
		tae.evaluateRunsAsync(runConfigs, myCallback, kObs);

	}
	

	private class KillingTargetAlgorithmEvaluatorRunObserver implements TargetAlgorithmEvaluatorRunObserver
	{
		
		private TargetAlgorithmEvaluatorRunObserver obs;
		
		
		final Set<AlgorithmRunConfiguration> killedRuns = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>());
		
		KillingTargetAlgorithmEvaluatorRunObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
		}
		
		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs) 
		{
			
			for(AlgorithmRunResult run : runs)
			{
				
				if(run.getRunStatus().equals(RunStatus.RUNNING))
				{
					
					if(run.getAlgorithmRunConfiguration().getCutoffTime() * scalingFactor < run.getRuntime())
					{
						
						if(!killedRuns.contains(run.getAlgorithmRunConfiguration()))
						{
							
							Object[] args = { run.getAlgorithmRunConfiguration() ,run.getRuntime(), scalingFactor, run.getAlgorithmRunConfiguration().getCutoffTime()};
							
							if(	killedRuns.add(run.getAlgorithmRunConfiguration()))
							{
								//Log the message only the first time we add the element to the set.
								log.warn("Killed run {} at {} for exceeding {} times its cutoff time of {} (secs)", args);
							}
						
							run.kill();
						}
					}
				}
			
			}
			
			if(obs != null)
			{
				obs.currentStatus(runs);
			}

		}
	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}

