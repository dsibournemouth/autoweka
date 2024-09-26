package ca.ubc.cs.beta.aeatk.objectives;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
/**
 * Enumeration listing the various run objectives (converts an {@link AlgorithmRunResult} into a response value)
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public enum RunObjective {
	
	/**
	 * Uses the runtime from the wrapper output
	 */
	RUNTIME,
	
	/**
	 * Use the quality from the wrapper output
	 */
	QUALITY;
	public double getObjective(AlgorithmRunResult r)
	{
		
		switch(this)
		{
			case RUNTIME:
				switch(r.getRunStatus())
				{
					case TIMEOUT:
						//Return the requested cutoff time for the run ( <kappaMax if requested to be censored)
						return r.getAlgorithmRunConfiguration().getCutoffTime();
					case KILLED:						
					case RUNNING:
					case SAT:
					case UNSAT:
						//Use the runtime in this case
						return r.getRuntime();
					case ABORT:
					case CRASHED:
						return r.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration().getAlgorithmMaximumCutoffTime();
					default:
						throw new IllegalStateException("Unsure how to compute the " + this + " RunOjective for a run with result type " + r.getRunStatus() );
				}
				
						
			case QUALITY:
				return r.getQuality();
		}
		throw new UnsupportedOperationException(this + ": Run Objective Not Implemented");
	}
	
	public void validateInstanceSpecificInformation(ProblemInstance pi)
	{
		switch(this)
		{
		case RUNTIME:
		case QUALITY:
			return;
		default:
			throw new UnsupportedOperationException(this.toString() + " Run Objective Not Implemented");
		}
		
	}
}
