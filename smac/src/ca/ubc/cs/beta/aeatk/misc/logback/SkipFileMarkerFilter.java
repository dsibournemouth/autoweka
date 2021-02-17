package ca.ubc.cs.beta.aeatk.misc.logback;

import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class SkipFileMarkerFilter extends Filter<ILoggingEvent> {

	@Override
	public FilterReply decide(ILoggingEvent arg0) {
		if(arg0.getMarker() != null && arg0.getMarker().equals(CommonMarkers.SKIP_FILE_PRINTING))
		{
			return FilterReply.DENY;
		} else
		{
			return FilterReply.NEUTRAL;
		}
	}

}
