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
 * Abstract class that delegates all methods to the supplied runhistory object.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public abstract class AbstractRunHistoryDecorator implements RunHistory {

	
	protected final RunHistory rh;
	
	public AbstractRunHistoryDecorator(RunHistory rh)
	{
		this.rh = rh;
	}

	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		rh.append(run);
	}

	@Override
	public RunObjective getRunObjective() {
		return rh.getRunObjective();
	}

	@Override
	public OverallObjective getOverallObjective() {
		return rh.getOverallObjective();
	}

	@Override
	public void incrementIteration() {
		rh.incrementIteration();
	}

	@Override
	public int getIteration() {
		return rh.getIteration();
	}

	@Override
	public Set<ProblemInstance> getProblemInstancesRan(ParameterConfiguration config) {
		return rh.getProblemInstancesRan(config);
	}

	@Override
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(
			ParameterConfiguration config) {
		return rh.getProblemInstanceSeedPairsRan(config);
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				hallucinatedValues);
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				hallucinatedValues, minimumResponseValue);
	}

	
	@Override
	public double getTotalRunCost() {
		return rh.getTotalRunCost();
	}
	
	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		return rh.getUniqueInstancesRan();
	}

	@Override
	public Set<ParameterConfiguration> getUniqueParamConfigurations() {
		return rh.getUniqueParamConfigurations();
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndexExcludingRedundant() {
		return rh.getParameterConfigurationInstancesRanByIndexExcludingRedundant();
	}

	@Override
	public List<ParameterConfiguration> getAllParameterConfigurationsRan() {
		return rh.getAllParameterConfigurationsRan();
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		return rh.getAllConfigurationsRanInValueArrayForm();
	}

	

	@Override
	public List<RunData> getAlgorithmRunDataExcludingRedundant() {
		return rh.getAlgorithmRunDataExcludingRedundant();
	}

	@Override
	public List<RunData> getAlgorithmRunDataIncludingRedundant() {
		return rh.getAlgorithmRunDataIncludingRedundant();
	}

	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant() {
		return rh.getAlgorithmRunsExcludingRedundant();
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant(ParameterConfiguration config) {
		return rh.getAlgorithmRunsExcludingRedundant(config);
	}

	@Override
	public int getTotalNumRunsOfConfigExcludingRedundant(ParameterConfiguration config) {
		return rh.getTotalNumRunsOfConfigExcludingRedundant(config);
	}

	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant() {
		return rh.getAlgorithmRunsIncludingRedundant();
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant(ParameterConfiguration config) {
		return rh.getAlgorithmRunsIncludingRedundant(config);
	}

	@Override
	public int getTotalNumRunsOfConfigIncludingRedundant(ParameterConfiguration config) {
		return rh.getTotalNumRunsOfConfigIncludingRedundant(config);
	}
	
	@Override
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(
			ParameterConfiguration config) {
		return rh.getEarlyCensoredProblemInstanceSeedPairs(config);
	}

	@Override
	public int getThetaIdx(ParameterConfiguration configuration) {
		return rh.getThetaIdx(configuration);
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			double minimumResponseValue) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				minimumResponseValue);
	}

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(
			ParameterConfiguration config) {
		return rh
				.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
	}

	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(
			ParameterConfiguration configuration) {
		return rh.getPerformanceForConfig(configuration);
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) {
		return rh.getSeedsUsedByInstance(pi);
	}
	
	@Override
	public double getEmpiricalCostLowerBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return rh.getEmpiricalCostLowerBound(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCostUpperBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return rh.getEmpiricalCostUpperBound(config, instanceSet, cutoffTime);
	}
	

	@Override
	public AlgorithmRunResult getAlgorithmRunResultForAlgorithmRunConfiguration(AlgorithmRunConfiguration runConfig)
	{
		return rh.getAlgorithmRunResultForAlgorithmRunConfiguration(runConfig);
	}
	
}
