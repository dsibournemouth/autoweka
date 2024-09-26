package ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;

public class NullParamConfigurationOriginTracker implements
		ParamConfigurationOriginTracker {

	@Override
	public void addConfiguration(ParameterConfiguration config, String origin,
			String... addlData) {
		
	}

	@Override
	public Map<String, String> getOrigins(ParameterConfiguration config) {
		return Collections.emptyMap();
	}

	@Override
	public Long getCreationTime(ParameterConfiguration config) {

		return 0L;
	}

	@Override
	public Set<String> getOriginNames() {
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<ParameterConfiguration> iterator() {

		return Collections.EMPTY_LIST.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int getGenerationCount(ParameterConfiguration config) {
		return 0;
	}

}
