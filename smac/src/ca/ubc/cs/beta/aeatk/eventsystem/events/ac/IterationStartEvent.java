package ca.ubc.cs.beta.aeatk.eventsystem.events.ac;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class IterationStartEvent extends AbstractTimeEvent {

	private final int iteration;

	public IterationStartEvent(TerminationCondition cond, int iteration) {
		super(cond);
		this.iteration = iteration;
	}

	public int getIteration()
	{
		return iteration;
	}
}
