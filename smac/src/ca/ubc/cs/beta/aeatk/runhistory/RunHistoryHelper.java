package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.SetInstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.random.RandomUtil;

public class RunHistoryHelper{

	private static Logger log =  LoggerFactory.getLogger(RunHistoryHelper.class);
	
	
	 /**
		 * Returns a random instance with the fewest runs for the configuration
		 * @param config  		ParamConfiguration to run
		 * @param instanceList  List of problem instances.
		 * @param rand			Random object used to break ties
		 * @return random instance with the fewest runs for a configuration
		 */
		protected static List<ProblemInstance> getRandomInstanceWithFewestRunsFor(RunHistory rh,ParameterConfiguration config, List<ProblemInstance> instanceList, Random rand, boolean deterministic, int N) {

			Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = rh.getPerformanceForConfig(config);
			
			/*
			 * First try and see if if there are some candidate instances with zero runs
			 */
			List<ProblemInstance> candidates = new ArrayList<ProblemInstance>(instanceList.size());
			candidates.addAll(instanceList);
			if (instanceSeedToPerformanceMap != null){
				//Allegedly this is a very slow operation (http://www.ahmadsoft.org/articles/removeall/index.html)
				candidates.removeAll(instanceSeedToPerformanceMap.keySet());
			}
			
			/*
			 * If not find the set with the smallest number of runs
			 */
			if (candidates.size() == 0){
				int minNumRuns = Integer.MAX_VALUE;
				for (Iterator<ProblemInstance> iterator = instanceList.iterator(); iterator.hasNext();) {
					ProblemInstance inst = iterator.next();
					int numRuns = instanceSeedToPerformanceMap.get(inst).size();
					if (numRuns <= minNumRuns){
						if (numRuns < minNumRuns){ // new value for fewest runs -> ditch all previous candidates
							candidates.clear();
							minNumRuns = numRuns;
						}
						candidates.add(inst);
					}
				}
			}
			
			if(!deterministic)
			{
				int[] permutations = RandomUtil.getPermutation(candidates.size(), 0, rand);
				RandomUtil.permuteList(candidates, permutations);
				
			}

			List<ProblemInstance> piDistribution = new ArrayList<ProblemInstance>(instanceList);			
			if(N > candidates.size())
			{
				
				while(N > candidates.size())
				{
					//=== Return a random element of the candidate instance set (it's sad there is no method for that in Java's Set).\
					int[] permutations = RandomUtil.getPermutation(piDistribution.size(), 0, rand);
					RandomUtil.permuteList(piDistribution, permutations);
					
					candidates.addAll(piDistribution);
				}
				
				
				return candidates.subList(0, N);
				
			} else if(N == candidates.size())
			{
				return candidates;
			} else
			{
				return candidates.subList(0, N);
			}
			
			
				
		}


		/**
		 * Determines a ProblemInstanceSeedPair to run for configuration subject to keeping all problem instances run within 1 of eachother
		 * 
		 * @param rh 			 			Thread Safe RunHistory object
		 * @param instanceSeedGenerator     Instance Seed Generator
		 * @param config			 		ParamConfiguration to run 
		 * @param instanceList 	 			List of problem instances
		 * @param rand					 	Random object used to break ties
		 * @return Random ProblemInstanceSeedPair object
		 */
		public static List<ProblemInstanceSeedPair> getRandomInstanceSeedWithFewestRunsFor( ThreadSafeRunHistory rh, InstanceSeedGenerator instanceSeedGenerator, ParameterConfiguration config, List<ProblemInstance> instanceList, Random rand, boolean deterministic, int N)
		{
			try {
				rh.readLock();
				return getRandomInstanceSeedWithFewestRunsFor((RunHistory) rh, instanceSeedGenerator, config, instanceList, rand, deterministic,N);
			} finally
			{
				rh.releaseReadLock();
			}
		}
		
		/**
		 * Determines a ProblemInstanceSeedPair to run for configuration subject to keeping all problem instances run within 1 of eachother
		 * 
		 * @param rh 			 			Thread Safe RunHistory object
		 * @param instanceSeedGenerator     Instance Seed Generator
		 * @param config 		 ParamConfiguration to run 
		 * @param instanceList 	 List of problem instances
		 * @param rand			 Random object used to break ties
		 * @return Random ProblemInstanceSeedPair object
		 */
		public static List<ProblemInstanceSeedPair> getRandomInstanceSeedWithFewestRunsFor( RunHistory rh, InstanceSeedGenerator instanceSeedGenerator, ParameterConfiguration config, List<ProblemInstance> instanceList, Random rand, boolean deterministic, int N) {
			List<ProblemInstance> pis = getRandomInstanceWithFewestRunsFor(rh, config, instanceList, rand, deterministic, N);
			
			List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>(N);
			
			
			
			ConcurrentMap<ProblemInstance, Set<Long>> assignedSeedsByPi = new ConcurrentHashMap<>();
			
			for(ProblemInstance pi : pis)
			{
				
				assignedSeedsByPi.putIfAbsent(pi, new HashSet<Long>());
				Map<ProblemInstance, LinkedHashMap<Long, Double>> instanceSeedToPerformanceMap = rh.getPerformanceForConfig(config);
				
				
				List<Long> seedsUsedByPi = rh.getSeedsUsedByInstance(pi);
				
				Set<Long> seedsUsedByPiConfigSet;
				if(instanceSeedToPerformanceMap == null || instanceSeedToPerformanceMap.get(pi) == null) 
				{ 
					seedsUsedByPiConfigSet = Collections.emptySet();
				} else
				{
					seedsUsedByPiConfigSet= instanceSeedToPerformanceMap.get(pi).keySet();
				}
				
				List<Long> seedsUsedByPiConfig = new ArrayList<Long>(seedsUsedByPiConfigSet);
				
				
				List<Long> potentialSeeds = new ArrayList<Long>(seedsUsedByPi.size() - seedsUsedByPiConfig.size());
				
				potentialSeeds.addAll(seedsUsedByPi);
				potentialSeeds.removeAll(seedsUsedByPiConfig);
				potentialSeeds.removeAll(assignedSeedsByPi.get(pi));
				long seed;
				if(potentialSeeds.size() == 0)
				{
				
					synchronized(instanceSeedGenerator)
					{	
						//We generate only positive seeds
						if(instanceSeedGenerator instanceof SetInstanceSeedGenerator)
						{
							if(instanceSeedGenerator.hasNextSeed(pi))
							{
								seed = instanceSeedGenerator.getNextSeed(pi); 
							} else
							{
								seed = -1;
							}
						} else
						{
							seed = instanceSeedGenerator.getNextSeed(pi); 
						}
						
					}
				} else
				{
					seed = potentialSeeds.get(rand.nextInt(potentialSeeds.size()));
				}
				ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, seed);
				pisps.add(pisp);
				assignedSeedsByPi.get(pi).add(seed);
			}
			
			List<ProblemInstanceSeedPair> newPISPS = new ArrayList<ProblemInstanceSeedPair>(pisps);
			
			Set<ProblemInstanceSeedPair> setPISPS = new HashSet<ProblemInstanceSeedPair>(newPISPS);
			
			
			
			if(setPISPS.size() != newPISPS.size())
			{
				throw new IllegalStateException("Duplicated problem instance seed paris generated oddly enough");
			}
			
			
			return pisps;
			
		}
		


	/**
	 * Determines a ProblemInstanceSeedPair to run for configuration subject to keeping all problem instances run within 1 of eachother
	 * 
	 * @param rh 			 			Thread Safe RunHistory object
	 * @param instanceSeedGenerator     Instance Seed Generator
	 * @param config			 		ParamConfiguration to run 
	 * @param instanceList 	 			List of problem instances
	 * @param rand					 	Random object used to break ties
	 * @return Random ProblemInstanceSeedPair object
	 */
	public static ProblemInstanceSeedPair getRandomInstanceSeedWithFewestRunsFor( ThreadSafeRunHistory rh, InstanceSeedGenerator instanceSeedGenerator, ParameterConfiguration config, List<ProblemInstance> instanceList, Random rand, boolean deterministic)
	{
	
			return getRandomInstanceSeedWithFewestRunsFor(rh, instanceSeedGenerator, config, instanceList, rand, deterministic,1).get(0);
	
	}
	
	/**
	 * Determines a ProblemInstanceSeedPair to run for configuration subject to keeping all problem instances run within 1 of eachother
	 * 
	 * @param rh 			 			Thread Safe RunHistory object
	 * @param instanceSeedGenerator     Instance Seed Generator
	 * @param config 		 ParamConfiguration to run 
	 * @param instanceList 	 List of problem instances
	 * @param rand			 Random object used to break ties
	 * @return Random ProblemInstanceSeedPair object
	 */
	public static ProblemInstanceSeedPair getRandomInstanceSeedWithFewestRunsFor( RunHistory rh, InstanceSeedGenerator instanceSeedGenerator, ParameterConfiguration config, List<ProblemInstance> instanceList, Random rand, boolean deterministic) {
		return getRandomInstanceSeedWithFewestRunsFor(rh, instanceSeedGenerator, config, instanceList, rand, deterministic,1).get(0);
	}

	/**
	 * Returns a breakdown of each individual run cost
	 * @return double[] reporting the response value for every run, under the run objective
	 */
	public static double[] getRunResponseValues(List<AlgorithmRunResult> runs, RunObjective runObj)
	{

		double[] responseValues = new double[runs.size()];
		int i=0;
		for(AlgorithmRunResult run : runs)
		{
			responseValues[i] = runObj.getObjective(run);
			i++;
		}
		return responseValues;
	}
	
	/**
	 * Returns an array containing a boolean for each run that tells us whether this run was capped or not.
	 * 
	 * @return boolean array signifying whether a run was capped
	 */
	public final static boolean[] getCensoredEarlyFlagForRuns(List<AlgorithmRunResult> runs)
	{
		boolean[] censored = new boolean[runs.size()];
		int i = 0;
		for(AlgorithmRunResult run : runs)
		{
			censored[i] = run.isCensoredEarly(); 
			i++;
		}
		return censored;
		
	}	
	
}
