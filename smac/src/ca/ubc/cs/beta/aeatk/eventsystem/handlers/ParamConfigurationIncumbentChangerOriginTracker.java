package ca.ubc.cs.beta.aeatk.eventsystem.handlers;

import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;

public class ParamConfigurationIncumbentChangerOriginTracker implements	EventHandler<IncumbentPerformanceChangeEvent>
{
	

	private final ParamConfigurationOriginTracker configTracker;

	private final ThreadSafeRunHistory runHistory;

	private double cutoffTime;
	public ParamConfigurationIncumbentChangerOriginTracker(ParamConfigurationOriginTracker configTracker, ThreadSafeRunHistory runHistory, double cutoffTime)
	{
		this.configTracker = configTracker;
		this.runHistory = runHistory;
		this.cutoffTime = cutoffTime;
		
	}
	
	
	ParameterConfiguration lastIncumbent;
	@Override
	public synchronized void handleEvent(IncumbentPerformanceChangeEvent event) 
	{
		
		
		if(event.incumbentChanged())
		{
			this.configTracker.addConfiguration(event.getIncumbent(), "Incumbent", "Performance="+event.getEmpiricalPerformance(), "Runs=" + event.getIncumbentRunCount());
			
			if(lastIncumbent != null)
			{
				runHistory.readLock();
				try {
				this.configTracker.addConfiguration(lastIncumbent, "Displaced Incumbent", "Performance=" + runHistory.getEmpiricalCost(lastIncumbent, runHistory.getProblemInstancesRan(lastIncumbent), cutoffTime),"Runs=" + runHistory.getTotalNumRunsOfConfigExcludingRedundant(lastIncumbent));
				} finally
				{
					runHistory.releaseReadLock();
				}
			}
			
			lastIncumbent = event.getIncumbent();

			
		}
				
	}
}

