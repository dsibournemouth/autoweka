package ca.ubc.cs.beta.smac.handler;

import java.util.Collections;
import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.ChallengeStartEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.model.ModelBuildEndEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.model.ModelBuildStartEvent;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.models.fastrf.RandomForest;
import ca.ubc.cs.beta.smac.configurator.AbstractAlgorithmFramework;

public class ChallengePredictionHandler implements EventHandler<AutomaticConfiguratorEvent>{

	private AbstractAlgorithmFramework smac;
	private RandomForest forest;
	private ParamConfigurationOriginTracker configTracker;
	public ChallengePredictionHandler(AbstractAlgorithmFramework smac, ParamConfigurationOriginTracker configTracker)
	{
		this.smac = smac;
		this.configTracker = configTracker;
		
	}
	@Override
	public void handleEvent(AutomaticConfiguratorEvent event) {
		
		if(event instanceof ChallengeStartEvent)
		{
			if(forest != null)
			{
				ParameterConfiguration incumbent = smac.getIncumbent();
				ParameterConfiguration challenger = ((ChallengeStartEvent) event).getChallenger();
				
				double[] incumbentMeanVar = applyMarginalModel(incumbent)[0];
				double[] challengerMeanVar = applyMarginalModel(challenger)[0];
				
				//X1 (challenger) > X2 (incumbent)
				//http://math.stackexchange.com/questions/40224/probability-of-a-point-taken-from-a-certain-normal-distribution-will-be-greater
				
				double challengerMean = challengerMeanVar[0];
				double incumbentMean = incumbentMeanVar[0];
				
				double challengerVar = challengerMeanVar[1];
				double incumbentVar = incumbentMeanVar[1];
				
				
				double mean = getExpMean(challengerMean, challengerVar) - getExpMean(incumbentMean, incumbentVar);
				double var = getExpVar(challengerMean, challengerVar) + getExpVar(incumbentMean, incumbentVar);
				
				NormalDistribution normal = new NormalDistributionImpl();
				
				double probability=Double.NaN;
				try {
					
					 probability = normal.cumulativeProbability(-mean/var);
				} catch (MathException e) {
					//Do not care
				}
				
				configTracker.addConfiguration(challenger, "Challenge-Prediction", "Incumbent Mean=" + incumbentMeanVar[0],"IncumbentVariance=" + incumbentMeanVar[1], "ChallengerMean=" + challengerMeanVar[0], "ChallengerVariance=" + challengerMeanVar[1], "ProbabilityOfSuccess="+probability);
			}
		} else if(event instanceof ModelBuildStartEvent)
		{
			forest = null;
		} else if(event instanceof ModelBuildEndEvent)
		{
			forest = (RandomForest) ((ModelBuildEndEvent) event).getModelIfAvailable();
			
		}
		
	}
	
	
	private double getExpMean(double pred, double var)
	{
		double test_mu_n = pred;
        double test_var_n = var;
        
        double var_ln = Math.log(test_var_n/(test_mu_n*test_mu_n) + 1);
        double	mu_ln = Math.log(test_mu_n) - var_ln/2;
        
        double var_l10 = var_ln / Math.log(10) / Math.log(10);
        double mu_l10 = mu_ln / Math.log(10); 
        
        pred = mu_l10;
        var = var_l10;
        
        return pred;
	}
	private double getExpVar(double pred, double var)
	{
		double test_mu_n = pred;
        double test_var_n = var;
        
        double var_ln = Math.log(test_var_n/(test_mu_n*test_mu_n) + 1);
        double	mu_ln = Math.log(test_mu_n) - var_ln/2;
        
        double var_l10 = var_ln / Math.log(10) / Math.log(10);
        double mu_l10 = mu_ln / Math.log(10); 
        
        pred = mu_l10;
        var = var_l10;
        
        return var;
	}
	
	/**
	 * Computes a marginal prediction across all instances for the configArrays.
	 * @param configArrays
	 * @return
	 */
	protected double[][] applyMarginalModel(double[][] configArrays)
	{
		//=== Use all trees.
		int[] treeIdxsToUse = new int[forest.numTrees];
		for(int i=0; i <  forest.numTrees; i++)
		{
			treeIdxsToUse[i]=i;
		}
		
		//=== Get the marginal (from preprocessed forest if available).
		return RandomForest.applyMarginal(forest,treeIdxsToUse,configArrays);
		
		
	}
	
	/**
	 * Computes a marginal prediction across all instances for the configs. 
	 * @param configs
	 * @return
	 */
	protected double[][] applyMarginalModel(ParameterConfiguration configtoCheck)
	{
		List<ParameterConfiguration> configs = Collections.singletonList(configtoCheck);
		//=== Translate into array format, and call method for that format.
		double[][] configArrays = new double[configs.size()][];
		int i=0; 
		for(ParameterConfiguration config: configs)
		{
			configArrays[i] = config.toValueArray();
			i++;
		}
		return applyMarginalModel(configArrays);		
	}

}
