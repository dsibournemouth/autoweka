package ca.ubc.cs.beta.aeatk.watchdog;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;


@UsageTextField(title="Thread Watch Dog Options", description="Options that control a thread that monitors the liveness of the application ")
public class ThreadWatchDogOptions extends AbstractOptions {
	
	@Parameter(names="--watchdog-enable", description="Enable Watchdog")
	public boolean watchDog = false;
	
	/**
	 * This doesn't work quite the way I want it to at the moment, will probably need to refactor but at least I get a thread dump
	 */
	
	@Parameter(names="--watchdog-last-event-delay", description="How long in seconds since the last event before we detect a problem", validateWith=NonNegativeInteger.class, hidden = true)
	public int lastEventDelay = 3600;
	
	@Parameter(names="--watchdog-delay-between-checks", description="How often in seconds we should check if there is a problem", validateWith=FixedPositiveInteger.class , hidden = true)
	public int delayBetweenChecks = 600;
	
	@Parameter(names="--watchdog-delay-before-shutdown", description="If we detect a problem how long (in seconds) after before we shutdown", validateWith=NonNegativeInteger.class, hidden=true)
	public int delayBeforeShutdown = 600;
	
	@Parameter(names="--watchdog-log-thread-activity", description="How often to log the thread activity")
	public int logThreadActivity = 300;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ThreadWatchDog<? extends AutomaticConfiguratorEvent> getWatchDog()
	{
		if(watchDog)
		{
			return new LiveThreadWatchDog(lastEventDelay, delayBetweenChecks, delayBeforeShutdown, logThreadActivity);
		} else
		{
			return new NullThreadWatchDog();
			
		}
	}
	
	
	public <K extends AutomaticConfiguratorEvent> ThreadWatchDog<K> getWatchDog(Class<K> t)
	{
		if(watchDog)
		{
			return new LiveThreadWatchDog<K>(lastEventDelay, delayBetweenChecks, delayBeforeShutdown, logThreadActivity);
		} else
		{
			return new NullThreadWatchDog<K>();
			
		}
	}
	
	
	
 
}
