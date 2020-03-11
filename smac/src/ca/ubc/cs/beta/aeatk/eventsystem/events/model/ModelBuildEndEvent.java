package ca.ubc.cs.beta.aeatk.eventsystem.events.model;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class ModelBuildEndEvent extends AbstractTimeEvent
{
	private final Object model; 

	private final Boolean logModel;
	public ModelBuildEndEvent(TerminationCondition cond) {
		super(cond);
		model = null;
		logModel = false;
	}

	public ModelBuildEndEvent(TerminationCondition cond, Object model, Boolean logModel) {
		super(cond);
		this.model = model;
		this.logModel = logModel;
	}

	public Object getModelIfAvailable()
	{
		return model;
	}
	
	public Boolean isLogModel()
	{
		return logModel;
	}
	
	
}
