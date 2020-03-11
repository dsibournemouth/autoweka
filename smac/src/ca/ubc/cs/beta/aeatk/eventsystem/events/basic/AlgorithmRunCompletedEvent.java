package ca.ubc.cs.beta.aeatk.eventsystem.events.basic;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class AlgorithmRunCompletedEvent extends AbstractTimeEvent {

	private final AlgorithmRunResult run;
	
	
	public AlgorithmRunCompletedEvent(TerminationCondition cond,AlgorithmRunResult run) {
		super(cond);
		this.run = run;
		
	}
	
	public AlgorithmRunResult getRun() {
		return run;
	}
	
	

}
