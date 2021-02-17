package ca.ubc.cs.beta.aeatk.eventsystem.handlers;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.ChallengeEndEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.ChallengeStartEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.state.StateRestoredEvent;
import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ca.ubc.cs.beta.aeatk.misc.cputime.CPUTime;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;
import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;


/**
 * Logs some runtime information from the RunHistory and Termination Condition objects
 * <p>
 * <b>Events:</b> Requires IncumbentChangeEvent presently.
 * AlgorithmRunCompleted event is used as well (but not required)
 *   
 * and any other events that it receives will cause it to log the runhistory
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class LogRuntimeStatistics implements EventHandler<AutomaticConfiguratorEvent> 
{
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ThreadSafeRunHistory runHistory;
	private final TerminationCondition termCond;;
	
	private final AtomicReference<String> lastString = new AtomicReference<String>();
	
	private final AtomicReference<IncumbentPerformanceChangeEvent> lastICE = new AtomicReference<IncumbentPerformanceChangeEvent>();
	
	private final double cutoffTime;
	
	private final AtomicInteger logCount = new AtomicInteger(1);
	private final long msToWait; 
	
	private long lastMessage = Long.MIN_VALUE;
	
	private double sumOfRuntime = 0.0;
	private double sumOfWallclockTime = 0.0;
	
	private final AtomicBoolean noIceMessage = new AtomicBoolean(false);
	private final TargetAlgorithmEvaluator tae;
	
	
	private final AtomicInteger challengesStarted = new AtomicInteger(0);
	private final AtomicInteger challengesEnded = new AtomicInteger(0);
	
	private final boolean showChallenges;
	
	private final CPUTime cpuTime;
	
	//== The logOnEvents argument to the constructor is annoying to get right for clients 
	//as most ways of automatically creating Collections, such as Arrays.asList() won't infer the correct type and result in annoying syntax.
	//It doesn't matter since we only call contains on this anyway.
	@SuppressWarnings("rawtypes")
	private final Set<Class> logOnEvents;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LogRuntimeStatistics(ThreadSafeRunHistory rh, TerminationCondition termCond, double cutoffTime, TargetAlgorithmEvaluator tae, boolean showChallenges, CPUTime cpuTime, Collection<?> logOnEvents)
	{
		this.runHistory = rh;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.msToWait = 0;
		lastString.set("No Runtime Statistics Logged");
		this.tae = tae;
		this.showChallenges = showChallenges;
		this.cpuTime = cpuTime;
		
		this.logOnEvents = new HashSet(logOnEvents);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LogRuntimeStatistics(ThreadSafeRunHistory rh, TerminationCondition termCond, double cutoffTime , long msToWait, TargetAlgorithmEvaluator tae, boolean showChallenges,CPUTime cpuTime, Collection<?> logOnEvents)
	{
		this.runHistory = rh;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.msToWait = msToWait;
		lastString.set("No Runtime Statistics Logged");
		
		this.tae = tae;
		this.showChallenges = showChallenges;
		this.cpuTime = cpuTime;
		
		this.logOnEvents = new HashSet(logOnEvents);
	}

	
	@Override
	public synchronized void handleEvent(AutomaticConfiguratorEvent event) {
			
		
		if(event instanceof IncumbentPerformanceChangeEvent)
		{
			IncumbentPerformanceChangeEvent ice = (IncumbentPerformanceChangeEvent) event;
			
			
			if((this.lastICE.get() == null) || this.lastICE.get().getTunerTime() < ice.getTunerTime())
			{
				lastICE.set(ice);
				
			}
		} else if( event instanceof AlgorithmRunCompletedEvent )
		{
			this.sumOfWallclockTime += ((AlgorithmRunCompletedEvent) event).getRun().getWallclockExecutionTime();
			this.sumOfRuntime += ((AlgorithmRunCompletedEvent) event).getRun().getRuntime();
			
		} else if( event instanceof StateRestoredEvent)
		{
			this.logCount.set(((StateRestoredEvent) event).getModelsBuilt());
			
			for(AlgorithmRunResult run : ((StateRestoredEvent) event).getRunHistory().getAlgorithmRunsIncludingRedundant())
			{
				this.sumOfWallclockTime += run.getWallclockExecutionTime();
				this.sumOfRuntime += run.getRuntime();
			}
		} else if(event instanceof ChallengeStartEvent)
		{
			this.challengesStarted.incrementAndGet();
		} else if(event instanceof ChallengeEndEvent)
		{
			this.challengesEnded.incrementAndGet();
		}
		
		if(logOnEvents.contains(event.getClass()))
		{
			try {
				runHistory.readLock();
				String myLastLogMessage;
				if(this.lastICE.get() == null)
				{
					if(this.noIceMessage.get() == false)
					{
						log.debug("Runtime Statistics are Not Available because we haven't seen an Incumbent Performance Changed Event yet");	
					}
					return;
				}
				ParameterConfiguration incumbent = this.lastICE.get().getIncumbent();
				
				
				Object[] arr = { logCount.get(),
						runHistory.getThetaIdx(incumbent) + " (internal ID:" + incumbent +")",
						runHistory.getTotalNumRunsOfConfigExcludingRedundant(incumbent),
						runHistory.getProblemInstancesRan(incumbent).size(),
						runHistory.getUniqueParamConfigurations().size(),
						format(runHistory.getEmpiricalCost(incumbent, runHistory.getUniqueInstancesRan(), this.cutoffTime)),
						"N/A", 
						"N/A",
						"N/A" ,
						"N/A", //options.runtimeLimit - wallTime 
						format(termCond.getTunerTime()),
						"N/A", //options.scenarioConfig.tunerTimeout - tunerTime,
						format(runHistory.getTotalRunCost()),
						format(cpuTime.getCPUTime()),
						format(cpuTime.getUserTime()) ,
						format(this.sumOfRuntime),
						format(this.sumOfWallclockTime),
						format0(Runtime.getRuntime().maxMemory() / 1024.0 / 1024),
						format0(Runtime.getRuntime().totalMemory() / 1024.0 / 1024),
						format0(Runtime.getRuntime().freeMemory() / 1024.0 / 1024) };
				
				StringBuilder sb = new StringBuilder(" ");
				for(ValueMaxStatus vms : termCond.currentStatus())
				{
					sb.append(vms.getStatus());
				}
				
				int challengersStarted = this.challengesStarted.get();
				
				//== It's incredibly unlikely but another challenge could start, and end in the interim, so we will just 'massage' it here, so that users aren't confused.
				int challengersEnded = Math.min(this.challengesEnded.get(), challengersStarted);
				
				
				String challenges = "";
				if(showChallenges)
				{
					challenges = "\n Number of Challenges Started: " + challengesStarted + 
					"\n Number of Challenges Oustanding:" + (challengersStarted - challengersEnded);  
				}
				
				myLastLogMessage = "*****Developer Statistics*****" +
						"\n Count: " + arr[0]+
						"\n Incumbent ID: "+ arr[1]+
						"\n Number of PISPs for Incumbent: " + arr[2] +
						"\n Number of Instances for Incumbent: " + arr[3]+
						"\n Number of Configurations Run: " + arr[4]+ 
						"\n Performance of the Incumbent: " + arr[5]+
						challenges+
						//"\n Total Number of runs performed: " + arr[6]+ 
						//"\n Last Iteration with a successful run: " + arr[7] + "\n" +
						"\n" + sb.toString().replaceAll("\n","\n ") + 
						//"\n Wallclock time: "+ arr[8] + " s" +
						//"\n Wallclock time remaining: "+ arr[9] +" s" +
						//"\n Configuration time budget used: "+ arr[10] +" s" +
						//"\n Configuration time budget remaining: "+ arr[11]+" s" +
						"Sum of Target Algorithm Execution Times (treating minimum value as 0.1): "+arr[12] +" s" + 
						"\n CPU time of Configurator: "+arr[13]+" s" +
						"\n User time of Configurator: "+arr[14]+" s" +
						"\n Outstanding Runs on Target Algorithm Evaluator: " + tae.getNumberOfOutstandingRuns() +
						"\n Outstanding Requests on Target Algorithm Evaluator: " + tae.getNumberOfOutstandingBatches() +  
						"\n Total Reported Algorithm Runtime: " + arr[15] + " s" + 
						"\n Sum of Measured Wallclock Runtime: " + arr[16] + " s" +
						"\n Max Memory: "+arr[17]+" MB" +
						"\n Total Java Memory: "+arr[18]+" MB" +
						"\n Free Java Memory: "+arr[19]+" MB" + "\n * PISP count is roughly the number of runs, but doesn't included redundant runs on the same problem instance & seed";
				
			lastString.set(myLastLogMessage);
			} finally
			{
				runHistory.releaseReadLock();
			}
			
			
			if(msToWait + lastMessage > System.currentTimeMillis())
			{
				
				return;
			} else
			{
				logCount.incrementAndGet();
				lastMessage = System.currentTimeMillis();
				log.debug(lastString.get());
			}
			
			
		} 
		
		
	}

	public void logLastRuntimeStatistics() {
		log.info(CommonMarkers.SKIP_CONSOLE_PRINTING,lastString.get());
	}

	private static final DecimalFormat df0 = new DecimalFormat("0"); 
	private static synchronized String format0(double d)
	{
		return df0.format(d);
	}
	
	private static final DecimalFormat df = new DecimalFormat("0.000"); 
	private static synchronized String format(double d)
	{
		return df.format(d);
	}
	


}
