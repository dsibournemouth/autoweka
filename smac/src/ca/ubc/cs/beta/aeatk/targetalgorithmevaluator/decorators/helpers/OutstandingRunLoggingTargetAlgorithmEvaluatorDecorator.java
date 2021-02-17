package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

@ThreadSafe
/***
 * Tracks the start and completion time of each run and logs it to a file
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	private final long ZERO_TIME = System.currentTimeMillis();
	private String resultFile;
	
	private String nameOfRuns;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final double resolutionInMS;
	

	private final ConcurrentHashMap<AlgorithmRunConfiguration, Double> startTime = new ConcurrentHashMap<AlgorithmRunConfiguration, Double>();
	private final ConcurrentHashMap<AlgorithmRunConfiguration, Double> endTime =  new ConcurrentHashMap<AlgorithmRunConfiguration, Double>();
	private final ConcurrentHashMap<AlgorithmRunConfiguration, Double> startWalltime = new ConcurrentHashMap<AlgorithmRunConfiguration, Double>();
	private final ConcurrentHashMap<AlgorithmRunConfiguration, Double> startCPUtime = new ConcurrentHashMap<AlgorithmRunConfiguration, Double>();
	
	private final ConcurrentHashMap<List<AlgorithmRunConfiguration>, Double> startBatchTime = new ConcurrentHashMap<List<AlgorithmRunConfiguration>, Double>();
	private final ConcurrentHashMap<List<AlgorithmRunConfiguration>, Double> endBatchTime = new ConcurrentHashMap<List<AlgorithmRunConfiguration>, Double>();
	
	
	
	public OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, String resultFile, double resolutionInSeconds, String nameOfRuns) {
		super(tae);
		this.resultFile = resultFile;
		this.nameOfRuns = nameOfRuns;
		this.resolutionInMS = resolutionInSeconds * 1000;
	}

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		startBatchTime.put(runConfigs, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
		
		TargetAlgorithmEvaluatorRunObserver wrappedObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				if(obs != null)
				{
					obs.currentStatus(runs);
				}
				processRuns(runs);
			}
			
		};
		
		try {
			return processRuns(tae.evaluateRun(processRunConfigs(runConfigs), wrappedObs));
		} finally
		{
			endBatchTime.put(runConfigs, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
		}
		
	}
	
	
	
	


	@Override
	public final void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		//We need to make sure wrapped versions are called in the same order
		//as there unwrapped versions.
	
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{
			private final TargetAlgorithmEvaluatorCallback handler = oHandler;

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
					runs = processRuns(runs);
					endBatchTime.put(runConfigs, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
					handler.onSuccess(runs);
			}

			@Override
			public void onFailure(RuntimeException t) {
					endBatchTime.putIfAbsent(runConfigs, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
					handler.onFailure(t);
			}
		};
		
		
		TargetAlgorithmEvaluatorRunObserver wrappedObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {				
				processRuns(runs);
				if(obs != null)
				{
					obs.currentStatus(runs);
				}
			}
			
		};
		
		
		startBatchTime.put(runConfigs, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
		
		tae.evaluateRunsAsync(processRunConfigs(runConfigs), myHandler, wrappedObs);

	}
	
	@SuppressWarnings("unchecked")
	public void postDecorateeNotifyShutdown()
	{
			
		log.trace("Processing detailed run statistics to {} ", this.resultFile);
		
		
		if(this.startTime.size() > this.endTime.size())
		{
			log.warn("Some runs are still outstanding, it is possible that we are shutting down prematurely, started: {} , finished: {}", this.startTime.size(), this.endTime.size());
		}
		
		if(this.startTime.size() < this.endTime.size())
		{
			
			for(Entry<AlgorithmRunConfiguration, Double> ent : this.startTime.entrySet())
			{
				log.error("At " + ent.getValue() + " : " + ent.getKey() + " started.");
			}
			
			for(Entry<AlgorithmRunConfiguration, Double> ent : this.endTime.entrySet())
			{
				log.error("At " + ent.getValue() + " : " + ent.getKey() + " ended. ");
			}
			
			throw new IllegalStateException("[BUG]: Determined that more algorithms ended " + this.endTime.size() + " than started " + this.startTime.size());
		}
		
		
		ConcurrentSkipListMap<Double, StartEnd> startEndMap = new ConcurrentSkipListMap<Double, StartEnd>();
		
		
		@SuppressWarnings("rawtypes")
		Collection[] myDoubles = { this.startTime.values(), this.endTime.values(), this.startWalltime.values(), this.startCPUtime.values(), this.startBatchTime.values(), this.endBatchTime.values()  };
		
		for(Collection<Double> cod : myDoubles)
		{
			for(Double d :  cod)
			{
				StartEnd e = startEndMap.get(d);
				
				if(e == null)
				{
					startEndMap.put(d, new StartEnd());
				}
				
			}
		}
		
		for(Entry<AlgorithmRunConfiguration, Double> startTimes : this.startTime.entrySet())
		{
			startEndMap.get(startTimes.getValue()).startDispatch++;
		}
		
		for(Entry<AlgorithmRunConfiguration, Double> endTimes : this.endTime.entrySet())
		{
			startEndMap.get(endTimes.getValue()).endDispatch++;
		}
		
		for(Entry<AlgorithmRunConfiguration, Double> startCPUTimes : this.startCPUtime.entrySet())
		{
			startEndMap.get(startCPUTimes.getValue()).startCPUtime++;
		}
		
		for(Entry<AlgorithmRunConfiguration, Double> startWallTimes : this.startWalltime.entrySet())
		{
			startEndMap.get(startWallTimes.getValue()).startWalltime++;
		}
		
		
		for(Entry<List<AlgorithmRunConfiguration>, Double> startCPUTimes : this.startBatchTime.entrySet())
		{
			startEndMap.get(startCPUTimes.getValue()).startBatchTime++;
		}
		
		for(Entry<List<AlgorithmRunConfiguration>, Double> startWallTimes : this.endBatchTime.entrySet())
		{
			startEndMap.get(startWallTimes.getValue()).endBatchTime++;
		}
		
		
		File f = new File(this.resultFile);
		try {
			FileWriter writer = new FileWriter(f);
			
			writer.write("Time (Zero is " + ZERO_TIME +"), Started, Ending, Number of " + this.nameOfRuns+  "  Runs, Approximate Start Based on CPU Time, Approximate Start Based on Walltime, Number of Running By CPU Time, Number of Running By Walltime, Started Batch, End Batch, Outstanding Batches\n");
			
			
			int outstanding = 0;
			int outstandingCPU = 0;
			int outstandingWall = 0;
			int outstandingBatches = 0;
			
			for(Entry<Double, StartEnd> ent : startEndMap.entrySet())
			{
				outstanding += ent.getValue().startDispatch - ent.getValue().endDispatch;
				
				outstandingCPU += ent.getValue().startCPUtime - ent.getValue().endDispatch;
				outstandingWall += ent.getValue().startWalltime - ent.getValue().endDispatch;
				
				outstandingBatches += ent.getValue().startBatchTime - ent.getValue().endBatchTime;
				
				writer.write(ent.getKey() + "," +  ent.getValue().startDispatch + "," + ent.getValue().endDispatch + "," + outstanding + "," + ent.getValue().startCPUtime + "," + ent.getValue().startWalltime + "," + outstandingCPU + "," + outstandingWall + "," +ent.getValue().startBatchTime + "," + ent.getValue().endBatchTime + "," + outstandingBatches+ "\n");
			}
			
			writer.flush();
			writer.close();

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
		log.trace("Processing complete");
	}
	

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected <K extends AlgorithmRunResult> K processRun(K run)
	{
		
		if(run.isRunCompleted())
		{
			AlgorithmRunConfiguration rc = run.getAlgorithmRunConfiguration();
			synchronized(rc)
			{
				if(endTime.get(rc) == null)
				{
					double endTimeX = (bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0;
					endTime.put(rc, Math.max(0,endTimeX));
				} else
				{
					//Return early because the other maps should be populated.
					return run;
				}
				
				if(startWalltime.get(rc) == null)
				{
					double startTimeX = ( bucketTime(System.currentTimeMillis() -  (long) (run.getWallclockExecutionTime()*1000) ) - ZERO_TIME) / 1000.0;
					
					startWalltime.put(rc, Math.max(0,startTimeX));
				}
				
				if(startCPUtime.get(rc) == null)
				{
					double startCPUTimeX = ( bucketTime(System.currentTimeMillis() - (long) ( run.getRuntime() * 1000 ) )  - ZERO_TIME) / 1000.0;
					
					startCPUtime.put(rc, Math.max(0,startCPUTimeX));
				}
				
					
			}
		}
		
		return run;
	}
	
	/**
	 * Template method that is invoked with each runConfig that we request
	 * @param rc the runconfig  being requested
	 * @return runConfig object to replace the run
	 */
	protected AlgorithmRunConfiguration processRun(AlgorithmRunConfiguration rc)
	{
		synchronized(rc)
		{
			if(startTime.get(rc) == null)
			{
				startTime.put(rc, Math.max(0,(bucketTime(System.currentTimeMillis()) - ZERO_TIME) / 1000.0));
			}
		}
	
		
		return rc;
	}
	
	
	protected final <K extends AlgorithmRunResult> List<K> processRuns(List<K> runs)
	{
		for(int i=0; i < runs.size(); i++)
		{
			runs.set(i, processRun(runs.get(i)));
		}
		
		return runs;
	}
	
	protected final List<AlgorithmRunConfiguration> processRunConfigs(List<AlgorithmRunConfiguration> runConfigs)
	{	
		runConfigs = new ArrayList<AlgorithmRunConfiguration>(runConfigs);
		for(int i=0; i < runConfigs.size(); i++)
		{
			runConfigs.set(i, processRun(runConfigs.get(i)));
		}
		return runConfigs;
	}
	
	
	

	private final long bucketTime(long time)
	{
		 return (long) ((long) ( (long) (time / resolutionInMS)) * resolutionInMS);  
	}
	
	
	private static class StartEnd
	{
		public long startDispatch = 0;
		public long endDispatch = 0;
		public long startCPUtime = 0;
		public long startWalltime = 0;
		public long startBatchTime = 0;
		public long endBatchTime = 0 ;
		
	}
}
