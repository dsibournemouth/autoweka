package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import net.jcip.annotations.ThreadSafe;

/**
 * This class keeps track of a set of outstanding runs and there current status, it is meant to assist in implementations of {@link TargetAlgorithmEvaluator} that may
 * divide or alter the schedule of runs.
 *  
 * <b>Implementation Note:</b> This class should really be used everywhere, as many TAE implementations redo the same thing, it's a shame it took two years to do this.
 * 
 * <b>Thread Safety:</b> Thread safety is assured via the use of synchronized methods. This should be okay for the time being, as none of the methods are blocking.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class PartialResultsAggregator {

	
	private final List<AlgorithmRunConfiguration> runConfigurations;
	
	/**
	 * These are runs that 
	 */
	private final Set<AlgorithmRunConfiguration> outstandingRunConfigurationSet;
	
	/**
	 * These are runs that have been observed (i.e., passed into {@link TargetAlgorithmEvaluatorRunObserver#currentStatus(List)})
	 */
	private final ConcurrentMap<AlgorithmRunConfiguration, AlgorithmRunResult> observerRunStatus = new ConcurrentHashMap<>();
	
	/**
	 * These are runs that have been completed (i.e., returned from {@link TargetAlgorithmEvaluator#evaluateRun(List, TargetAlgorithmEvaluatorRunObserver)} or passed in {@link TargetAlgorithmEvaluatorCallback#onSuccess(List)}) 
	 */
	private final ConcurrentMap<AlgorithmRunConfiguration, AlgorithmRunResult> completedRunsMap = new ConcurrentHashMap<>();
	
	
	/**
	 * This map contains kill handlers for initial runs, when we later update the observer map with other runs we will no
	 * longer use these handlers. 
	 */
	private final ConcurrentMap<AlgorithmRunConfiguration, KillHandler> nonAuthorativekillHandlerMap = new ConcurrentHashMap<>();
	
	public PartialResultsAggregator(List<AlgorithmRunConfiguration> initalRunConfigurations)
	{
		runConfigurations = Collections.unmodifiableList(new ArrayList<>(initalRunConfigurations));
		
		outstandingRunConfigurationSet = new LinkedHashSet<>(runConfigurations);
		
		if(runConfigurations.size() != outstandingRunConfigurationSet.size())
		{
			
			Set<AlgorithmRunConfiguration> rcs = new HashSet<>();

			Set<AlgorithmRunConfiguration> duplicates = new HashSet<>();
			
			for(AlgorithmRunConfiguration rc : runConfigurations)
			{
				if(rcs.contains(rc))
				{
					duplicates.add(rc);
				} 
				rcs.add(rc);
			}
			
			throw new IllegalArgumentException("Duplicate run configuration detected: " +  duplicates);
		}
		
		
		
		for(AlgorithmRunConfiguration rc : runConfigurations)
		{
			KillHandler kh = new StatusVariableKillHandler();
			nonAuthorativekillHandlerMap.put(rc, kh );
			observerRunStatus.put(rc, new RunningAlgorithmRunResult(rc, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),0 , kh));
			
		}
	}
	
	public synchronized Set<AlgorithmRunConfiguration> getOutstandingRunConfigurations() {
		return Collections.unmodifiableSet(outstandingRunConfigurationSet);
	}
	
	public synchronized List<AlgorithmRunConfiguration> getOutstandingRunConfigurationsAsList() {
		return Collections.unmodifiableList(new ArrayList<AlgorithmRunConfiguration>(outstandingRunConfigurationSet));
	}
	
	public synchronized boolean isCompleted()
	{
		return outstandingRunConfigurationSet.isEmpty();
	}
	
	
	public synchronized List<AlgorithmRunResult> getCurrentRunStatusForObserver()
	{
		List<AlgorithmRunResult> results = new ArrayList<>();
		for(AlgorithmRunConfiguration rc : runConfigurations)
		{	
			results.add(observerRunStatus.get(rc));
		}
		
		return results;
	}
	
	public synchronized List<AlgorithmRunResult> getCurrentRunStatusOnCompletion()
	{
		if(!isCompleted())
		{
			throw new IllegalStateException("Runs are not completed yet");
		}
		
		List<AlgorithmRunResult> results = new ArrayList<>();
		for(AlgorithmRunConfiguration rc : runConfigurations)
		{	
			results.add(completedRunsMap.get(rc));
		}
		
		return results;
	}
	
	public synchronized boolean updateCurrentRunStatus(AlgorithmRunResult result)
	{
		return _updateMap(result, observerRunStatus);
	}
	
	public synchronized boolean updateCurrentRunStatus(Collection<? extends AlgorithmRunResult> results)
	{
		return _updateMap(results, observerRunStatus);
	}
	
	public synchronized boolean updateCompletedRun(AlgorithmRunResult result)
	{
		outstandingRunConfigurationSet.remove(result.getAlgorithmRunConfiguration());
		_updateMap(result, observerRunStatus);
		return _updateMap(result, completedRunsMap);
	}
	
	public synchronized boolean updateCompletedRuns(Collection<? extends AlgorithmRunResult> results)
	{
		for(AlgorithmRunResult run : results)
		{
			outstandingRunConfigurationSet.remove(run.getAlgorithmRunConfiguration());
		}
		
		_updateMap(results, observerRunStatus);
		return _updateMap(results, completedRunsMap);
	}
	
	
	private synchronized boolean _updateMap(AlgorithmRunResult result, Map<AlgorithmRunConfiguration, AlgorithmRunResult> map)
	{

		if(observerRunStatus.containsKey(result.getAlgorithmRunConfiguration())) //This map always has a key for every run.
		{
			if(this.nonAuthorativekillHandlerMap.get(result.getAlgorithmRunConfiguration()).isKilled() && !result.isRunCompleted())
			{
				result.kill();
			}
			map.put(result.getAlgorithmRunConfiguration(), result);
		} else
		{
			throw new IllegalStateException("Attempted to add a run that we didn't request:" + result + " requests: " + this.runConfigurations);
		}
		
		return isCompleted();
	}
	
	private synchronized boolean _updateMap(Collection<? extends AlgorithmRunResult> results, Map<AlgorithmRunConfiguration, AlgorithmRunResult> map)
	{
		for(AlgorithmRunResult run : results)
		{
			
			_updateMap(run, map);
		}
		
		return isCompleted();
	}


	
	
	
	
	
	
}
