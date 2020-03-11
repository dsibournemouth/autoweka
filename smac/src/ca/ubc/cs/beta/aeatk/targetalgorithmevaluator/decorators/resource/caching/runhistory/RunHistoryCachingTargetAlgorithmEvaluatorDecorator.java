package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching.runhistory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.runhistory.ReadOnlyThreadSafeRunHistoryWrapper;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * Caching Target Algorithm Evaluator that queries the run history object for caching.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class RunHistoryCachingTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	
	/**
	 * Run history object to query for run information
	 */
	private final ThreadSafeRunHistory runHistory;
	
	private static final Logger log = LoggerFactory.getLogger(RunHistoryCachingTargetAlgorithmEvaluatorDecorator.class);
	
	private final AtomicInteger requests = new AtomicInteger(0);
	private final AtomicInteger cacheHits = new AtomicInteger(0);	
	
	public RunHistoryCachingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, ThreadSafeRunHistory runHistory)
	{
		super(tae);
		this.runHistory = new ReadOnlyThreadSafeRunHistoryWrapper(runHistory);
	}
	
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver observer) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, observer);
	}

	
	@Override
	public void evaluateRunsAsync( final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer) {
		
		
		
		List<AlgorithmRunConfiguration> runsToSubmit = new ArrayList<>();
		
		final Map<AlgorithmRunConfiguration, AlgorithmRunResult> currentStatus = new ConcurrentHashMap<>();
		
		
		
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
			AlgorithmRunResult run = runHistory.getAlgorithmRunResultForAlgorithmRunConfiguration(rc);
			if(run == null)
			{
				runsToSubmit.add(rc);
			} else
			{
				currentStatus.put(rc, run);
			}
		}
		
		
		TargetAlgorithmEvaluatorCallback newCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				for(AlgorithmRunResult run : runs)
				{
					currentStatus.put(run.getAlgorithmRunConfiguration(), run);
				}
				
				List<AlgorithmRunResult> returnList = new ArrayList<>(runConfigs.size());
				for(AlgorithmRunConfiguration runConfig : runConfigs)
				{
					returnList.add(currentStatus.get(runConfig));
				}
				
				//Notify the client 
				if(observer != null)
				{
					synchronized(observer)
					{
						observer.currentStatus(returnList);
					}
				}
				callback.onSuccess(returnList);
			}

			@Override
			public void onFailure(RuntimeException e) {
				callback.onFailure(e);
			}
			
		};
		requests.addAndGet(runConfigs.size());
		cacheHits.addAndGet(runConfigs.size() - runsToSubmit.size());
		
		
		log.trace("Submitting {} runs out of {} requested ",runsToSubmit.size(), runConfigs.size() );
		
	
		TargetAlgorithmEvaluatorRunObserver runObserver = new TargetAlgorithmEvaluatorRunObserver() {
				
				@Override
				public void currentStatus(List<? extends AlgorithmRunResult> runs) {
					for(AlgorithmRunResult run : runs)
					{
						currentStatus.put(run.getAlgorithmRunConfiguration(), run);
					}
					
					List<AlgorithmRunResult> returnList = new ArrayList<>(runConfigs.size());
					for(AlgorithmRunConfiguration runConfig : runConfigs)
					{
						returnList.add(currentStatus.get(runConfig));
					}
					
					if(observer != null)
					{
						observer.currentStatus(returnList);
					}
					
				}
			};
		
		tae.evaluateRunsAsync(runsToSubmit, newCallback, runObserver);
	}
		
		
		
	

	
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		
		
		NumberFormat nf = NumberFormat.getPercentInstance();
		int misses = requests.get() - cacheHits.get();
		
		int requestCount =  requests.get();

		
		//log.info(this.getClass().getSimpleName()+ ": Cache misses {}, Cache requests {}, Hit Rate {} ", misses, requests, nf.format( ((double) requestCount - misses) / requestCount)  );
		
	}

}
