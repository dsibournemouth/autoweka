package ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ca.ubc.cs.beta.aeatk.json.serializers.AlgorithmExecutionConfigurationJson;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
/**
 * Immutable Object contains all the information related to executing a target algorithm run
 * @author seramage
 *
 */
@SuppressWarnings("unused")
@JsonSerialize(using=AlgorithmExecutionConfigurationJson.AlgorithmExecutionConfigSerializer.class)
@JsonDeserialize(using=AlgorithmExecutionConfigurationJson.AlgorithmExecutionConfigDeserializer.class)
public class AlgorithmExecutionConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3L;
	
	private final String algorithmExecutable;
	private final String algorithmExecutionDirectory;
	private final ParameterConfigurationSpace paramFile;
		private final boolean deterministicAlgorithm;

	private final double cutoffTime; 

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<String, String> taeContext;
	
	public AlgorithmExecutionConfiguration(String algorithmExecutable, String algorithmExecutionDirectory,
			ParameterConfigurationSpace paramFile, boolean executeOnCluster, boolean deterministicAlgorithm, double cutoffTime) {
		this(algorithmExecutable, algorithmExecutionDirectory, paramFile, deterministicAlgorithm, cutoffTime, Collections.EMPTY_MAP);
		
	}
	

	public AlgorithmExecutionConfiguration(String algorithmExecutable, String algorithmExecutionDirectory,	ParameterConfigurationSpace paramFile, boolean deterministicAlgorithm, double cutoffTime, Map<String, String> taeContext) {
		this.algorithmExecutable = algorithmExecutable;
		this.algorithmExecutionDirectory = algorithmExecutionDirectory;
		this.paramFile = paramFile;
		
		this.deterministicAlgorithm = deterministicAlgorithm;
		if(cutoffTime < 0)
		{
			throw new IllegalArgumentException("Cutoff time must be greater than zero");
		}
		
		if(cutoffTime == 0)
		{
			log.warn("Cutoff time should be greater than zero");
		}
		this.cutoffTime = cutoffTime;
		
		this.taeContext = new TreeMap<String, String>(taeContext);
	}

	public String getAlgorithmExecutable() {
		return algorithmExecutable;
	}

	public String getAlgorithmExecutionDirectory() {
		return algorithmExecutionDirectory;
	}

	public ParameterConfigurationSpace getParameterConfigurationSpace() {
		return paramFile;
	}

	public boolean isDeterministicAlgorithm()
	{
		return deterministicAlgorithm;
	}
	
	/**
	 * Additional context information necessary to execute runs, this is TAE dependent.
	 * @return
	 */
	public Map<String, String> getTargetAlgorithmExecutionContext()
	{
		return Collections.unmodifiableMap(this.taeContext);
	}
	
	public int hashCode()
	{
		return algorithmExecutable.hashCode() ^ algorithmExecutionDirectory.hashCode() ^ paramFile.hashCode() ^ (deterministicAlgorithm ? 0 : 1) ^ taeContext.hashCode();
	}
	
	public String toString()
	{
		return "algoExec:" + algorithmExecutable + "\nAlgorithmExecutionDirectory:" + algorithmExecutionDirectory + "\n"+paramFile +  "\nDetermininstic:" + deterministicAlgorithm + "\nID:" + myID + " MapSize:" + taeContext.size();
	}
	
	public boolean equals(Object o)
	{ 
		if(this == o) return true;
		if (o instanceof AlgorithmExecutionConfiguration)
		{
			AlgorithmExecutionConfiguration co = (AlgorithmExecutionConfiguration) o;
			return (co.algorithmExecutable.equals(algorithmExecutable) && co.algorithmExecutionDirectory.equals(algorithmExecutionDirectory) && co.paramFile.equals(paramFile)) && co.deterministicAlgorithm == deterministicAlgorithm  && co.taeContext.equals(taeContext);
		} 
		return false;
	}
	
	/**
	 * Returns the maximum cutoff time
	 * @return maximum cutoff time for the algorithm
	 */
	public double getAlgorithmMaximumCutoffTime() {
		return cutoffTime;
	}
	
	
	//public final static String MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX = "Who am I, Alan Turing?...also from X-Men?";
	
	
	
	private static final AtomicInteger idPool = new AtomicInteger(0);
	private int myID = idPool.incrementAndGet();	
	
	/**
	 * Friendly IDs are just unique numbers that identify this configuration for logging purposes
	 * you should <b>NEVER</b> rely on this for programatic purposes.
	 * 
	 * @return unique id for this object
	 */
	public int getFriendlyID() {
		return myID;
	}

	public String getFriendlyIDHex()
	{
		String hex = Integer.toHexString(getFriendlyID());
		
		StringBuilder sb = new StringBuilder("0x");
		while(hex.length() + sb.length() < 6)
		{
			sb.append("0");
		}
		sb.append(hex.toUpperCase());
		return sb.toString();
	}
	
	
	
	
}
