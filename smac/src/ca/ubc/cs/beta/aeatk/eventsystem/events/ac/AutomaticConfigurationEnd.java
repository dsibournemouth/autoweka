package ca.ubc.cs.beta.aeatk.eventsystem.events.ac;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AbstractTimeEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;

public class AutomaticConfigurationEnd extends AbstractTimeEvent {

	private final ParameterConfiguration incumbent;
	private final double empiricalPerformance;

	/**
	 * Constructs an Automatic Configuration Event
	 * @param incumbent
	 * @param empiricalPerformance
	 * @param termCond 
	 */
	public AutomaticConfigurationEnd(TerminationCondition termCond,ParameterConfiguration incumbent, double empiricalPerformance) {
		super(termCond);
		this.incumbent = incumbent;
		this.empiricalPerformance = empiricalPerformance;
	}

	

	/**
	 * Constructs an Automatic Configuration Event
	 * @param incumbent
	 * @param empiricalPerformance
	 * @param wallClockTime
	 * @param tunerTime
	 * @deprecated use the Termination Condition constructor
	 */
	public AutomaticConfigurationEnd(ParameterConfiguration incumbent, double empiricalPerformance, long wallClockTime, double tunerTime) {
		super(tunerTime, wallClockTime);
		this.incumbent = incumbent;
		this.empiricalPerformance = empiricalPerformance;
	}

	public ParameterConfiguration getIncumbent() {
		return incumbent;
	}
	
	public double getEmpiricalPerformance()
	{
		return empiricalPerformance;
	}

}
