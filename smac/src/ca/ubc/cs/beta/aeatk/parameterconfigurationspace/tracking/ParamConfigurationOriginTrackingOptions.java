package ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking;

import java.io.File;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.AutomaticConfigurationEnd;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.handlers.ParamConfigurationIncumbentChangerOriginTracker;
import ca.ubc.cs.beta.aeatk.eventsystem.handlers.ParamConfigurationOriginLogger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;


@UsageTextField(hiddenSection=true)
public class ParamConfigurationOriginTrackingOptions extends AbstractOptions {

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@Parameter(names={"--config-tracking"}, description="Take measurements of configuration as it goes through it's lifecycle and write to file (in state folder)")
	public boolean configTracking;
	
	
	public ParamConfigurationOriginTracker getTracker(EventManager eventManager, ParameterConfiguration initialIncumbent, String outputDir, ThreadSafeRunHistory rh, AlgorithmExecutionConfiguration execConfig, int numRun)
	{
		if(configTracking)
		{
			ParamConfigurationOriginTracker configTracker = new RealParamConfigurationOriginTracker();
			configTracker.addConfiguration(initialIncumbent, "DEFAULT", "true");
			eventManager.registerHandler(AutomaticConfigurationEnd.class, new ParamConfigurationOriginLogger(configTracker, outputDir + File.separator + "state-run" + numRun + File.separator , rh, System.currentTimeMillis(), execConfig.getAlgorithmMaximumCutoffTime()));
			eventManager.registerHandler(IncumbentPerformanceChangeEvent.class, new ParamConfigurationIncumbentChangerOriginTracker(configTracker, rh, execConfig.getAlgorithmMaximumCutoffTime()));
			return configTracker;
		} else
		{
			return new NullParamConfigurationOriginTracker();
		}
	}
}
