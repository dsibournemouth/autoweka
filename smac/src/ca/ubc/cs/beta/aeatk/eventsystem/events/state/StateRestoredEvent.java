package ca.ubc.cs.beta.aeatk.eventsystem.events.state;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

/**
 * Event that is fired when state has been restored
 * <br>
 * <b>NOTE:</b> This event should always be flushed when fired, so that everything handling it can get a consistent view of the runHistory.
 *  
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class StateRestoredEvent extends AbstractTimeEvent {

	private int modelsBuilt;
	private ThreadSafeRunHistory runHistory;
	private ParameterConfiguration incumbent;

	public StateRestoredEvent(TerminationCondition cond,  int modelsBuilt, ThreadSafeRunHistory runHistory, ParameterConfiguration incumbent) {
		super(cond);
		this.modelsBuilt = modelsBuilt;
		this.runHistory = runHistory;
		this.incumbent = incumbent;
		
	}

	public int getModelsBuilt() {
		return modelsBuilt;
	}

	public ThreadSafeRunHistory getRunHistory() {
		return runHistory;
	}
	
	public ParameterConfiguration getIncumbent()
	{
		return incumbent;
		
	}

	
	 
	

}
