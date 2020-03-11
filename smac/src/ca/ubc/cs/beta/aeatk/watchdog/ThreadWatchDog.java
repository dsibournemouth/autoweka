package ca.ubc.cs.beta.aeatk.watchdog;

import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;

public interface ThreadWatchDog<K extends AutomaticConfiguratorEvent> extends EventHandler<K>{

	public void handleEvent(K event);

	public void registerCurrentThread();

	public void registerThread(Thread t);

}