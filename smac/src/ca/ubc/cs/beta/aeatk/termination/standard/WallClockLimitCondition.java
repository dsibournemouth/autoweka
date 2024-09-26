package ca.ubc.cs.beta.aeatk.termination.standard;

import java.util.Collection;
import java.util.Collections;

import ca.ubc.cs.beta.aeatk.termination.ConditionType;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;
import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;

public class WallClockLimitCondition extends AbstractTerminationCondition implements TerminationCondition {

	private final long applicationStartTime;
	private final long applicationEndTime;
	private final double runTimeInSeconds;
	
	public WallClockLimitCondition(long applicationStartTime, double runTimeInSeconds)
	{
		this.applicationStartTime = applicationStartTime;
		this.applicationEndTime = applicationStartTime +  (long) (runTimeInSeconds *1000);
		this.runTimeInSeconds = runTimeInSeconds;
		
	}
	@Override
	public boolean haveToStop() {
		return (System.currentTimeMillis() >= applicationEndTime);
		
	}

	@Override
	public double getWallTime()
	{
		return (System.currentTimeMillis() - applicationStartTime) / 1000.0;
	}
	
	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.singleton(new ValueMaxStatus(ConditionType.WALLTIME,(System.currentTimeMillis()-applicationStartTime)/1000.0, runTimeInSeconds, "WALLCLOCK", "Wall-clock Time Budget", "s"));
	}

	public String toString()
	{
		return currentStatus().toString();
	}
	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "wall-clock time limit (" +  (System.currentTimeMillis()-applicationStartTime)/1000.0 +  " s) has been reached.";
		} else
		{
			return "";
		}
	}
	
	
}
