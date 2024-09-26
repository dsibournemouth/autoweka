package ca.ubc.cs.beta.aeatk.algorithmrunconfiguration;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.json.serializers.AlgorithmRunConfigurationJson;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
/**
 * Immutable class that contains all the information necessary for a target algorithm run.
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@JsonSerialize(using=AlgorithmRunConfigurationJson.RunConfigSerializer.class)
@JsonDeserialize(using=AlgorithmRunConfigurationJson.RunConfigDeserializer.class)
public class AlgorithmRunConfiguration implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7749039021874017859L;
	private final ProblemInstanceSeedPair pisp;
	private final double cutoffTime;
	private final ParameterConfiguration params;
	private final boolean cutoffLessThanMax;
	private final AlgorithmExecutionConfiguration algorithmExecutionConfiguration;

	/**
	 * Creates a RunConfig object with the following attributes
	 * @param pisp 			problem instance and seed that we will run against
	 * @param cutoffTime 	double representing the amount of time to execute for (in seconds)
	 * @param config 		paramconfiguration of the target algorithm
	 * @param execConfig	execution configuration the run represents
	 */
	public AlgorithmRunConfiguration(ProblemInstanceSeedPair pisp, double cutoffTime, ParameterConfiguration config, AlgorithmExecutionConfiguration execConfig)
	{
		if(pisp == null)
		{
			throw new IllegalArgumentException("ProblemInstanceSeedPair cannot be null");
		}
		
		if(config == null)
		{
			throw new IllegalArgumentException("ParamConfiguration cannot be null");
		}
		
		if(cutoffTime  < 0)
		{
			throw new IllegalArgumentException("Cutoff time must be non-negative positive");
		}
		
		
		if(execConfig == null)
		{
			throw new IllegalArgumentException("Algorithm Execution Configuration cannot be null");
		}
		if(!config.getParameterConfigurationSpace().equals(execConfig.getParameterConfigurationSpace()))
		{
			throw new IllegalArgumentException("Configuration Space of ParamConfiguration, and that of the AlgorithmExecutionConfig object need to be the same.");
		}
		
		this.pisp = pisp;
		this.cutoffTime = cutoffTime;
		if(config.isLocked())
		{
			this.params = config;
		} else
		{
			this.params = config.copy();
			this.params.lock();
		}
		
		
		
		
		this.cutoffLessThanMax = cutoffTime < execConfig.getAlgorithmMaximumCutoffTime();
		this.algorithmExecutionConfiguration = execConfig;
	}
	
	/**
	 * Creates a RunConfig object which uses the execution configuration cutoff time
	 * @param pisp 					problem instance and seed that we will run against
	 * @param config 				paramconfiguration of the target algorithm
	 * @param execConfig			execution configuration 
	 */
	public AlgorithmRunConfiguration(ProblemInstanceSeedPair pisp, ParameterConfiguration config, AlgorithmExecutionConfiguration execConfig)
	{
		this(pisp,execConfig.getAlgorithmMaximumCutoffTime(), config, execConfig);
	}


	/**
	 * 
	 * @return ProblemInstanceSeedPair of the run
	 */
	public ProblemInstanceSeedPair getProblemInstanceSeedPair()
	{
		return pisp;
	}

	/**
	 * 
	 * @return cutoffTime of the run
	 */
	public double getCutoffTime() {
		return cutoffTime;
	}

	/**
	 * Returns an immutable instance of the parameter configuration
	 * @return an immutable instance of the parameter configuration
	 */
	public ParameterConfiguration getParameterConfiguration()
	{
		return params;
	}
	
	/**
	 * @return <code>true</code> if this run has a cutoff less than the cutoff time less than kappaMax.
	 */
	public boolean hasCutoffLessThanMax()
	{
		return cutoffLessThanMax;
	}
	
	/**
	 * @return <code>true</code> if the two runconfigs have the same cutoffTime, probleminstanceseedpair, and param configuration, <code>false</code> otherwise
	 */
	@Override
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if (o instanceof AlgorithmRunConfiguration)
		{
			AlgorithmRunConfiguration oar = (AlgorithmRunConfiguration) o;
			return (pisp.equals(oar.pisp)) && cutoffTime == oar.cutoffTime && params.equals(oar.params) && algorithmExecutionConfiguration.equals(oar.algorithmExecutionConfiguration);
		} else
		{
			return false;
		}
	} 
	
	
	@Override
	public int hashCode()
	{
		/*
		 * Due to adaptive Capping and floating point issues, we don't consider the cutofftime as part of the hashcode.
		 * Theoretically this may cause certain performance issues in hash based collections
		 * however it is hoped that the number of re-runs with increasing cap times is small.
		 */
	
		return (int) ( (pisp.hashCode())^ params.hashCode() + 37*this.algorithmExecutionConfiguration.hashCode()) ;
	}

	
	@Override
	public String toString()
	{
		int instID = this.getProblemInstanceSeedPair().getProblemInstance().getInstanceID();
		long seed = this.getProblemInstanceSeedPair().getSeed();
		String confID = this.params.getFriendlyIDHex();
		StringBuilder sb = new StringBuilder();
		sb.append("<Instance:" +instID + ", Seed:" + seed + ", Config:" + confID+", Kappa:" + cutoffTime+ ", Execution Config: " + this.getAlgorithmExecutionConfiguration().getFriendlyIDHex() + ">");
		return sb.toString();
		
	}
	
	/**
	 * Returns a user friendly representation of this run object
	 * @return friendly representation of this object
	 */
	public String getFriendlyRunInfo()
	{
		int instID = this.getProblemInstanceSeedPair().getProblemInstance().getInstanceID();
		long seed = this.getProblemInstanceSeedPair().getSeed();
		String confID = this.params.getFriendlyIDHex();
		return "Run for Instance (" + instID + ") Config (" +confID + ") Seed: (" + seed +")";   
	}

	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfiguration() {
		return this.algorithmExecutionConfiguration;
	}
	
	
	

}
