package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.Collection;
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

public class ThreadSafeRunHistoryWrapper implements ThreadSafeRunHistory {

	private final RunHistory runHistory;
	
	ReadWriteLockThreadTracker rwltt = new ReadWriteLockThreadTracker();
	
	
	
	public ThreadSafeRunHistoryWrapper(RunHistory runHistory)
	{
		this.runHistory = runHistory;
	}
	
	@Override
	public void append(Collection<AlgorithmRunResult> runs)
			throws DuplicateRunException {

	
		lockWrite();
		
		try {
			for(AlgorithmRunResult run : runs)
			{
				//log.debug("Atomically appending run {} " + run.getRunConfig());
				runHistory.append(run);
			}
			
		} finally
		{
			unlockWrite();
		}
		
	}
	
	
	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		
		
		lockWrite();
		try {
			//log.debug("Appending single run {} " + run.getRunConfig());
			runHistory.append(run);
		} finally
		{
			unlockWrite();
		}
		
		
	}

	@Override
	public RunObjective getRunObjective() {
		lockRead();
		
		try {
			return runHistory.getRunObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public OverallObjective getOverallObjective() {
		lockRead();
		try {
			return runHistory.getOverallObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public void incrementIteration() {
		lockWrite();
		
		try {
			 runHistory.incrementIteration();
		} finally
		{
			unlockWrite();
		}
	}

	@Override
	public int getIteration() {

		lockRead();
		try {
			return runHistory.getIteration();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstance> getProblemInstancesRan(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getProblemInstancesRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(
			ParameterConfiguration config) {
		
		lockRead();
		try {
			return runHistory.getProblemInstanceSeedPairsRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, minimumResponseValue);
		} finally
		{
			unlockRead();
		}
	}




	@Override
	public double getTotalRunCost() {
		lockRead();
		try {
			return runHistory.getTotalRunCost();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		lockRead();
		try {
			return runHistory.getUniqueInstancesRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ParameterConfiguration> getUniqueParamConfigurations() {
		lockRead();
		try {
			return runHistory.getUniqueParamConfigurations();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndexExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getParameterConfigurationInstancesRanByIndexExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}


	
	@Override
	public List<ParameterConfiguration> getAllParameterConfigurationsRan() {
		lockRead();
		try {
			return runHistory.getAllParameterConfigurationsRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		lockRead();
		try {
			return runHistory.getAllConfigurationsRanInValueArrayForm();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public List<RunData> getAlgorithmRunDataIncludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunDataIncludingRedundant();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public List<RunData> getAlgorithmRunDataExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunDataExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}

	

	@Override
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(
			ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getEarlyCensoredProblemInstanceSeedPairs(config);
		} finally
		{
			unlockRead();
	
		}
	}



	@Override
	public int getThetaIdx(ParameterConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getThetaIdx(configuration);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(
			ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
		} finally
		{
			unlockRead();
	
		}
	}


	@Override
	public void readLock() {
		lockRead();
	}


	@Override
	public void releaseReadLock() {
		unlockRead();
		
	}

	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigExcludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigExcludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigIncludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigIncludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	

	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(
			ParameterConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getPerformanceForConfig(configuration);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) {
		lockRead();
		try {
			return runHistory.getSeedsUsedByInstance(pi);
		} finally
		{
			unlockRead();
		}
	}
	
	
	public void lockRead()
	{
	
		if(runHistory instanceof ThreadSafeRunHistory)
		{
			((ThreadSafeRunHistory) runHistory).readLock();
		}
		
		this.rwltt.lockRead();
	
	}
	
	private void unlockRead()
	{
		if(runHistory instanceof ThreadSafeRunHistory)
		{
			((ThreadSafeRunHistory) runHistory).releaseReadLock();
		}
		
		this.rwltt.unlockRead();
	}
	
	private void lockWrite()
	{
		this.rwltt.lockWrite();
	}
	
	private void unlockWrite()
	{
		this.rwltt.unlockWrite();
		
	}

	@Override
	public int getOrCreateThetaIdx(ParameterConfiguration config) {
		lockWrite();
		try {
			return this.runHistory.getOrCreateThetaIdx(config);
		} finally
		{
			unlockWrite();
		}
	
		
	}

	@Override
	public double getEmpiricalCostLowerBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostLowerBound(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCostUpperBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostUpperBound(config, instanceSet, cutoffTime);
	}

	@Override
	public AlgorithmRunResult getAlgorithmRunResultForAlgorithmRunConfiguration(
			AlgorithmRunConfiguration runConfig) {
		lockRead();
		try {
			return this.runHistory.getAlgorithmRunResultForAlgorithmRunConfiguration(runConfig);
		} finally
		{
			unlockRead();
		}
	}


	

	

}
