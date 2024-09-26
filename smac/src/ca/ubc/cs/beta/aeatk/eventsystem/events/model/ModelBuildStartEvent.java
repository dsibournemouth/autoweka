package ca.ubc.cs.beta.aeatk.eventsystem.events.model;


import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class ModelBuildStartEvent extends AbstractTimeEvent
{

	public ModelBuildStartEvent(TerminationCondition cond) {
		super(cond);
	}
	

}
