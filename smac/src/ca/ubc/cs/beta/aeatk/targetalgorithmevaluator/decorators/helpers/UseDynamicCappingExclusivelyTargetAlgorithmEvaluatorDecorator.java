package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * This decorator changes cap times of submitted runs to be that of the execution configuration, and then uses an observer to enforce the time limit.
 * 
 * The primary benefit of this, is that it allows better use of caching {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching.CachingTargetAlgorithmEvaluatorDecorator} 
 * additionally also some TAEs internally do this, such as the MySQL TAE.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class UseDynamicCappingExclusivelyTargetAlgorithmEvaluatorDecorator
		extends AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final Logger log = LoggerFactory.getLogger(getClass());
	public UseDynamicCappingExclusivelyTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);

	}
	

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}

	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, final TargetAlgorithmEvaluatorRunObserver obs) {

		
		final Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> transformedRuns = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunConfiguration>();
		
		List<AlgorithmRunConfiguration> newRunConfigs = new ArrayList<AlgorithmRunConfiguration>(runConfigs.size());
		
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
			if(rc.hasCutoffLessThanMax())
			{
				
				
				AlgorithmRunConfiguration newRC = new AlgorithmRunConfiguration(rc.getProblemInstanceSeedPair(), rc.getParameterConfiguration(), rc.getAlgorithmExecutionConfiguration());
				transformedRuns.put(newRC, rc);
				newRunConfigs.add(newRC);
			} else
			{
				
				transformedRuns.put(rc, rc);
				newRunConfigs.add(rc);
			}
			
			
		}
		
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
					List<AlgorithmRunResult> fixedRuns = new ArrayList<AlgorithmRunResult>(runs.size());
					for(AlgorithmRunResult run : runs)
					{
						RunStatus r = run.getRunStatus();
						double runtime = run.getRuntime();
						
						AlgorithmRunConfiguration origRunConfig = transformedRuns.get(run.getAlgorithmRunConfiguration());
						
						if(runtime > origRunConfig.getCutoffTime())
						{
							switch(r)
							{
								case SAT:
								case UNSAT:
								case KILLED:
								case TIMEOUT:
									r= RunStatus.TIMEOUT;
									runtime = origRunConfig.getCutoffTime();
								break;
								default:
								//NOOP
							}
						
						}
						fixedRuns.add(new ExistingAlgorithmRunResult(origRunConfig, r, runtime, run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
					}
					handler.onSuccess(fixedRuns);
			}

			@Override
			public void onFailure(RuntimeException t) {
					handler.onFailure(t);
			}
		};
		
		TargetAlgorithmEvaluatorRunObserver runObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) 
			{

				List<AlgorithmRunResult> fixedRuns = new ArrayList<AlgorithmRunResult>(runs.size());
				for( final AlgorithmRunResult run : runs)
				{
					if(run.getRunStatus().equals(RunStatus.RUNNING))
					{
						
						KillHandler kh = new KillHandler()
						{

							AtomicBoolean b = new AtomicBoolean(false);
							@Override
							public void kill() {
								b.set(true);
								run.kill();
							}

							@Override
							public boolean isKilled() {
								return b.get();
							}
							
						};
						if(transformedRuns.get(run.getAlgorithmRunConfiguration()) == null)
						{
							log.error("Couldn't find original run config for {} in {} ", run.getAlgorithmRunConfiguration(), transformedRuns);
						}
						fixedRuns.add(new RunningAlgorithmRunResult(transformedRuns.get(run.getAlgorithmRunConfiguration()),run.getRuntime(),run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getWallclockExecutionTime(),kh));
						
						
						if(transformedRuns.get(run.getAlgorithmRunConfiguration()).getCutoffTime() < run.getRuntime())
						{
							run.kill();
						}
						
					} else
					{
						fixedRuns.add(new ExistingAlgorithmRunResult(transformedRuns.get(run.getAlgorithmRunConfiguration()), run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
					}
				}
				
				if(obs != null)
				{
					obs.currentStatus(fixedRuns);
				}
				
				
			}
			
		};
		
		tae.evaluateRunsAsync(newRunConfigs, myHandler, runObs);

	}
	

	@Override
	protected void postDecorateeNotifyShutdown() {
		// TODO Auto-generated method stub
		
	}

}
