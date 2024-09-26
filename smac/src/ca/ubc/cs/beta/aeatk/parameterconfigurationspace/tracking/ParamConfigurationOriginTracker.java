package ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import net.jcip.annotations.ThreadSafe;

/**
 * Allows Point Selectors and other processes to track who created what configuration
 * 
 * Iteration order is insertion order.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public interface ParamConfigurationOriginTracker extends Iterable<ParameterConfiguration> {

	
	public void addConfiguration(ParameterConfiguration config, String origin, String... addlData);
		
	public Map<String, String> getOrigins(ParameterConfiguration config);
	
	
	public Long getCreationTime(ParameterConfiguration config);
	
	public Set<String> getOriginNames();
	
	@Override
	public Iterator<ParameterConfiguration> iterator();
	
	
	public int size();
	
	
	public int getGenerationCount(ParameterConfiguration config);
	
}
