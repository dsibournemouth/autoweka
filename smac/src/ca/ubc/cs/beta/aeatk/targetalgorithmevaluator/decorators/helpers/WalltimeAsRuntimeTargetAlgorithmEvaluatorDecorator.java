package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AbstractAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

public class WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final double wallclockMultScaleFactor;
	private final double startAt;
	
	public WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae) {
		super(tae);
		wallclockMultScaleFactor = 0.95;
		startAt = 0.05;
	}
	
	public WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae, double scaleFactor, double startAt) {
		super(tae);
		wallclockMultScaleFactor = scaleFactor;
		this.startAt = startAt;
	}
	
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * Stores runs we have already killed in a weak map so that they can be garbage collected if need be.
	 * The synchronization here is for memory visibility only, it doesn't
	 *
	 */
	

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return processRuns(tae.evaluateRun(runConfigs, new WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(obs)));
	}
	
	
	
	
	
	
	public List<AlgorithmRunResult> processRuns(List<AlgorithmRunResult> runs)
	{
		List<AlgorithmRunResult> myRuns = new ArrayList<AlgorithmRunResult>(runs.size());
		for(AlgorithmRunResult run : runs)
		{
			myRuns.add(processRun(run));
		}
		return myRuns;
	}
	
	public AlgorithmRunResult processRun(AlgorithmRunResult run )
	{
		if(run.getRunStatus().equals(RunStatus.KILLED))
		{
			if(run.getRuntime() == 0 && run.getWallclockExecutionTime() > startAt)
			{
		
				return new WalltimeAsRuntimeAlgorithmRun(run, wallclockMultScaleFactor);
			}
		}
		return run;
		
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
		
		tae.evaluateRunsAsync(runConfigs, myHandler, new WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(obs));

	}

	


	private class WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private TargetAlgorithmEvaluatorRunObserver obs;
		WalltimeAsRuntimeTargetAlgorithmEvaluatorObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
		}
		
		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs) 
		{
			
			List<AlgorithmRunResult> myRuns = new ArrayList<AlgorithmRunResult>(runs.size());
			
			for(AlgorithmRunResult run : runs)
			{
				
				if(run.getRunStatus().equals(RunStatus.RUNNING))
				{
					if(run.getRuntime() == 0 && run.getWallclockExecutionTime() > startAt)
					{
				
						myRuns.add(new WalltimeAsRuntimeAlgorithmRun(run, wallclockMultScaleFactor));
						
					} else
					{
						myRuns.add(run);
					}
				} else
				{
					myRuns.add(run);
				}
			}
			if(obs != null)
			{
				obs.currentStatus(myRuns);
			}
		}
		
		
	}
	
	
	private static class WalltimeAsRuntimeAlgorithmRun implements AlgorithmRunResult
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 9082975671200245863L;
		
		AlgorithmRunResult wrappedRun;
		AlgorithmRunResult wrappedKillableRun;

		private final double wallclockMultScaleFactor;  
	
		public WalltimeAsRuntimeAlgorithmRun(AlgorithmRunResult r, double wallClockMultScaleFactor)
		{
			if(r instanceof AlgorithmRunResult)
			{
				wrappedKillableRun = (AlgorithmRunResult) r;
			}
			this.wrappedRun = r;
			this.wallclockMultScaleFactor = wallClockMultScaleFactor;
		}

		@Override
		public AlgorithmRunConfiguration getAlgorithmRunConfiguration() {
			return wrappedRun.getAlgorithmRunConfiguration();
		}

		@Override
		public RunStatus getRunStatus() {
			return wrappedRun.getRunStatus();
		}

		@Override
		public double getRuntime() {
			return Math.max(wrappedRun.getWallclockExecutionTime() * wallclockMultScaleFactor,0);
		}

		@Override
		public double getRunLength() {
			return wrappedRun.getRunLength();
		}

		@Override
		public double getQuality() {
			return wrappedRun.getQuality();
		}

		@Override
		public long getResultSeed() {
			return wrappedRun.getResultSeed();
		}

		@Override
		public String getResultLine() {
			return AbstractAlgorithmRunResult.getResultLine(this);
		}

		@Override
		public String getAdditionalRunData() {
			return wrappedRun.getAdditionalRunData();
		}

		@Override
		public boolean isRunCompleted() {
			return wrappedRun.isRunCompleted();
		}

		
		@Override
		public String rawResultLine() {
			return "[Probably not accurate:]" + wrappedRun.rawResultLine();
		}

		@Override
		public double getWallclockExecutionTime() {
			return wrappedRun.getWallclockExecutionTime();
		}
		
		@Override
		public boolean isCensoredEarly() {
			return ((getRunStatus().equals(RunStatus.TIMEOUT) && getAlgorithmRunConfiguration().hasCutoffLessThanMax()) ||  (getRunStatus().equals(RunStatus.KILLED) && getRuntime() < getAlgorithmRunConfiguration().getCutoffTime()));
			
			
		}
		
		@Override
		public void kill() {
			if(wrappedKillableRun != null)
			{
				wrappedKillableRun.kill();
				
			}			
		}
		
		public String toString()
		{
			return AbstractAlgorithmRunResult.toString(this);
		}
		

		@Override
		public ParameterConfiguration getParameterConfiguration() {
			
			return wrappedRun.getParameterConfiguration();
		}


		@Override
		public AlgorithmExecutionConfiguration getAlgorithmExecutionConfiguration() {
			
			return wrappedRun.getAlgorithmExecutionConfiguration();
		}


		@Override
		public ProblemInstanceSeedPair getProblemInstanceSeedPair() {

			return wrappedRun.getProblemInstanceSeedPair();
		}


		@Override
		public ProblemInstance getProblemInstance() {
			return wrappedRun.getProblemInstance();
		}
		
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}
