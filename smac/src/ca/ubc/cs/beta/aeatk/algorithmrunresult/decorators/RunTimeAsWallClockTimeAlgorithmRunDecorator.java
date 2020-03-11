package ca.ubc.cs.beta.aeatk.algorithmrunresult.decorators;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

/**
 * Reports the Wallclock time of an algorithm run as opposed to response time
 * @author Steve Ramage 
 *
 */
public class RunTimeAsWallClockTimeAlgorithmRunDecorator extends AbstractAlgorithmRunDecorator {

	public RunTimeAsWallClockTimeAlgorithmRunDecorator(AlgorithmRunResult run) {
		super(run);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2252084625866007671L;

	@Override
	public double getRuntime()
	{
		return getWallclockExecutionTime();
	}
}
