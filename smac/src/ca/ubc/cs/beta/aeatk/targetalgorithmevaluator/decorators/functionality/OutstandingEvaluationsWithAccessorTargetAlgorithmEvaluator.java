package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;

/**
 * The same as an {@link OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator} but 
 * also has an accessor for getting the outstanding RunConfigs. This is only useful for testing.
 * <br> 
 * <b>Note:</b> If it isn't clear you can decorate with this decorator, but you need to keep a reference 
 * to it as you will lose access to the new method once decorated. 
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class OutstandingEvaluationsWithAccessorTargetAlgorithmEvaluator extends	OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator {

	private final Set<List<AlgorithmRunConfiguration>> outstandingRuns = Collections.newSetFromMap(new ConcurrentHashMap<List<AlgorithmRunConfiguration>,Boolean>());
	
	private final Set<List<AlgorithmRunConfiguration>> unmodifyableView = Collections.unmodifiableSet(outstandingRuns);
	
	public OutstandingEvaluationsWithAccessorTargetAlgorithmEvaluator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}
	
	/***
	 * Additionally template methods
	 */
	
	protected void preRun(List<AlgorithmRunConfiguration> runConfigs)
	{
		
		
		//System.out.println(outstandingRuns);
		//System.out.println(runConfigs);
		boolean added = outstandingRuns.add(runConfigs);
		
		
		//System.out.println("Pre Run" + outstandingRuns.size() + " " + unmodifyableView.size() + " added:" + added);
		
	}
	
	protected void postRun(List<AlgorithmRunConfiguration> runConfigs)
	{
		//System.out.println("Post Run");
		outstandingRuns.remove(runConfigs);
	}
	
	public Set<List<AlgorithmRunConfiguration>> getOutstandingRunConfigs()
	{
		return this.unmodifyableView;
	}
	
	@Override
	protected void postDecorateeNotifyShutdown()
	{
		this.outstandingRuns.clear();
	}
}
