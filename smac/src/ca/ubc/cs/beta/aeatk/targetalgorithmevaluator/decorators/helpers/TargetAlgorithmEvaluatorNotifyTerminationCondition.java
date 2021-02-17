package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class TargetAlgorithmEvaluatorNotifyTerminationCondition extends
		AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	private final EventManager evtManager;
	private boolean flush;
	private TerminationCondition termCond;
	

	public TargetAlgorithmEvaluatorNotifyTerminationCondition(TargetAlgorithmEvaluator tae, EventManager evtManager, TerminationCondition termCond, boolean flush) {
		super(tae);
		this.evtManager = evtManager;
		this.flush = flush;
		this.termCond = termCond;
		
	}

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected synchronized AlgorithmRunResult processRun(AlgorithmRunResult run)
	{
		termCond.notifyRun(run);
		evtManager.fireEvent(new AlgorithmRunCompletedEvent(termCond, run));
		if(flush)
		{
			evtManager.flush();
		}
		return run;
	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
	
}
