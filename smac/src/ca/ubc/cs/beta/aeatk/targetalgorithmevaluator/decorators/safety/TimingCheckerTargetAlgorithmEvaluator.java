package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * Logs warnings if timing invariants on algorithm aren't holding
 * 
 * This class generally increases the time after a single warning, so as not to be spammy.
 * 
 * @author Steve Ramage<seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class TimingCheckerTargetAlgorithmEvaluator extends	AbstractTargetAlgorithmEvaluatorDecorator {


	private double totalWallClockOverhead = 0;
	private double totalRuntimeOverhead = 0;
	private double totalWallClockVersusRuntimeDifference = 0;
	private double totalWalltime;
	private double totalRuntime;

	
	private boolean subsecondOverheadLogged = false;
	
	private static final Logger log = LoggerFactory.getLogger(TimingCheckerTargetAlgorithmEvaluator.class);
	

	public TimingCheckerTargetAlgorithmEvaluator( TargetAlgorithmEvaluator tae) 
	{
		super(tae);
		wallClockDeltaToRequireLogging = 10;
	}
	/**
	 * Linear amount of time we should allow the algorithm to exceed the request before logging a warning. 
	 * 
	 * We use delta because cap times are not constant, and we are concerned with the uncounted overhead
	 */
	private double runtimeDeltaToRequireLogging = 1;
	
	/**
	 * Linear amount of time we should allow the algorithms wallclock time to exceed the request before logging a warning.
	 * 
	 * We use delta because cap times are not constant, and we are concerned with the uncounted overhead
	 */
	private double wallClockDeltaToRequireLogging;
	
	

	public void postDecorateeNotifyShutdown()
	{
		synchronized(this)
		{
			double maxOverhead = 0.0;
			maxOverhead = Math.max( ((double) totalRuntimeOverhead) / totalRuntime, maxOverhead);
			maxOverhead = Math.max(((double) totalWallClockOverhead) / totalWalltime, maxOverhead);
			maxOverhead = Math.max(((double) totalWalltime) / totalRuntime, maxOverhead);
			
			
			
			if(log.isDebugEnabled() || maxOverhead > 0.1)
			{
				log.debug("Total Reported Runtime: {} (s), Total of Sum Max(runtime-cutoff,0): {} (s)", totalRuntime, totalRuntimeOverhead);
				log.debug("Total Walltime: {} (s), Total of Sum Max(walltime - cutoff, 0): {} (s)", totalWalltime, totalWallClockOverhead);
				log.debug("Total Difference between Walltime and Runtime (Sum of the amount of wallclock time - sum of the amount of reported CPU time) : {} seconds", this.totalWallClockVersusRuntimeDifference);
			}
		}
	}

	
	protected synchronized AlgorithmRunResult processRun(AlgorithmRunResult run) {
		
		double runtimeOverhead = run.getRuntime() - run.getAlgorithmRunConfiguration().getCutoffTime();
		
		totalRuntime += Math.max(run.getRuntime(), 0);
		totalRuntimeOverhead += Math.max(runtimeOverhead, 0);
		
		
		if(runtimeOverhead > runtimeDeltaToRequireLogging)
		{
			runtimeDeltaToRequireLogging = runtimeOverhead + 1;
			
			Object[] args = {run.getRuntime(), run.getAlgorithmRunConfiguration().getCutoffTime(), runtimeOverhead, runtimeDeltaToRequireLogging};
			log.warn("Algorithm Run Result reported a runtime of {} (secs) that exceeded it's cutoff time of {} (secs) by {} (secs). Next warning at {} (secs)  ", args);
		}
		
		double wallClockOverhead = run.getWallclockExecutionTime() - run.getAlgorithmRunConfiguration().getCutoffTime();
		
		totalWalltime += Math.max(run.getWallclockExecutionTime(), 0);
		totalWallClockOverhead += Math.max(wallClockOverhead, 0);
		
		
		
		if(wallClockOverhead > Math.min(Math.max(10,1.5*run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration().getAlgorithmMaximumCutoffTime()), wallClockDeltaToRequireLogging))
		{
			
			wallClockDeltaToRequireLogging = wallClockOverhead + 1;
			
	
			Object[] args = {run.getWallclockExecutionTime(), run.getAlgorithmRunConfiguration().getCutoffTime(), wallClockOverhead, wallClockDeltaToRequireLogging};
			log.warn("Algorithm Run Result reported wallclock time of {} (secs) that exceeded it's cutoff time of {} (secs) by {} (secs). Next warning at {} (secs)  ", args);
		}
		
		this.totalWallClockVersusRuntimeDifference += Math.max(run.getWallclockExecutionTime()-run.getRuntime(), 0); 
		
		return run;
	}
	
	

	protected synchronized AlgorithmRunConfiguration processRun(AlgorithmRunConfiguration rc) 
	{
		return rc;
	}
	
	

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return processRuns(tae.evaluateRun(processRunConfigs(runConfigs), new WarnOnExceededCutoffRunObserver(obs)));
	}

	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		//We need to make sure wrapped versions are called in the same order
		//as there unwrapped versions.
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
					runs = processRuns(runs);			
					handler.onSuccess(runs);
			}

			@Override
			public void onFailure(RuntimeException t) {
					handler.onFailure(t);
			}
		};
		
		tae.evaluateRunsAsync(processRunConfigs(runConfigs), myHandler, new WarnOnExceededCutoffRunObserver(obs));

	}

	protected final List<AlgorithmRunResult> processRuns(List<AlgorithmRunResult> runs)
	{
		for(int i=0; i < runs.size(); i++)
		{
			runs.set(i, processRun(runs.get(i)));
		}
		
		return runs;
	}
	
	protected final List<AlgorithmRunConfiguration> processRunConfigs(List<AlgorithmRunConfiguration> runConfigs)
	{	
		runConfigs = new ArrayList<AlgorithmRunConfiguration>(runConfigs);
		for(int i=0; i < runConfigs.size(); i++)
		{
			runConfigs.set(i, processRun(runConfigs.get(i)));
		}
		return runConfigs;
	}
	
	
	private class WarnOnExceededCutoffRunObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private final TargetAlgorithmEvaluatorRunObserver obs;
		public WarnOnExceededCutoffRunObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
			
		}
		private ConcurrentHashMap<AlgorithmRunConfiguration, Boolean> warnCreated = new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>();
		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs) 
		{
			
			try 
			{
			    
				if (obs!= null)
				{
				    this.obs.currentStatus(runs);
				}
			} finally
			{
			
				for(AlgorithmRunResult run : runs)
				{
					if(!run.isRunCompleted())
					{
						//If the run has taken more 3 minutes, and it is more than 3 times the cutoff time we warn.
						if(run.getWallclockExecutionTime() > 3 * run.getAlgorithmRunConfiguration().getCutoffTime() && run.getWallclockExecutionTime() > 180)
						{
							if(warnCreated.putIfAbsent(run.getAlgorithmRunConfiguration(), Boolean.TRUE) == null)
							{
								//log.warn("We have been waiting for {} seconds for a run that should have taken at most {} seconds.\n "
								//		+ "The sample call for the run that is delayed is: cd \"{}\" " + CommandLineAlgorithmRun.COMMAND_SEPERATOR + "  {} ",run.getWallclockExecutionTime(), run.getAlgorithmRunConfiguration().getCutoffTime(), new File(run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory()).getAbsolutePath(), CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString(run.getAlgorithmRunConfiguration()));
							}
							
						}
					}
					
					
				}
			}
			
		}
		
	}
}
