package ca.ubc.cs.beta.aeatk.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

public class ObjectiveHelper {

	private final RunObjective runObj;
	private final OverallObjective intraObjective;
	private final OverallObjective interObjective;
	private final double cutoffTime;

	public ObjectiveHelper(RunObjective runObj,OverallObjective intraObjective , OverallObjective interObjective, double cutoffTime)
	{
		this.runObj = runObj;
		this.intraObjective = intraObjective;
		this.interObjective = interObjective;
		this.cutoffTime = cutoffTime;
	}
	
	/**
	 * Too lazy to pass this object around properly, you can delete this method and refactor what breaks
	 * @return the run objective associated with this object
	 */
	public RunObjective getRunObjective()
	{
		return runObj;
	}
	/**
	 * Computes the objective for a given set of runs 
	 * <p>
	 * <b>Implementation Note: </b> I'm not sure what the slack is suppose to do with this method, it has something to do with dSMAC and knowing if we have exceeded the objective.
	 * 
	 * @param runs			A set of runs that all have the same configuration
	 * @param capSlack 		The amount of slack that is permitted to runs
	 * @return the computed objective
	 */
	public double computeObjective(List<? extends AlgorithmRunResult> runs, final double capSlack)
	{
		
		List<ProblemInstance> instances = new ArrayList<ProblemInstance>(runs.size());
		ConcurrentHashMap<ProblemInstance, List<ProblemInstanceSeedPair>> map = new ConcurrentHashMap<ProblemInstance, List<ProblemInstanceSeedPair>>();
		ConcurrentHashMap<ProblemInstance, List<Double>> performance = new ConcurrentHashMap<ProblemInstance, List<Double>>();
		
		
		double remainingCapSlack = capSlack;
		for(AlgorithmRunResult run : runs)
		{
			ProblemInstance pi = run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getProblemInstance();
			
			instances.add(pi);
			map.putIfAbsent(pi,new ArrayList<ProblemInstanceSeedPair>());
			performance.putIfAbsent(pi,new ArrayList<Double>());
		
			map.get(pi).add(run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair());
			
			double obj = runObj.getObjective(run);
			obj -= remainingCapSlack;
			
			if(obj < 0)
			{ //Remaining slack left over
				remainingCapSlack = -obj;
				obj = 0;
			} else
			{
				remainingCapSlack = 0;
			}
			
			performance.get(pi).add(obj);
			
		}
		
		
		
		List<Double> intraInstanceObjectiveValues = new ArrayList<Double>(instances.size());
		
		for(Entry<ProblemInstance, List<Double>> prefEnt : performance.entrySet())
		{
			intraInstanceObjectiveValues.add(intraObjective.aggregate(prefEnt.getValue(), cutoffTime));			
		}
		
		
		
		return interObjective.aggregate(intraInstanceObjectiveValues, cutoffTime);
	}

	public double computeObjective(List<? extends AlgorithmRunResult> runs) {
		return computeObjective(runs,0);
	}

}
