package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;

/**
 * Transforms the solution quality of a crashed run to be at least a specified value.
 * 
 * The purpose of this is to guard against cases where a run crashes, and the wrapper developer has careless set the quality to 0, which looks very good.
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class CrashedSolutionQualityTransformingTargetAlgorithmEvaluatorDecorator extends
		AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	private final AtomicBoolean transformedResponseValueWarning = new AtomicBoolean(false);
	private final double crashedSolutionQualityValue;
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected AlgorithmRunResult processRun(AlgorithmRunResult run)
	{
		if(run.getRunStatus().equals(RunStatus.CRASHED))
		{
			
			if(run.getQuality() < crashedSolutionQualityValue)
			{
				if(transformedResponseValueWarning.compareAndSet(false, true))
				{
					log.warn("Detected CRASHED run. The Solution Quality of CRASHED runs will be transformed to the MAX(quality, {}), to disable this use --transform-crashed-quality false to change the value use --transform-crashed-quality-value", crashedSolutionQualityValue);
				}
				run = new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), run.getRunStatus(), run.getRuntime(),run.getRunLength(), Math.max(crashedSolutionQualityValue, run.getQuality()),run.getResultSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());
			}
			
		}
		return run;
	}
	
	public CrashedSolutionQualityTransformingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, double crashedSolutionQualityValue) {
		super(tae);
		this.crashedSolutionQualityValue = crashedSolutionQualityValue;
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		
	}

}
