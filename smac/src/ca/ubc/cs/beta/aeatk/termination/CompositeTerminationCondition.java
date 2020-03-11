package ca.ubc.cs.beta.aeatk.termination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;

public class CompositeTerminationCondition implements TerminationCondition {

	
	private final LinkedHashSet<TerminationCondition> conditions;
	
	public CompositeTerminationCondition(TerminationCondition c)
	{
		this(Collections.singletonList(c));
	}
	
	public CompositeTerminationCondition(Collection<? extends TerminationCondition> conditions)
	{
		if(conditions.size() == 0 )
		{
			throw new IllegalArgumentException("Must have atleast one condition");
		}
		
		this.conditions = new LinkedHashSet<TerminationCondition>(conditions);
		
	}
	@Override
	public boolean haveToStop() {
		for(TerminationCondition c : conditions)
		{
			if(c.haveToStop())
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		List<ValueMaxStatus> statuses = new ArrayList<ValueMaxStatus>();
		
		for(TerminationCondition c : conditions)
		{
			statuses.addAll(c.currentStatus());
		}
		return statuses;
		
	}

	public String toString()
	{
		return currentStatus().toString();
	}
	@Override
	public void registerWithEventManager(EventManager evtManager) {
		for(TerminationCondition c : conditions)
		{
			c.registerWithEventManager(evtManager);
		}
	}
	@Override
	public void notifyRun(AlgorithmRunResult run) {
		for(TerminationCondition c : conditions)
		{
			c.notifyRun(run);
		}
	}
	@Override
	public double getTunerTime() {
		double tunerTime = Double.MIN_VALUE;
		for(TerminationCondition c : conditions)
		{
			tunerTime = Math.max(tunerTime, c.getTunerTime());
		}
		return tunerTime;
	}
	
	@Override
	public double getWallTime() {
		double wallclockTime = Double.MIN_VALUE;
		for(TerminationCondition c : conditions)
		{
			wallclockTime = Math.max(wallclockTime, c.getWallTime());
		}
		return wallclockTime;
	}
	
	@Override
	public String getTerminationReason() {
		StringBuilder sb = new StringBuilder();
		
		for(TerminationCondition c : conditions)
		{
			if(c.haveToStop())
			{
				sb.append(c.getTerminationReason()).append(" & ");
			}
		}

		sb.setCharAt(sb.length()-2,' ');
		
		return sb.toString();
	}
	
	/**
	 * Adds a condition to the composite
	 * @param cond
	 */
	public synchronized void addCondition(TerminationCondition cond)
	{
		this.conditions.add(cond);
	}
	
}
