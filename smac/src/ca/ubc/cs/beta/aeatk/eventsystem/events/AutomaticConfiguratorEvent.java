package ca.ubc.cs.beta.aeatk.eventsystem.events;

import java.util.UUID;

public abstract class AutomaticConfiguratorEvent {

	private final UUID uuid;
	public AutomaticConfiguratorEvent()
	{
		this.uuid = getUUID();
	}
	
	public UUID getUUID()
	{
		return uuid;
	}
}
