package ca.ubc.cs.beta.aeatk.termination.standard;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public abstract class AbstractTerminationCondition implements TerminationCondition {


	@Override
	public void registerWithEventManager(EventManager evtManager) {
		//noop
	}

	@Override
	public void notifyRun(AlgorithmRunResult run) {
		//noop
	}

	@Override
	public double getTunerTime()
	{
		return Double.MIN_VALUE;
	}
	
	@Override
	public double getWallTime()
	{
		return Double.MIN_VALUE;
	}
}
