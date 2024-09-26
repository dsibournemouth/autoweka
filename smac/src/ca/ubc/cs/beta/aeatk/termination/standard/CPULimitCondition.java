package ca.ubc.cs.beta.aeatk.termination.standard;

import java.util.Collection;
import java.util.Collections;

import com.google.common.util.concurrent.AtomicDouble;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.cputime.CPUTime;
import ca.ubc.cs.beta.aeatk.termination.ConditionType;
import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;

@ThreadSafe
public class CPULimitCondition extends AbstractTerminationCondition 
{

	private final double cpuTimeLimit;
	private AtomicDouble currentTime;
	
	private final String NAME = "CPUTIME";
	private final boolean countACTime;

	private final CPUTime cpuTime;
	
	//private final AtomicBoolean haveToStop = new AtomicBoolean(false);
	public CPULimitCondition(double totalCPUTimeLimit, boolean countACTime, CPUTime cpuTime)
	{
		this.cpuTimeLimit = totalCPUTimeLimit;
		this.currentTime = new AtomicDouble(0);
		this.countACTime = countACTime;
		this.cpuTime = cpuTime;
	}
	
	public double getTunerTime()
	{
		
		return currentTime.get() + ((countACTime) ? cpuTime.getCPUTime() : 0);
	}

	
	@Override
	public boolean haveToStop() {
		return (cpuTimeLimit <= getTunerTime());
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		double tunerTime = getTunerTime();
		
		return Collections.singleton(new ValueMaxStatus(ConditionType.TUNERTIME, tunerTime, cpuTimeLimit, NAME, "Configuration Time Budget", "s"));
	}

	@Override
	public synchronized void notifyRun(AlgorithmRunResult run) {
		currentTime.addAndGet(Math.max(0.1, run.getRuntime()));
	}
	
	@Override
	public String toString()
	{
		return currentStatus().toString();
	}

	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "total CPU time limit (" +  cpuTimeLimit +  " s) has been reached.";
		} else
		{
			return "";
		}
		
	}

	
}
