package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

/**
 * Stores a complete listing of runs
 * 
 * NOTE: This class also does some other stuff like selecting new runs, perhaps this should be refactored
 * but we will see later
 * 
 * @author sjr
 *
 */
public interface RunHistory {

	/**
	 * Append a run to the RunHistory Object
	 * @param run	run to log
	 * @throws DuplicateRunException  - If a previous run has a duplicate config,instance and seed. NOTE: An exception will prevent the run from being logged, but the state of the RunHistory will still be consistent
	 */
	public void append(AlgorithmRunResult run) throws DuplicateRunException;
		
	/**
	 * Get the Run Objective we are opitimizing
	 * @return RunObjective we are optimizing
	 */
	public RunObjective getRunObjective();

	/**
	 * Get the Overall objective we are optimizing
	 * @return OverallObjective we are optimizing over
	 */
	public OverallObjective getOverallObjective();

	/**
	 * Increment the iteration we are storing runs with
	 */
	public void incrementIteration();

	
	/**
	 * Get the current iteration value
	 * @return current iteration
	 */
	public int getIteration();

	/**
	 * Return the set of instances we have run a ParamConfiguration on
	 * @param config configuration to get instances for
	 * @return	set instances that were run
	 */
	public Set<ProblemInstance> getProblemInstancesRan(ParameterConfiguration config);

	/**
	 * Returns a copy of the set of instance seed pairs we have run a Param Configuration on.
	 * @param config	configuration to get ProblemInstanceSeedPairs for
	 * @return	set of ProblemInstanceSeedPairs
	 */
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(ParameterConfiguration config);
	
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for
	 * @param config  		 ParamConfiguration to get Cost of
	 * @param instanceSet 	 instances to compute cost over
	 * @param cutoffTime 	 cutoff time for algorithm runs
	 * @deprecated Not implemented currently as there is a bug in the interface and will need to be refactored at a later point in time. Essentially it will erroneusly include other seeds and throw off the bound.
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	public double getEmpiricalCostLowerBound(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime);
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for
	 * @param config  		 ParamConfiguration to get Cost of
	 * @param instanceSet 	 instances to compute cost over
	 * @param cutoffTime 	 cutoff time for algorithm runs
	 * @deprecated Not implemented currently as there is a bug in the interface and will need to be refactored at a later point in time. Essentially it will erroneusly include other seeds and throw off the bound.
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	public double getEmpiricalCostUpperBound(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime);
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for
	 * @param config  		 ParamConfiguration to get Cost of
	 * @param instanceSet 	 instances to compute cost over
	 * @param cutoffTime 	 cutoff time for algorithm runs
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	public double getEmpiricalCost(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime);
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for.
	 * @param config 		ParamConfiguration to get Cost of
	 * @param instanceSet   Instances to compute cost over
	 * @param cutoffTime 	cutoff time for algorithm runs
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	double getEmpiricalCost(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime,	Map<ProblemInstance, Map<Long, Double>> hallucinatedValues);

	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for.
	 * @param config 		ParamConfiguration to get Cost of
	 * @param instanceSet   Instances to compute cost over
	 * @param cutoffTime 	cutoff time for algorithm runs
	 * @param minimumResponseValue  the minimum legal response value (all values lower than this are replaced)
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue);

	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for.
	 * @param config 		ParamConfiguration to get Cost of
	 * @param instanceSet   Instances to compute cost over
	 * @param cutoffTime 	cutoff time for algorithm runs
	 * @param minimumResponseValue  the minimum legal response value (all values lower than this are replaced)
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost
	 */
	double getEmpiricalCost(ParameterConfiguration config,	Set<ProblemInstance> instanceSet, double cutoffTime, double minimumResponseValue);
	

	
	
	
	
	/**
	 * Returns the total cost of the all the runs used.
	 * @return total run cost (sum of all run times)
	 */
	public double getTotalRunCost();

	/**
	 * Get the set of Unique instances ran
	 * @return set of unique instances ran
	 */
	public Set<ProblemInstance> getUniqueInstancesRan();
	
	/**
	 * Gets a list of Unique Param Configurations Ran
	 * @return	set of param configurations ran
	 */
	public Set<ParameterConfiguration> getUniqueParamConfigurations();
	
	/**
	 * Returns an Nx2 matrix where each row corresponds to a
	 * tuple of a param_configuration instance (in {@link ca.ubc.cs.beta.aeatk.runhistory.RunHistory#getUniqueParamConfigurations()})
	 * and an instance (in {@link ca.ubc.cs.beta.aeatk.runhistory.RunHistory#getUniqueInstancesRan()}). These represent the run configurations.
	 * @return array of entries of the form [thetaIdx, instanceIdx]
	 */
	public int[][] getParameterConfigurationInstancesRanByIndexExcludingRedundant();
	
	


	

	/**
	 * Returns a list containing all param configurations that ran in order (i.e. in order of theta idx)
	 * 
	 * The param configurations are unique
	 * 
	 * @return list of param configurations
	 */
	public List<ParameterConfiguration> getAllParameterConfigurationsRan();
	
	/**
	 * Returns all configurations run in Value Array Form
	 * 
	 * @return array of double[] where each double is the paramconfiguration value Array. Indexed by thetaIdx
	 * 
	 */
	public double[][] getAllConfigurationsRanInValueArrayForm();
	
	
	
	/**
	 * Returns a list of all the Run Data
	 * 
	 * @return	list of run data
	 */
	public List<RunData> getAlgorithmRunDataExcludingRedundant();

	/**
	 * Returns a list of all the Run Data
	 * 
	 * @return	list of run data
	 */
	public List<RunData> getAlgorithmRunDataIncludingRedundant();
	
	
	/**
	 * Returns a new list containing all the runs we have done.
	 * <p>
	 * <b>Implementation Note:</b>Implementors must return a list that clients can modify directly without
	 * corrupting internal state.
	 * 
	 * @return list of runs that we have recorded 
	 */
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant();
	
	/**
	 * Returns a new list containing all the runs we have done.
	 * <p>
	 * <b>Implementation Note:</b>Implementors must return a list that clients can modify directly without
	 * corrupting internal state.
	 * 
	 * @return list of runs that we have recorded 
	 */
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant();
	
	/**
	 * Returns the total number of runs for a configuration, ignoring early capped runs that have been replaced with better capped data.
	 * @param config ParamConfiguration
	 * @return number of runs
	 */
	public int getTotalNumRunsOfConfigExcludingRedundant(ParameterConfiguration config);
	
	/**
	 * Returns the total number of runs for a configuration, , containing early capped runs.
	 * @param config ParamConfiguration
	 * @return number of runs
	 */
	public int getTotalNumRunsOfConfigIncludingRedundant(ParameterConfiguration config);
	
	/**
	 * Returns an unmodifiable list of run data for challenger, ignoring early capped runs that have been replaced with better capped data.
	 * @param config
	 * @return 	list of algorithms for the configuration
	 */
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant(ParameterConfiguration config);

	/**
	 * Returns an unmodifiable list of run data for challenger, containing early capped runs.
	 * @param config
	 * @return 	list of algorithms for the configuration
	 */
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant(ParameterConfiguration config);
	

	/**
	 * Returns a set of Instance Seed Pairs that were capped for a given configuration
	 * 
	 * @param config	paramconfiguration to select
	 * @return	set of instance seed pairs that are capped runs
	 */
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(ParameterConfiguration config);

	/**
	 * Returns the Index into arrays represented by this configuration
	 * 
	 * @param configuration 	Configuration needed (must exist in RunHistory)
	 * @return index into the theta array for this configuration
	 */
	public int getThetaIdx(ParameterConfiguration configuration);

	/**
	 * Returns the number of unique problem instance seed pairs run for this configuration 
	 * (i.e. the number of runs for a configuration, but counting capped runs all as one)
	 * 
	 * @param config
	 * @return number of pisps that a config has run on
	 */
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration( ParameterConfiguration config);

	/**
	 * Returns an unmodifiable map that stores the seeds and performance for a configuration
	 * @param configuration
	 * @return map
	 */
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(ParameterConfiguration configuration);

	/**
	 * Returns a list of seeds used by instance 
	 * @param pi problem instance
	 * @return list
	 */
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi);


	/**
	 * Returns the Index into arrays represented by this configuration, if it doesn't already exist it is created
	 * 
	 * @param configuration 	Configuration to create idx for
	 * @return index into the theta array for this configuration
	 */
	public int getOrCreateThetaIdx(ParameterConfiguration config);


	/**
	 * Returns the AlgorithmRunResult for an AlgorithmRunConfiguration if available
	 * 
	 * @param runConfig 	AlgorithmRunConfiguration object
	 * @return AlgorithmRunResult or null if no match
	 */
	public AlgorithmRunResult getAlgorithmRunResultForAlgorithmRunConfiguration(AlgorithmRunConfiguration runConfig);

	
	
	
}
