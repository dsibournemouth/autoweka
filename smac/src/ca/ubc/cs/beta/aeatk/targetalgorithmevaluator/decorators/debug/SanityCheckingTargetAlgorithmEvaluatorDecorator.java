package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * This decorator checks to see if the Target Algorithm Evaluator is honouring post-conditions
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class SanityCheckingTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public SanityCheckingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae) {
		super(tae);
	}

	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		List<AlgorithmRunConfiguration> originalList = Collections.unmodifiableList( new ArrayList<AlgorithmRunConfiguration>(runConfigs));
		
		
		
		List<AlgorithmRunResult> runs = tae.evaluateRun(runConfigs, new SanityCheckingTargetAlgorithmEvaluatorObserver(obs, originalList));
		
		List<AlgorithmRunConfiguration> returnedRuns = new ArrayList<AlgorithmRunConfiguration>(runs.size());
		
		for(AlgorithmRunResult run : runs)
		{
			returnedRuns.add(run.getAlgorithmRunConfiguration());
		}
		
		
		if(!returnedRuns.equals(originalList))
		{
			log.error("Misbehaiving Target Algorithm Evaluator Detected, run configs of returned runs DO NOT match run configs of submitted runs, either different run configs, or different order detected: \n Submitted: {},\n Observed: {}", originalList, returnedRuns );
		}
		
		return runs;
	}

	@Override
	public final void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		final List<AlgorithmRunConfiguration> originalList = Collections.unmodifiableList( new ArrayList<AlgorithmRunConfiguration>(runConfigs));
		
		
		TargetAlgorithmEvaluatorCallback handler = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				
				
				List<AlgorithmRunConfiguration> returnedRuns = new ArrayList<AlgorithmRunConfiguration>(runs.size());
				
				for(AlgorithmRunResult run : runs)
				{
					returnedRuns.add(run.getAlgorithmRunConfiguration());
				}
				
				
				if(!returnedRuns.equals(originalList))
				{
					log.error("Misbehaiving Target Algorithm Evaluator Detected, run configs of returned runs DO NOT match run configs of submitted runs, either different run configs, or different order detected: \n Submitted: {},\n Observed: {}", originalList, returnedRuns );
				}
				
				oHandler.onSuccess(runs);
			}

			@Override
			public void onFailure(RuntimeException e) {
				oHandler.onFailure(e);
				
			}
			
		};
		tae.evaluateRunsAsync(runConfigs, handler,  new SanityCheckingTargetAlgorithmEvaluatorObserver(obs, originalList));

	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		// TODO Auto-generated method stub
		
	}

	private class SanityCheckingTargetAlgorithmEvaluatorObserver implements TargetAlgorithmEvaluatorRunObserver{

		private final TargetAlgorithmEvaluatorRunObserver obs;
		private final List<AlgorithmRunConfiguration> originalList;
		public SanityCheckingTargetAlgorithmEvaluatorObserver(TargetAlgorithmEvaluatorRunObserver obs, List<AlgorithmRunConfiguration> originalList)
		{
			this.obs = obs;
			this.originalList = originalList;
		}
		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs) {
			
			List<AlgorithmRunConfiguration> observerRuns = new ArrayList<AlgorithmRunConfiguration>(runs.size());
			
			for(AlgorithmRunResult run : runs)
			{
				observerRuns.add(run.getAlgorithmRunConfiguration());
			}
			
			
			if(!observerRuns.equals(originalList))
			{
				log.error("Misbehaiving Target Algorithm Evaluator Detected, run configs of observed runs DO NOT match run configs of submitted runs, either different run configs, or different order detected: \n Submitted: {},\n Observed: {}", originalList, observerRuns );
			}
			
			if(obs != null)
			{
				obs.currentStatus(runs);
			}
		}
		
	}
	
}
