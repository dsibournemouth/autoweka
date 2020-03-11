package ca.ubc.cs.beta.aeatk.misc.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.objectives.ObjectiveHelper;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;


public class ConfigurationDifference {
	
	public enum ChallengeResult
	{
		CONTINUE,
		REJECT_CHALLENGER,
		ACCEPT_CHALLENGER
	}
	
	public static ChallengeResult compareChallengerWithIncumbent(ThreadSafeRunHistory runHistory, ParameterConfiguration challenger, ParameterConfiguration incumbent, Random rand, ObjectiveHelper obj, double cutoffTime)
	{
	
		
		Set<ProblemInstance> piCommon = runHistory.getProblemInstancesRan(incumbent);
		piCommon.retainAll( runHistory.getProblemInstancesRan( challenger ));
		
		double incCost = runHistory.getEmpiricalCost(incumbent, piCommon,cutoffTime);
		double chalCost = runHistory.getEmpiricalCost(challenger, piCommon, cutoffTime);
		
		
		//System.out.println("Incumbent Cost:" + incCost);
		//System.out.println("Challenger Cost: " + chalCost);
		
		if( incCost + Math.pow(10, -6) < chalCost)
		{
			return ChallengeResult.REJECT_CHALLENGER;
		}
	
		
		
	

		List<AlgorithmRunResult> incumbentRuns = runHistory.getAlgorithmRunsExcludingRedundant(incumbent);
		
		List<AlgorithmRunResult> chalRuns = runHistory.getAlgorithmRunsExcludingRedundant(challenger);
		
		//TODO replace with common.
		Set<ProblemInstanceSeedPair> chalPisps = runHistory.getProblemInstanceSeedPairsRan(challenger);
		
		Set<ProblemInstanceSeedPair> common = new HashSet<>(runHistory.getProblemInstanceSeedPairsRan(incumbent));
		
		common.retainAll(chalPisps);
		
		
		
		//Want deterministic instance order  for these maps, so that results are deterministic when we sample from the instance seed pairs.
		Map<ProblemInstanceSeedPair, AlgorithmRunResult> challengerRunMap = new LinkedHashMap<>();
		Map<ProblemInstanceSeedPair, AlgorithmRunResult> incRunMap = new LinkedHashMap<>();
		
		
		for(AlgorithmRunResult run : incumbentRuns)
		{
			if(common.contains(run.getProblemInstanceSeedPair()))
			{
				incRunMap.put(run.getProblemInstanceSeedPair(), run);
			}
		}
		
		
		for(AlgorithmRunResult run : chalRuns)
		{
			if(common.contains(run.getProblemInstanceSeedPair()))
			{
				challengerRunMap.put(run.getProblemInstanceSeedPair(), run);
			}
		}
		
		List<ProblemInstanceSeedPair> instanceSeedPairs = new ArrayList<ProblemInstanceSeedPair>();
		
		
		
		instanceSeedPairs.addAll(challengerRunMap.keySet());
		
		
		final int BOOTSTRAP_SAMPLES = 1000;
		double[] bootstrapDiff = new double[1000];
		for(int B=0; B < 1000; B++)
		{
			List<AlgorithmRunResult> chalBootSamples = new ArrayList<>();
			List<AlgorithmRunResult> incBootSamples = new ArrayList<>();
			
			
			for(int i=0; i < instanceSeedPairs.size(); i++)
			{
				ProblemInstanceSeedPair pisp = instanceSeedPairs.get(rand.nextInt(instanceSeedPairs.size()));
				
				AlgorithmRunResult chalRun = challengerRunMap.get(pisp);
				
				if(chalRun == null)
				{
					throw new IllegalStateException("Challenger Run for pisp " + pisp + " was null this was unexpected");
				}
				AlgorithmRunResult incRun = incRunMap.get(pisp);			
				
				if(incRun == null)
				{
					throw new IllegalStateException("Incumbent Run for pisp " + pisp + " was null this was unexpected");
				}
				
				chalBootSamples.add(chalRun);
				incBootSamples.add(incRun);
			}
			
			double chal = obj.computeObjective(chalBootSamples);
			double inc = obj.computeObjective(incBootSamples);
	
			bootstrapDiff[B] = chal - inc;
		}
		
		
		/*
		//System.out.println(Arrays.toString(bootstrapDiff));
		for(int i=1; i < 21; i++)
		{
			Percentile p = new Percentile();
			double pValue =  p.evaluate(bootstrapDiff, i * 5);
			
			System.out.println(i * 5 + ":"+  pValue);
		}
		*/
		
		Percentile p = new Percentile();
		double sigLevel =  p.evaluate(bootstrapDiff, 95);
	
		//System.out.println("95 : " + sigLevel);
		
		if(sigLevel < 0)
		{
			return ChallengeResult.ACCEPT_CHALLENGER;
		} else
		{
			return ChallengeResult.CONTINUE;
		}
		 

		
		
	}
}
