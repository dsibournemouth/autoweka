package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.AtomicDouble;

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
 * Ensures that observed runtimes for RUNNING/KILLED runs always strictly increasing
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class StrictlyIncreasingRuntimesTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AtomicDouble> maxRuntimeObserved = new ConcurrentHashMap<AlgorithmRunConfiguration, AtomicDouble>();
	
	public StrictlyIncreasingRuntimesTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae) {
		super(tae);
	}

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}

	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, final TargetAlgorithmEvaluatorRunObserver obs) {

		
		
		
		
		
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
			maxRuntimeObserved.put(rc, new AtomicDouble(0));
		}
		
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
					List<AlgorithmRunResult> fixedRuns = new ArrayList<AlgorithmRunResult>(runs.size());
					
					
					for(AlgorithmRunResult run : runs)
					{
						
						double runtime = 0;
						
						switch(run.getRunStatus())
						{
							case KILLED:
								runtime = Math.max(maxRuntimeObserved.get(run.getAlgorithmRunConfiguration()).get(), run.getRuntime());
								break;
							default:
								runtime = run.getRuntime();
						}
						
						fixedRuns.add(new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(),run.getRunStatus(),runtime, run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
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
					
						fixedRuns.add(new RunningAlgorithmRunResult(run.getAlgorithmRunConfiguration(),updateRuntime(run.getAlgorithmRunConfiguration(),run.getRuntime()),run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getWallclockExecutionTime(),kh));						
					} else if(run.getRunStatus().equals(RunStatus.KILLED))
					{
						fixedRuns.add(new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), run.getRunStatus(),updateRuntime(run.getAlgorithmRunConfiguration(),run.getRuntime()), run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
					}	else
					{
						fixedRuns.add(new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
					}
				}
				
				if(obs != null)
				{
					obs.currentStatus(fixedRuns);
				}
				
				
			}
			
			private double updateRuntime(AlgorithmRunConfiguration rc, double runtime)
			{
				AtomicDouble d = maxRuntimeObserved.get(rc);
				while(true)
				{
					double oldValue = d.get();
					if(oldValue < runtime)
					{
						d.compareAndSet(oldValue, runtime);
					} else
					{
						return runtime;
					}
					
				}
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, myHandler, runObs);

	}
	

	@Override
	protected void postDecorateeNotifyShutdown() {
		// TODO Auto-generated method stub
		
	}

	
}
