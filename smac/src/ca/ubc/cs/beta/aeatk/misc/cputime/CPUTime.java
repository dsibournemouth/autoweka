package ca.ubc.cs.beta.aeatk.misc.cputime;

/**
 * Class that tracks CPUTime usage since object creation
 * <br>
 * Previously this class used static methods to just give you the time, but this was encapsulated as an object so that you could essentially re-zero the CPU Time measurement,
 * as otherwise you could only accrue CPU time. This would make it difficult to run other meta-algorithmic procedures in the same instance of the JVM.
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public final class CPUTime {
	
	public static final CPUTime getCPUTimeTracker()
	{
		return new CPUTime();
	}
	private final double startCPUTime;
	private final double startUserTime;
	
	public CPUTime()
	{
		startCPUTime = CPUTimeCalculator._getCPUTime();
		startUserTime = CPUTimeCalculator._getUserTime();
	}
	
	public double getCPUTime()
	{
		return CPUTimeCalculator._getCPUTime() - startCPUTime;
	}
	
	public double getUserTime()
	{
		return CPUTimeCalculator._getUserTime() - startUserTime;
	}
	
	public static double getCPUTimeSinceJVMStart()
	{
		return CPUTimeCalculator._getCPUTime();
	}
	
	public static double getUserTimeSinceJVMStart()
	{
		return CPUTimeCalculator._getUserTime();
	}
	
	public String toString()
	{
		return "CPUTime: " + getCPUTime() + " User:" + getUserTime(); 
	}
}
