package ca.ubc.cs.beta.aeatk.example.statemerge;



import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.model.ModelBuildingOptions;
import ca.ubc.cs.beta.aeatk.model.builder.AdaptiveCappingModelBuilder;
import ca.ubc.cs.beta.aeatk.model.builder.BasicModelBuilder;
import ca.ubc.cs.beta.aeatk.model.builder.ModelBuilder;
import ca.ubc.cs.beta.aeatk.model.data.DefaultValueForConditionalsMDS;
import ca.ubc.cs.beta.aeatk.model.data.MaskCensoredDataAsUncensored;
import ca.ubc.cs.beta.aeatk.model.data.MaskInactiveConditionalParametersWithDefaults;
import ca.ubc.cs.beta.aeatk.model.data.SanitizedModelData;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistoryHelper;
import ca.ubc.cs.beta.models.fastrf.RandomForest;

/**
 * Awful code that was ripped from SMAC that builds a model
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class StateMergeModelBuilder {

	
	/**
	 * Most recent forest built
	 */
	private RandomForest forest;
	
	/**
	 * Most recent prepared forest built (may be NULL but always corresponds to the last forest built)
	 */
	private RandomForest preparedForest;
	
	/**
	 * Last build of sanitized data
	 */
	private SanitizedModelData sanitizedData;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void learnModel(List<ProblemInstance> instances, RunHistory runHistory, ParameterConfigurationSpace configSpace, RandomForestOptions rfOptions, ModelBuildingOptions mbOptions,double cutoffTime, OverallObjective intraInstanceObjective, boolean adaptiveCapping, SeedableRandomPool pool) 
	{

		//=== The following two sets are required to be sorted by instance and paramConfig ID.
		Set<ProblemInstance> all_instances = new LinkedHashSet<ProblemInstance>(instances);
		Set<ParameterConfiguration> paramConfigs = runHistory.getUniqueParamConfigurations();
		
		Set<ProblemInstance> runInstances=runHistory.getUniqueInstancesRan();
		ArrayList<Integer> runInstancesIdx = new ArrayList<Integer>(all_instances.size());
		
		//=== Get the instance feature matrix (X).
		int i=0; 
		double[][] instanceFeatureMatrix = new double[all_instances.size()][];
		for(ProblemInstance pi : all_instances)
		{
			if(runInstances.contains(pi))
			{
				runInstancesIdx.add(i);
			}
			instanceFeatureMatrix[i] = pi.getFeaturesDouble();
			i++;
		}

		//=== Get the parameter configuration matrix (Theta).
		double[][] thetaMatrix = new double[paramConfigs.size()][];
		i = 0;
		for(ParameterConfiguration pc : paramConfigs)
		{
			if(mbOptions.maskInactiveConditionalParametersAsDefaultValue)
			{
				thetaMatrix[i++] = pc.toComparisonValueArray();
			} else
			{
				thetaMatrix[i++] = pc.toValueArray();
			}
		}

		//=== Get an array of the order in which instances were used (TODO: same for Theta, from ModelBuilder) 
		int[] usedInstanceIdxs = new int[runInstancesIdx.size()]; 
		for(int j=0; j <  runInstancesIdx.size(); j++)
		{
			usedInstanceIdxs[j] = runInstancesIdx.get(j);
		}
		
		
		
		List<AlgorithmRunResult> runs = runHistory.getAlgorithmRunsExcludingRedundant();
		
		double[] runResponseValues = RunHistoryHelper.getRunResponseValues(runs, runHistory.getRunObjective());
		
		boolean[] censored = RunHistoryHelper.getCensoredEarlyFlagForRuns(runs);
		
		
		if(mbOptions.maskCensoredDataAsKappaMax)
		{
			for(int j=0; j < runResponseValues.length; j++)
			{
				if(censored[j])
				{
					runResponseValues[j] = cutoffTime;
				}
			}
		}
		
		
		
		
		for(int j=0; j < runResponseValues.length; j++)
		{ //=== Not sure if I Should be penalizing runs prior to the model
			// but matlab sure does
			if(runResponseValues[j] >= cutoffTime)
			{	
				runResponseValues[j] = cutoffTime * intraInstanceObjective.getPenaltyFactor();
			}
			
		}
	
		//=== Sanitize the data.
		//sanitizedData = new PCAModelDataSanitizer(instanceFeatureMatrix, thetaMatrix, numPCA, runResponseValues, usedInstanceIdxs, logModel, runHistory.getParameterConfigurationInstancesRanByIndex(), runHistory.getCensoredFlagForRuns(), configSpace);
		
		SanitizedModelData sanitizedData = new DefaultValueForConditionalsMDS(instanceFeatureMatrix, thetaMatrix, runResponseValues, usedInstanceIdxs, rfOptions.logModel,runHistory.getParameterConfigurationInstancesRanByIndexExcludingRedundant(), censored, configSpace);
		
		if(mbOptions.maskCensoredDataAsUncensored)
		{
			sanitizedData = new MaskCensoredDataAsUncensored(sanitizedData);
		}
		
		
		if(mbOptions.maskInactiveConditionalParametersAsDefaultValue)
		{
			sanitizedData = new MaskInactiveConditionalParametersWithDefaults(sanitizedData, configSpace);
		}
		
		
		//=== Actually build the model.
		ModelBuilder mb;
		//TODO: always go through AdaptiveCappingModelBuilder
		forest = null;
		preparedForest = null;
		if(adaptiveCapping)
		{
			mb = new AdaptiveCappingModelBuilder(sanitizedData, rfOptions, pool.getRandom("RANDOM_FOREST_BUILDING_PRNG"), mbOptions.imputationIterations, cutoffTime, intraInstanceObjective.getPenaltyFactor(), 1);
		} else
		{
			//mb = new HashCodeVerifyingModelBuilder(sanitizedData,smacConfig.randomForestOptions, runHistory);
			mb = new BasicModelBuilder(sanitizedData, rfOptions,1, pool.getRandom("RANDOM_FOREST_BUILDING_PRNG")); 
		}
		
		 /*= */
		forest = mb.getRandomForest();
		preparedForest = mb.getPreparedRandomForest();
	
		log.debug("Random Forest Built");
	}
	
	
	
	public RandomForest getRandomForest()
	{
		return forest;
	}
	
	public RandomForest getPreparedForest()
	{
		return preparedForest;
	}

}
