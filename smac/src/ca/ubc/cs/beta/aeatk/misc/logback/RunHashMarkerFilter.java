package ca.ubc.cs.beta.aeatk.misc.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;


/**
 * Utility class used to filter out entries in logging related to Algorithm Run Hash Codes (used in MATLAB synchronization and debugging)
 *
 */
public class RunHashMarkerFilter extends Filter<ILoggingEvent> {

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if(event.getMarker() == null) return FilterReply.ACCEPT;
		if(!event.getMarker().getName().equals("RUN_HASH"))
		{
			return FilterReply.DENY;
		} else
		{
			return FilterReply.ACCEPT;
		}
	}

}
