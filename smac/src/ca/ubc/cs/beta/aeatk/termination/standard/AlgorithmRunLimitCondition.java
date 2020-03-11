package ca.ubc.cs.beta.aeatk.termination.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aeatk.termination.ConditionType;
import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;

@ThreadSafe
public class AlgorithmRunLimitCondition extends AbstractTerminationCondition implements EventHandler<AlgorithmRunCompletedEvent> {

	private final String NAME = "NUMBER OF RUNS";
	private final long algorithmRunLimit;
	private final AtomicLong algorithmRuns = new AtomicLong(0);

	public AlgorithmRunLimitCondition(long algorithmRunLimit)
	{
		this.algorithmRunLimit = algorithmRunLimit;
	}
		

	@Override
	public boolean haveToStop() {
		return (algorithmRuns.get() >= algorithmRunLimit);
			
	}


	@Override
	public void handleEvent(AlgorithmRunCompletedEvent event) {
		
	}
	
	@Override
	public void notifyRun(AlgorithmRunResult run)
	{
		algorithmRuns.incrementAndGet();
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public void registerWithEventManager(EventManager evtManager) {
		//evtManager.registerHandler(AlgorithmRunCompletedEvent.class, this);
	}


	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.singleton(new ValueMaxStatus(ConditionType.NUMBER_OF_RUNS, algorithmRuns.get(), algorithmRunLimit, NAME, "Algorithm Runs", ""));
	}

	
	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "algorithm run limit (" +  algorithmRuns +  " runs) has been reached.";
		} else
		{
			return "";
		}
	}
	
}
