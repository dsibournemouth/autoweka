package ca.ubc.cs.beta.aeatk.probleminstance;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import ca.ubc.cs.beta.aeatk.json.serializers.ProblemInstanceJson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Immutable Class that represents a Problem Instance of a target algorithm
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@JsonSerialize(using=ProblemInstanceJson.ProblemInstanceSerializer.class)
@JsonDeserialize(using=ProblemInstanceJson.ProblemInstanceDeserializer.class)
public class ProblemInstance implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2077458754749675377L;

	
	/**
	 * Unique Identifier for this instance
	 */
	private final String instanceName;
	
	/**
	 * An instance id
	 * @deprecated this is used primarily for MATLAB compatibility, one day this will disappear
	 * The giant problem with this is that when loading multiple state restores in surrogates
	 * we can't ensure that this are consistent in any great way.
	 */
	private final int instanceId;

	/**
	 * Array representation of all the features
	 */
	private final double[] featuresDouble;
	
	/**
	 * String that contains additional information regarding this instance
	 * 
	 * This will be passed to the Target Algorithm and is of no consequence to us generally.
	 */
	private final String instanceSpecificInformation;
	
	/**
	 * Unmodifiable Map containing our features
	 * 
	 * <b>NOTE:</b> this map MUST have a consistent iteration order
	 */
	private final Map<String,Double> featuresMap;
	
	/**
	 * Standard Constructor
	 * @param instanceName  unique name for this instance
	 */
	public ProblemInstance(String instanceName)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = 0;
		this.featuresMap = Collections.emptyMap();
		this.featuresDouble = new double[0];
		this.instanceSpecificInformation = "0";
	}
	
	/**
	 * Standard Constructor
	 * @param instanceName  unique name for this instance
	 * @param instanceSpecificInformation instance specific information
	 */
	public ProblemInstance(String instanceName, String instanceSpecificInformation)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = 0;
		this.featuresMap = Collections.emptyMap();
		this.featuresDouble = new double[0];
		if((instanceSpecificInformation == null) || (instanceSpecificInformation.trim().length() == 0))
		{
			instanceSpecificInformation = "0";
		}
		this.instanceSpecificInformation = instanceSpecificInformation;
	}
	
	/**
	 * Deprecated Constructor supporting IDs
	 * @param instanceName  unique name for this instance
	 * @param id 			numeric identifier for this instance
	 * @deprecated We will be moving away from id's eventually 
	 */
	public ProblemInstance(String instanceName, int id)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = id;
		this.featuresMap = Collections.emptyMap();
		this.featuresDouble = new double[0];
		this.instanceSpecificInformation = "0";
		
	}
	
	/**
	 * Constructor supporting features  
	 * 
	 * NOTE: In general when supply features it is an error to give two instances the same name but different features, this may result in unexpected behaivor
	 * 
	 * @param instanceName	unique name for this instance
	 * @param id			numeric id for this instance (will be removed)
	 * @param features		map for this instance containing string value pairs
	 * 
	 */
	public ProblemInstance(String instanceName, int id, Map<String, Double> features)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = id;
		
		this.featuresMap = features;
		this.featuresDouble = new double[features.size()];
	
		int i=0;
		for(Entry<String, Double> ent : features.entrySet())
		{
			featuresDouble[i++] = ent.getValue();
		}
		this.instanceSpecificInformation = "0";
		
	}
	
	/**
	 * Constructor supporting features  
	 * 
	 * NOTE: In general when supply features & instanceSpecificInformation it is an error to give two instances the same name but different features, this may result in unexpected behaivour
	 * 
	 * @param instanceName					unique name for this instance
	 * @param id							numeric id for this instance (will be removed)
	 * @param features						map for this instance containing string value pairs
	 * @param instanceSpecificInformation	string containing additional information about this instance
	 */
	public ProblemInstance(String instanceName, int id, Map<String, Double> features, String instanceSpecificInformation)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance name cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = id;
		
		this.featuresMap = features;
		this.featuresDouble = new double[features.size()];
	
		int i=0;
		for(Entry<String, Double> ent : features.entrySet())
		{
			featuresDouble[i++] = ent.getValue();
		}
		if(instanceSpecificInformation == null)
		{
			this.instanceSpecificInformation = "0";
		} else
		{
			this.instanceSpecificInformation = instanceSpecificInformation;
		}
		
		
	}
	
	/**
	 * Constructor supporting features  
	 * 
	 * NOTE: In general when supply features & instanceSpecificInformation it is an error to give two instances the same name but different features, this may result in unexpected behaivour
	 * 
	 * @param instanceName					unique name for this instance
	 * @param id							numeric id for this instance (will be removed)
	 * @param features						map for this instance containing string value pairs
	 * @param instanceSpecificInformation	string containing additional information about this instance
	 */
	public ProblemInstance(String instanceName, Map<String, Double> features, String instanceSpecificInformation)
	{
		if (instanceName == null)
		{
			throw new IllegalArgumentException("Instance cannot be null");
		}
		this.instanceName = instanceName;
		this.instanceId = 0;
		
		this.featuresMap = features;
		this.featuresDouble = new double[features.size()];
	
		int i=0;
		for(Entry<String, Double> ent : features.entrySet())
		{
			featuresDouble[i++] = ent.getValue();
		}
		if(instanceSpecificInformation == null)
		{
			this.instanceSpecificInformation = "0";
		} else
		{
			this.instanceSpecificInformation = instanceSpecificInformation;
		}
		
		
	}
	
	/**
	 * Gets the Instance Name of the object
	 * @return unique instance name
	 */
	public String getInstanceName()
	{
		return this.instanceName;
	}
	
	/**
	 * Two instances are considered equal if they share the same instance name
	 */ 
	@Override
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if(o instanceof ProblemInstance)
		{
			ProblemInstance ai = (ProblemInstance) o;
			return instanceName.equals(ai.instanceName);
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return instanceName.hashCode();
	}
	
	@Override
	public String toString()
	{
		if(instanceId != 0)
		{
			return "Instance(" + instanceId + "):"+instanceName;
		} else
		{
			return "Instance:"+instanceName;
		}
	}

	/**
	 * Returns the Instance ID
	 * @deprecated this should not be relied upon as it's ugly and hacky.
	 * @return instance id
	 */
	public int getInstanceID() {
		return instanceId;
	}

	/**
	 * Returns the feature map
	 * @return map containing all the features
	 */
	public Map<String, Double> getFeatures()
	{
		return featuresMap;
	}
	
	/**
	 * Returns a copy of all features in array format
	 * 
	 * @return a copy of the features of the instance in array format
	 */
	public double[] getFeaturesDouble()
	{
		return featuresDouble.clone();
	}

	/**
	 * Returns the Instance Specific Information
	 * @return string if specified or 0 if none was provided
	 */
	public String getInstanceSpecificInformation() 
	{
		return instanceSpecificInformation;
	}
}
