package ca.ubc.cs.beta.aeatk.algorithmrunresult;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;

/**
 * AlgorithmRun that reports that it's current status is RUNNING.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class RunningAlgorithmRunResult extends ExistingAlgorithmRunResult {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5427091882882378946L;
	private transient final KillHandler handler;

	public RunningAlgorithmRunResult(
			AlgorithmRunConfiguration runConfig, double runtime, double runlength, double quality, long seed, double walltime, KillHandler handler) {
		super( runConfig, RunStatus.RUNNING, runtime, runlength, quality, seed, walltime);
		this.handler = handler;
	}

	/*
	@Deprecated
	/**
	 * @deprecated  the constructor that doesn't take a result string is preferred.
	 */
	/*
	public RunningAlgorithmRun(AlgorithmExecutionConfig execConfig,
			RunConfig runConfig, String result, KillHandler handler) {
		super(execConfig, runConfig, result);
		this.handler = handler;
	}*/

	@Override
	public void kill() {
		if(handler != null)
		{
			handler.kill();
		}
		
	}

}
