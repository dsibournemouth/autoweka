package ca.ubc.cs.beta.aeatk.eventsystem.events.basic;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class LogRuntimeStatisticsEvent extends AbstractTimeEvent {

	public LogRuntimeStatisticsEvent(TerminationCondition cond) {
		super(cond);
	}

}
