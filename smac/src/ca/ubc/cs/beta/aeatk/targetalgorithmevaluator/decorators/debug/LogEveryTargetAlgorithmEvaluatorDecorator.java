package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;

@ThreadSafe
public class LogEveryTargetAlgorithmEvaluatorDecorator extends
		AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	Logger log = LoggerFactory.getLogger(LogEveryTargetAlgorithmEvaluatorDecorator.class);
	
	private final boolean logRCOnly;
	
	private final String context;
	
	public LogEveryTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae) {
		this(tae, false);
	}
	public LogEveryTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, String context) {
		this(tae, context, false);
	}
	
	public LogEveryTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, boolean logRequestResponsesRCOnly) {
		super(tae);
		this.context = "";
		this.logRCOnly = logRequestResponsesRCOnly;
	}

	public LogEveryTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, String context, boolean logRequestResponsesRCOnly) {
		super(tae);
		this.logRCOnly = logRequestResponsesRCOnly;
		this.context = (context != null) ? "(" +context+") " : "";
	}

	
	protected AlgorithmRunResult processRun(AlgorithmRunResult run)
	{
		if(logRCOnly)
		{
			//log.debug("Run {}Completed: {} ", context, run.getAlgorithmRunConfiguration());
		} else
		{
			//log.debug("Run {}Completed: {} : {} ", context, run, run.getAdditionalRunData());
		}
		return run;
	}
	
	protected AlgorithmRunConfiguration processRun(AlgorithmRunConfiguration rc)
	{
		log.debug("Run {}Scheduled: {} ", context, rc);
		return rc;
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}
