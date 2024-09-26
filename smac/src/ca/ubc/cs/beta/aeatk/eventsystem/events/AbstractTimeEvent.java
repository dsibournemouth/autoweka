package ca.ubc.cs.beta.aeatk.eventsystem.events;

import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;


public abstract class AbstractTimeEvent extends AutomaticConfiguratorEvent {
	
	private double tunerTime;
	private double walltime;

	public AbstractTimeEvent(double tunerTime, double walltime) {
		this.tunerTime = tunerTime;
		this.walltime = walltime;
	}

	public AbstractTimeEvent(TerminationCondition cond)
	{
		this.tunerTime = cond.getTunerTime();
		this.walltime = cond.getWallTime();
	}
	public double getTunerTime()
	{
		return this.tunerTime;
	}
	
	public double getWallTime()
	{
		return this.walltime;
	}
}
