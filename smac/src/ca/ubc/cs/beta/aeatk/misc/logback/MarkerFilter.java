package ca.ubc.cs.beta.aeatk.misc.logback;

import java.util.HashMap;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.misc.logging.LoggingMarker;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class MarkerFilter extends Filter<ILoggingEvent> {


	private static final Map<String, FilterReply> filterDecisions = new HashMap<String, FilterReply>();
	
	@Override
	public FilterReply decide(ILoggingEvent event) {
		
		if(event.getMarker() == null) return FilterReply.ACCEPT;
		
		FilterReply r = filterDecisions.get(event.getMarker().getName());
		if(r != null)
		{
			return r;
		} else
		{
			return FilterReply.ACCEPT;
		}
	}
	
	public static boolean log(String s)
	{
		if(filterDecisions.get(s) == null) return true;
		if(filterDecisions.get(s).equals(FilterReply.DENY)) return false;
		
		return true;
	}
	
	public static void accept(LoggingMarker s)
	{
		filterDecisions.put(s.name(), FilterReply.ACCEPT);
	}

	public static void neutral(LoggingMarker s)
	{
		filterDecisions.put(s.name(), FilterReply.NEUTRAL);
	}

	
	public static void deny(LoggingMarker s)
	{
		filterDecisions.put(s.name(), FilterReply.DENY);
	}

	 
}
