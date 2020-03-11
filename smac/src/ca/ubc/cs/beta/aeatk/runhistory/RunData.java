package ca.ubc.cs.beta.aeatk.runhistory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
/**
 * Value object that holds useful book keeping information about a run
 * @author seramage
 */
public class RunData 
{
	

	private final int iteration;
	private final int thetaIdx;
	private final int instanceidx;
	private final AlgorithmRunResult run;
	private final double responseValue;
	private final boolean cappedRun; 
	
	/**
	 * Constructor of Run Data
	 * @param iteration 	iteration of the run
	 * @param thetaIdx		index of the param configuration
	 * @param instanceIdx	index of the instance 
	 * @param run			the run object itself
	 * @param responseValue	the response value we care about
	 * @param cappedRun  	whether this run was capped 
	 */
	public RunData(int iteration, int thetaIdx, int instanceIdx, AlgorithmRunResult run, double responseValue, boolean cappedRun)
	{
		this.iteration=iteration;
		this.thetaIdx = thetaIdx;
		this.instanceidx = instanceIdx;
		this.run = run;
		this.responseValue = responseValue;
		this.cappedRun = cappedRun;
	}
	

	public int getIteration() {
		return iteration;
	}

	public int getThetaIdx() {
		return thetaIdx;
	}

	public int getInstanceidx() {
		return instanceidx;
	}

	public AlgorithmRunResult getRun() {
		return run;
	}

	public double getResponseValue() {
		return responseValue;
	}
	
	public boolean isCappedRun()
	{
		return cappedRun;
	}
	@Override
	public int hashCode()
	{
		return  run.hashCode();
	}
	/**
	 * Two RunData are considered equal if they have the run
	 */
	public boolean equals(Object o)
	{
		if(o instanceof RunData)
		{
			return ((RunData) o).getRun().equals(run);
		} else
		{
			return false;
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Iteration:").append(getIteration()).append(" Theta:");
		sb.append(getThetaIdx()).append(" Instance:").append(getInstanceidx()).append("=>").append(getResponseValue()).append("[").append(cappedRun).append("]");
		return sb.toString();
	}
}
