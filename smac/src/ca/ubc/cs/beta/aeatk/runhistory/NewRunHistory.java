package ca.ubc.cs.beta.aeatk.runhistory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;

/**
 * Stores a list of runs that have been completed in multiple ways
 * that make it easier to query for certain kinds of information / comparsions.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@SuppressWarnings("unused")
@NotThreadSafe
public class NewRunHistory implements RunHistory {


	/**
	 * Objective function that allows us to aggregate various instance / seed pairs to 
	 * give us a value for the instance
	 */
	private final OverallObjective perInstanceObjectiveFunction;
	
	/**
	 * Objective function that allows us to aggeregate various instances (aggregated by the perInstanceObjectiveFunction), 
	 * to determine a cost for the set of instances.
	 */
	private final OverallObjective aggregateInstanceObjectiveFunction;
	
	/**
	 * Objective function that determines the response value from a run.
	 */
	private final RunObjective runObj;
	
	/**
	 * Current iteration we are on
	 */
	private int iteration = 0;

	/**
	 * Stores a list of Parameter Configurations along with there associted thetaIdx
	 */
	private final KeyObjectManager<ParameterConfiguration> paramConfigurationList = new KeyObjectManager<ParameterConfiguration>();
	
	/**
	 * Stores a list of RunData
	 */
	//private final List<RunData> runHistoryList = new ArrayList<RunData>();
	private final List<RunData> runHistoryListIncludingRedundant = new ArrayList<RunData>();
	
	private final List<RunData> runHistoryListExcludingRedundant = new ArrayList<RunData>();
	
	
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Stores the sum of all the runtimes
	 */
	private double totalRuntimeSum = 0;
	
	/**
	 * Stores for each configuration a mapping of instances to a map of seeds => response values
	 * 
	 * We store the Seeds in a LinkedHashMap because the order of them matters as far as determining which seed to pick to run
	 * as far as Matlab synchronizing goes. Otherwise it could be a regular map
	 */
	private final Map<ParameterConfiguration, Map<ProblemInstance, LinkedHashMap<Long, Double>>> configToPerformanceMap =
			new HashMap<ParameterConfiguration, Map<ProblemInstance, LinkedHashMap<Long, Double>>>();
	
	/**
	 * Stores for each instance the list of seeds used 
	 */
	private final HashMap<ProblemInstance, List<Long>> seedsUsedByInstance = new HashMap<ProblemInstance, List<Long>>();
	
	/**
	 * Stores the number of times a config has been run
	 */
	private final LinkedHashMap<ParameterConfiguration, Integer> configToNumRunsMap = new LinkedHashMap<ParameterConfiguration, Integer>();
	
	/**
	 * Stores the number of times a config has been run
	 */
	private final LinkedHashMap<ParameterConfiguration, Integer> configToNumRunsIgnoringRedundantMap = new LinkedHashMap<ParameterConfiguration, Integer>();
	
	/**
	 * Stores a list of Instance Seed Pairs whose runs were capped.
	 */
	private final HashMap<ParameterConfiguration, Set<ProblemInstanceSeedPair>> censoredEarlyRuns = new HashMap<ParameterConfiguration, Set<ProblemInstanceSeedPair>>(); 
	
	/**
	 * Stores the set of instances we have run
	 */
	private Set<ProblemInstance> instancesRanSet = new HashSet<ProblemInstance>();
	
	private final HashMap<ParameterConfiguration, List<AlgorithmRunResult>> configToRunMap = new HashMap<ParameterConfiguration, List<AlgorithmRunResult>>();
	
	/**
	 * Stores for each run the best known value at a given time. We use a linked hash map because order is important.
	 */
	private final LinkedHashMap<ParameterConfiguration, LinkedHashMap<ProblemInstanceSeedPair,AlgorithmRunResult>> configToRunIgnoreRedundantMap = new LinkedHashMap<ParameterConfiguration, LinkedHashMap<ProblemInstanceSeedPair,AlgorithmRunResult>>();
	
	

	/**
	 * Stores the best known value for each run at any given time. We want to maintain insertion order for this.
	 */
	private final List<AlgorithmRunResult> runsInAuthorativeOrderExcludingRedundant = new ArrayList<AlgorithmRunResult>();
	
	/**
	 * Stores for every run configuration the index of the location 
	 */
	private final Map<ParamConfigurationProblemInstanceSeedPair, Integer> runIndex = new HashMap<ParamConfigurationProblemInstanceSeedPair, Integer>();
	
	
	private static final DecimalFormat format = new DecimalFormat("#######.####");
	
	
	private final Map<AlgorithmRunConfiguration, AlgorithmRunResult> algorithmRunConfigurationResultMap = new HashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
	
	
	public NewRunHistory()
	{
		this(OverallObjective.MEAN, OverallObjective.MEAN10, RunObjective.RUNTIME);
	}
	/**
	 * Creates NewRunHistory object
	 * @param intraInstanceObjective	intraInstanceObjective to use when calculating costs
	 * @param interInstanceObjective	interInstanceObjective to use when calculating costs
	 * @param runObj					run objective to use 
	 */
	public NewRunHistory( OverallObjective intraInstanceObjective,  OverallObjective interInstanceObjective, RunObjective runObj)
	{
		
		if(intraInstanceObjective == null)
		{
			throw new IllegalArgumentException("You must supply an intra instance objective");
		}
		
		if(interInstanceObjective == null)
		{
			throw new IllegalArgumentException("You must supply an interInstanceObjective");
			
		}
		
		if(runObj == null)
		{
			throw new IllegalArgumentException("You must supply a run objective");
		}
		this.perInstanceObjectiveFunction = intraInstanceObjective;
		this.aggregateInstanceObjectiveFunction = interInstanceObjective;
		this.runObj = runObj;
	
	}
	
	
	private volatile AlgorithmExecutionConfiguration firstExecConfig;
	
	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException{

		
		
		if(firstExecConfig == null)
		{
			this.firstExecConfig = run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration();
		} else
		{
			if(!this.firstExecConfig.equals(run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration()))
			{
				throw new IllegalArgumentException("RunHistory object cannot store runs for different exec configs first was: " + firstExecConfig + " current run was : " + run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration());
			}
		}
		
		if(run.getRunStatus().equals(RunStatus.RUNNING))
		{
			throw new IllegalArgumentException("Runs with Run Result RUNNING cannot be saved to a RunHistory object");
		}
		ParameterConfiguration config = run.getAlgorithmRunConfiguration().getParameterConfiguration();
		ProblemInstanceSeedPair pisp = run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair();
		ProblemInstance pi = pisp.getProblemInstance();
		long seed = run.getResultSeed();
		
		Double runResult = runObj.getObjective(run);
		
		/**
		 * Add run data to the list of seeds used by Instance
		 */
		List<Long> instanceSeedList = seedsUsedByInstance.get(pi);
		if(instanceSeedList == null)
		{ //Initialize List if non existant
			instanceSeedList = new LinkedList<Long>();
			seedsUsedByInstance.put(pi,instanceSeedList);
		}
		instanceSeedList.add(seed);

		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceToPerformanceMap = configToPerformanceMap.get(config);
		if(instanceToPerformanceMap == null)
		{ //Initialize Map if non-existant
			instanceToPerformanceMap = new HashMap<ProblemInstance, LinkedHashMap<Long,Double>>();
			configToPerformanceMap.put(config,instanceToPerformanceMap);
		}
		
		LinkedHashMap<Long, Double> seedToPerformanceMap = instanceToPerformanceMap.get(pi);
		if(seedToPerformanceMap == null)
		{ //Initialize Map if non-existant
			seedToPerformanceMap = new LinkedHashMap<Long, Double>();
			instanceToPerformanceMap.put(pi, seedToPerformanceMap);
		}
		
		Double dOldValue = seedToPerformanceMap.put(seed,runResult);
		
		RunStatus result = run.getRunStatus();
		
		boolean censoredEarly = run.isCensoredEarly();
		
		
		if(configToRunIgnoreRedundantMap.get(config) == null)
		{
			configToRunIgnoreRedundantMap.put(config, new LinkedHashMap<ProblemInstanceSeedPair, AlgorithmRunResult>());
		}
		
		
		
		if(dOldValue != null)
		{
			//If the value already existed then either
			//we have a duplicate run OR the previous run was capped
			
			Set<ProblemInstanceSeedPair> censoredEarlyRunsForConfig = censoredEarlyRuns.get(config);

			if((censoredEarlyRunsForConfig != null) && censoredEarlyRunsForConfig.contains(pisp))
			{
				
				if(this.runObj != RunObjective.RUNTIME)
				{
					//Duplicate check before we tamper with the data structure
					log.error("Not sure how to rectify early censored runs under different run objectives, current run seems to conflict with a previous one: {} " , run);
					throw new IllegalStateException("Unable to handle capped runs for the RunObjective: " + runObj);
				}
				
				//We remove it now and will re-add it if this current run was capped
				censoredEarlyRunsForConfig.remove(pisp); 
			} else
			{
				AlgorithmRunResult matchingRun = null;
				for(AlgorithmRunResult algoRun : this.getAlgorithmRunsExcludingRedundant(config))
				{
					if(algoRun.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().equals(run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair()))
					{
						matchingRun = algoRun;
					}
				}
							
				//Restores the state of the RunHistory object to essentially idential to when we found it.
				seedToPerformanceMap.put(seed, dOldValue);
				
				Object[] args = {matchingRun, run, config, pi,dOldValue};				
				
				//log.error("RunHistory already contains a run with identical config, instance and seed \n Original Run:{}\nRun:{}\nConfig:{}\nInstance:{}\nPrevious Performance:{}", args);
				throw new DuplicateRunException("Duplicate Run Detected", run);
			}
			
			
			if(this.runObj != RunObjective.RUNTIME)
			{
				log.error("Not sure how to rectify early censored runs under different run objectives, current run seems to conflict with a previous one: {} " , run);
				throw new IllegalStateException("Unable to handle capped runs for the RunObjective: " + runObj);
			} else
			{
				//We know that both the previous and current result must be censored early, so we take the maximimum
				if(censoredEarly)
				{	
					
					seedToPerformanceMap.put(seed, Math.max(dOldValue, runResult));
					
					if(dOldValue < runResult)
					{
						//this.configToRunIgnoreRedundantMap.get(config).put(pisp, run);
						
						
						AlgorithmRunResult run2 = this.configToRunIgnoreRedundantMap.get(config).get(pisp);
						this.configToRunIgnoreRedundantMap.get(config).put(pisp, run);
						
						ParamConfigurationProblemInstanceSeedPair pcpisp = new ParamConfigurationProblemInstanceSeedPair(pisp,config);
						
						if(this.runIndex.get(pcpisp) == null)
						{
							throw new IllegalStateException("This run should exist somewhere else in our list");
						} 
						
						int index = this.runIndex.get(pcpisp);
						
						this.runsInAuthorativeOrderExcludingRedundant.set(index, run);
						
					}
				}
			}

		} else
		{ 
			//Haven't seen this run before, so we have new data
			if(configToNumRunsIgnoringRedundantMap.get(config) == null)
			{
				configToNumRunsIgnoringRedundantMap.put(config, Integer.valueOf(1));
			} else
			{
				configToNumRunsIgnoringRedundantMap.put(config, configToNumRunsIgnoringRedundantMap.get(config) +1);
			}
			
			this.configToRunIgnoreRedundantMap.get(config).put(pisp, run);
			
			ParamConfigurationProblemInstanceSeedPair pcpisp = new ParamConfigurationProblemInstanceSeedPair(pisp,config);
			this.runsInAuthorativeOrderExcludingRedundant.add(run);
			
			this.runIndex.put(pcpisp,this.runsInAuthorativeOrderExcludingRedundant.size()-1);
			
			
		}
		
	
		
		if(this.configToRunMap.get(config) == null)
		{
			this.configToRunMap.put(config, new ArrayList<AlgorithmRunResult>());
		}
		
		this.configToRunMap.get(config).add(run);
		totalRuntimeSum += Math.max(0.1, run.getRuntime());
		
		/*
		 * Add data to the run List
		 */
		int thetaIdx = paramConfigurationList.getOrCreateKey(config);
		
	
		
		int instanceIdx = pi.getInstanceID();
		
	
		RunData rd = new RunData(iteration, thetaIdx, instanceIdx, run,runResult, censoredEarly);
		//runHistoryList.add(rd);
		
		runHistoryListIncludingRedundant.add(rd);
		

		if(dOldValue != null)
		{
			if(dOldValue < runResult)
			{
				//Previous value
				
				ParamConfigurationProblemInstanceSeedPair pcpisp = new ParamConfigurationProblemInstanceSeedPair(pisp,config);
				
				if(this.runIndex.get(pcpisp) == null)
				{
					throw new IllegalStateException("This run should exist somewhere else in our list");
				} 
				
				int index = this.runIndex.get(pcpisp);
				
				this.runHistoryListExcludingRedundant.set(index, rd);
			}
			
		} else
		{
			runHistoryListExcludingRedundant.add(rd);
		}
		
		
		
		
		/*
		 * Increment the config run counter
		 */
		if(configToNumRunsMap.get(config) == null)
		{
			configToNumRunsMap.put(config, Integer.valueOf(1));
		} else
		{
			configToNumRunsMap.put(config, configToNumRunsMap.get(config) +1);
		}
		
		/*
		 * Add Instance to the set of instances ran 
		 */
		instancesRanSet.add(pi);
		
		/*
		 * Add to the capped runs set
		 */
		if(censoredEarly)
		{
			if(!censoredEarlyRuns.containsKey(config))
			{
				censoredEarlyRuns.put(config, new LinkedHashSet<ProblemInstanceSeedPair>());
			}
				
			censoredEarlyRuns.get(config).add(pisp);
		}
		
		
		this.algorithmRunConfigurationResultMap.put(run.getAlgorithmRunConfiguration(), run);	
		
	}

	
	
	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime)
	{
		Map<ProblemInstance, Map<Long, Double>> foo = Collections.emptyMap();
		return getEmpiricalCost(config, instanceSet, cutoffTime, foo);
	}
	
	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime, double minimumResponseValue)
	{
		Map<ProblemInstance, Map<Long, Double>> foo = Collections.emptyMap();
		return getEmpiricalCost(config, instanceSet, cutoffTime, foo, minimumResponseValue);
	}
	
	
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime, Map<ProblemInstance, Map<Long,Double>> hallucinatedValues)
	{
		return getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, Double.NEGATIVE_INFINITY);
	}
	
	@Override
	public double getEmpiricalCostLowerBound(ParameterConfiguration config,	Set<ProblemInstance> instanceSet, double cutoffTime) 
	{
		
		return getEmpiricalCostBound(config, instanceSet, cutoffTime, 0.0, Bound.LOWER);
		
	}

	@Override
	public double getEmpiricalCostUpperBound(ParameterConfiguration config,	Set<ProblemInstance> instanceSet, double cutoffTime) 
	{	
		return getEmpiricalCostBound(config, instanceSet, cutoffTime, cutoffTime, Bound.UPPER);

	}
	
	private enum Bound{
		UPPER,
		LOWER
	}
	
	private double getEmpiricalCostBound(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime, Double boundValue, Bound b)
	{
		Map<ProblemInstance, Map <Long,Double>> hallucinatedValues = new HashMap<ProblemInstance,Map<Long, Double>>();
		
		Set<ProblemInstanceSeedPair> earlyCensoredPISPs = this.getEarlyCensoredProblemInstanceSeedPairs(config);
		
		for(ProblemInstance pi : instanceSet)
		{
			Map<Long,Double> instPerformance =  new HashMap<Long, Double>(); 
			hallucinatedValues.put(pi, instPerformance);
			
			//Pass one is to put the bound value in every necessary slot.
			
			if(seedsUsedByInstance.get(pi) == null)
			{
				seedsUsedByInstance.put(pi, new ArrayList<Long>());
			}
			for(Long l : seedsUsedByInstance.get(pi))
			{
				instPerformance.put(l, boundValue);
			}

			//Pass two puts the observed performance in the appropriate slot.
			Map<Long, Double> actualPerformance = new HashMap<Long, Double>();

			
			if(this.configToPerformanceMap.get(config) != null)
			{
				instPerformance.putAll(this.configToPerformanceMap.get(config).get(pi));
			}
			
			//Pass three rounds early censored values back up to the cuttoff time
			if(b == Bound.UPPER)
			{
				for(Long l : seedsUsedByInstance.get(pi))
				{
					ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, l);
					if(earlyCensoredPISPs.contains(pisp))
					{
						instPerformance.put(l, boundValue);
					}
				}
				
				
			} 
			
			
			if(instPerformance.size() == 0)
			{
				//== We insert a bound value  if we have nothing, because of the way getEmipricalCost is implemented.
				instPerformance.put(Long.MIN_VALUE, boundValue);
			}
			 
			
			
		}
		
		return getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, 0);

	}
	@Override
	public double getEmpiricalCost(ParameterConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime, Map<ProblemInstance, Map<Long,Double>> hallucinatedValues, double minimumResponseValue)
	{
		if (!configToPerformanceMap.containsKey(config) && hallucinatedValues.isEmpty()){
			return Double.MAX_VALUE;
		}
		ArrayList<Double> instanceCosts = new ArrayList<Double>();
		
		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = configToPerformanceMap.get(config);

		if(instanceSeedToPerformanceMap == null) 
		{
			instanceSeedToPerformanceMap = new HashMap<ProblemInstance, LinkedHashMap<Long, Double>>();
			
		}
		/*
		 * Compute the Instances to use in the cost calculation
		 * It's everything we ran out of everything we requested.
		 */
		Set<ProblemInstance> instancesToUse = new HashSet<ProblemInstance>();
		instancesToUse.addAll(instanceSet);
		
		Set<ProblemInstance> instancesToKeep = new HashSet<ProblemInstance>(instanceSeedToPerformanceMap.keySet());
		instancesToKeep.addAll(hallucinatedValues.keySet());
		instancesToUse.retainAll(instancesToKeep);
		
		
		for(ProblemInstance pi : instancesToUse)
		{
			
			Map<Long, Double> seedToPerformanceMap = new HashMap<Long, Double>();
			if(instanceSeedToPerformanceMap.get(pi) != null) seedToPerformanceMap.putAll(instanceSeedToPerformanceMap.get(pi));
			if(hallucinatedValues.get(pi) != null) seedToPerformanceMap.putAll(hallucinatedValues.get(pi));
			
			/*
			 * Aggregate the cost over the instances
			 */
			ArrayList<Double> localCosts = new ArrayList<Double>();
			for(Map.Entry<Long, Double> ent : seedToPerformanceMap.entrySet())
			{
					localCosts.add( Math.max(minimumResponseValue, ent.getValue()) );	
			}
			instanceCosts.add( perInstanceObjectiveFunction.aggregate(localCosts,cutoffTime)); 
		}
		return aggregateInstanceObjectiveFunction.aggregate(instanceCosts,cutoffTime);
	}

	@Override
	public RunObjective getRunObjective() {
		return runObj;
	}

	@Override
	public OverallObjective getOverallObjective() {
	
		return perInstanceObjectiveFunction;
	}

	@Override
	public void incrementIteration() {
		iteration++;

	}

	@Override
	public int getIteration() {
		return iteration;
	}

	@Override
	public Set<ProblemInstance> getProblemInstancesRan(ParameterConfiguration config) {
		if (!configToPerformanceMap.containsKey(config)){
			return new HashSet<ProblemInstance>();
		}
		return new HashSet<ProblemInstance>( configToPerformanceMap.get(config).keySet() );
	}

	@Override
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(ParameterConfiguration config) {
		if (!configToPerformanceMap.containsKey(config)){
			return new HashSet<ProblemInstanceSeedPair>();
		}
		Set<ProblemInstanceSeedPair> pispSet = new HashSet<ProblemInstanceSeedPair>();		
		Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = configToPerformanceMap.get(config);
		
		for (Entry<ProblemInstance, LinkedHashMap<Long, Double>> kv : instanceSeedToPerformanceMap.entrySet()) {
			ProblemInstance pi =  kv.getKey();
			Map<Long, Double> hConfigInst = kv.getValue();
			for (Long seed: hConfigInst.keySet()) {
				pispSet.add( new ProblemInstanceSeedPair(pi, seed) );
			}
		}
		return pispSet;
	}

	@Override
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(ParameterConfiguration config)
	{
		if(!censoredEarlyRuns.containsKey(config))
		{
			return Collections.emptySet();
		}
		
		return Collections.unmodifiableSet(censoredEarlyRuns.get(config));
	}

	

	@Override
	public double getTotalRunCost() {
		return totalRuntimeSum;
	}

	

	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		return Collections.unmodifiableSet(instancesRanSet);
	}

	@Override
	public Set<ParameterConfiguration> getUniqueParamConfigurations() {
		return Collections.unmodifiableSet(configToNumRunsMap.keySet());
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndexExcludingRedundant() {
		int[][] result = new int[runHistoryListExcludingRedundant.size()][2];
		
		int i=0; 
		for(RunData runData : runHistoryListExcludingRedundant)
		{
			result[i][0] = runData.getThetaIdx();
			result[i][1] = runData.getInstanceidx();
			i++;
		}
		
		return result;
	}

	@Override
	public List<ParameterConfiguration> getAllParameterConfigurationsRan() {
		List<ParameterConfiguration> runs = new ArrayList<ParameterConfiguration>(paramConfigurationList.size());
		
		for(int i=1; i <= paramConfigurationList.size(); i++)
		{
			runs.add(paramConfigurationList.getValue(i));
		}
		return runs;
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		double[][] configs = new double[paramConfigurationList.size()][];
		for(int i=1; i <= paramConfigurationList.size(); i++)
		{
			configs[i-1] = paramConfigurationList.getValue(i).toValueArray();
		}
	
		return configs;
	}

	
	

	@Override
	public List<RunData> getAlgorithmRunDataExcludingRedundant() {
		return Collections.unmodifiableList(runHistoryListExcludingRedundant);
	}

	@Override
	public List<RunData> getAlgorithmRunDataIncludingRedundant() {
		return Collections.unmodifiableList(runHistoryListIncludingRedundant);
	}
	

	@Override
	public int getThetaIdx(ParameterConfiguration config) {
		Integer thetaIdx = paramConfigurationList.getKey(config);
		if(thetaIdx == null)
		{
			return -1;
		} else
		{
			return thetaIdx;
		}
		
	}
	
	@Override
	public int getOrCreateThetaIdx(ParameterConfiguration config) {
		 return paramConfigurationList.getOrCreateKey(config);
		
	}
	

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(ParameterConfiguration config)
	{
		 Map<ProblemInstance, LinkedHashMap<Long, Double>> runs = configToPerformanceMap.get(config);
		 
		 int total =0;
		 for(Entry<ProblemInstance, LinkedHashMap<Long, Double>> ent : runs.entrySet())
		 {
			 total+=ent.getValue().size();
		 }
		 
		 return total;
		
	}
	
	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(ParameterConfiguration config)
	{
		Map<ProblemInstance, LinkedHashMap<Long,Double>> map =  configToPerformanceMap.get(config);
		if(map != null)
		{
			return Collections.unmodifiableMap(map);
		} else
		{
			return Collections.emptyMap();
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigIncludingRedundant(ParameterConfiguration config) {
		Integer value = configToNumRunsMap.get(config);
		if( value != null)
		{
			return value;
		} else
		{
			return 0;
		}
	}
	
	@Override
	public int getTotalNumRunsOfConfigExcludingRedundant(ParameterConfiguration config) {
		Integer value = configToNumRunsIgnoringRedundantMap.get(config);
		if( value != null)
		{
			return value;
		} else
		{
			return 0;
		}
		
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant() {
		
		return new ArrayList<>(this.runsInAuthorativeOrderExcludingRedundant);
	}
	
	@Override
	/**
	 * Get a list of algorithm runs we have used
	 * 
	 * Slow O(n) method to generate a list of Algorithm Runs
	 * We could speed this up but at this point we only do this for restoring state
	 * @return list of algorithm runs we have recieved
	 */
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant() 
	{
		List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>(this.runHistoryListIncludingRedundant.size());
		for(RunData runData : getAlgorithmRunDataIncludingRedundant() )
		{
			runs.add(runData.getRun());
		}
		return runs;
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant(ParameterConfiguration config) {
		
		List<AlgorithmRunResult> runs = this.configToRunMap.get(config);
		
		if(runs != null)
		{
			return Collections.unmodifiableList(runs);
		} else
		{
			return Collections.emptyList();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant(ParameterConfiguration config)
	{
		Map<ProblemInstanceSeedPair, AlgorithmRunResult> runs = this.configToRunIgnoreRedundantMap.get(config);
		
		if(runs == null)
		{
			runs = Collections.emptyMap();
		}
		
		return Collections.unmodifiableList(new ArrayList<AlgorithmRunResult>(runs.values()));
	}
	

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) 
	{

		if(seedsUsedByInstance.get(pi) == null)
		{
			seedsUsedByInstance.put(pi, new ArrayList<Long>());
		}
		return Collections.unmodifiableList(seedsUsedByInstance.get(pi));
	}

	


	private static class ParamConfigurationProblemInstanceSeedPair
	{
		private final ProblemInstanceSeedPair pisp;
		private final ParameterConfiguration config;
		
		public ParamConfigurationProblemInstanceSeedPair(ProblemInstanceSeedPair pisp, ParameterConfiguration config)
		{
			this.pisp = pisp;
			this.config = config;
		}

		public ProblemInstanceSeedPair getProblemInstanceSeedPair() {
			return pisp;
		}

		public ParameterConfiguration getConfig() {
			return config;
		}

		@Override
		public int hashCode() {
			final int prime = 37;
			int result = 1;
			result = prime * result
					+ ((config == null) ? 0 : config.hashCode());
			result = prime * result + ((pisp == null) ? 0 : pisp.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ParamConfigurationProblemInstanceSeedPair))
				return false;
			ParamConfigurationProblemInstanceSeedPair other = (ParamConfigurationProblemInstanceSeedPair) obj;
			if (config == null) {
				if (other.config != null)
					return false;
			} else if (!config.equals(other.config))
				return false;
			if (pisp == null) {
				if (other.pisp != null)
					return false;
			} else if (!pisp.equals(other.pisp))
				return false;
			return true;
		}
	}
	
	@Override
	public AlgorithmRunResult getAlgorithmRunResultForAlgorithmRunConfiguration(AlgorithmRunConfiguration runConfig) {
		return this.algorithmRunConfigurationResultMap.get(runConfig);
	}

}
