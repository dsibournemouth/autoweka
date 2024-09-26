package ca.ubc.cs.beta.aeatk.eventsystem.events.ac;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class ChallengeEndEvent extends AbstractTimeEvent {

	
	private final ParameterConfiguration challenger;
	private final boolean newIncumbent;
	private final int numberOfRuns; 
	
	public ChallengeEndEvent( TerminationCondition cond, ParameterConfiguration challenger, boolean newIncumbent, int numberOfRuns) {

		super(cond);
		this.challenger = challenger;
		this.newIncumbent = newIncumbent;
		this.numberOfRuns = numberOfRuns;
	}
	
	
	public ChallengeEndEvent( double tunerTime, double wallTime, ParameterConfiguration challenger, boolean newIncumbent, int numberOfRuns) {

		super(tunerTime, wallTime);
		this.challenger = challenger;
		this.newIncumbent = newIncumbent;
		this.numberOfRuns = numberOfRuns;
	}
	
	
	public ParameterConfiguration getChallenger()
	{
		return challenger;
	}
	
	public boolean newIncumbent()
	{
		return newIncumbent;
	}
	
	public int getNumberOfRuns()
	{
		return numberOfRuns;
	}

	
	
	
}
