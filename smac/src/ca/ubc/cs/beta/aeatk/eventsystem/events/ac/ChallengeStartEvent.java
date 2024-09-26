package ca.ubc.cs.beta.aeatk.eventsystem.events.ac;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class ChallengeStartEvent extends AbstractTimeEvent{

	
	private final ParameterConfiguration challenger;
	

	public ChallengeStartEvent(TerminationCondition cond, ParameterConfiguration challenger) 
	{
		super(cond);
		
		this.challenger = challenger;
		
	}


	public ParameterConfiguration getChallenger()
	{
		return challenger;
	}
	
		
}
