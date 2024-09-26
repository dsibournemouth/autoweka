package ca.ubc.cs.beta.aeatk.algorithmrunresult.decorators;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

/**
 * Abstract class that wraps another AlgorithmRun
 * @author Steve Ramage 
 *
 */
public class AbstractAlgorithmRunDecorator implements AlgorithmRunResult {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6324011218801256306L;
	
	private final AlgorithmRunResult wrappedRun;
	

	/**
	 * Wraps the specified run 
	 * @param run	run to wrap
	 */
	public AbstractAlgorithmRunDecorator(AlgorithmRunResult run)
	{
		this.wrappedRun = run;
		
	}

	
	@Override
	public AlgorithmRunConfiguration getAlgorithmRunConfiguration() {
		return wrappedRun.getAlgorithmRunConfiguration();
	}

	@Override
	public RunStatus getRunStatus() {
		return wrappedRun.getRunStatus();
	}

	@Override
	public double getRuntime() {
		return wrappedRun.getRuntime();
	}

	@Override
	public double getRunLength() {
		return wrappedRun.getRunLength();
	}

	@Override
	public double getQuality() {
		return wrappedRun.getQuality();
	}

	@Override
	public long getResultSeed() {
		return wrappedRun.getResultSeed();
	}

	@Override
	public String getResultLine() {
		return wrappedRun.getResultLine();
	}

	

	@Override
	public boolean isRunCompleted() {

		return wrappedRun.isRunCompleted();
	}

	
	@Override
	public String rawResultLine() {
		return wrappedRun.rawResultLine();
	}

	

	@Override
	public double getWallclockExecutionTime() {
		return wrappedRun.getWallclockExecutionTime();
	}

	@Override
	public String getAdditionalRunData() {

		return wrappedRun.getAdditionalRunData();
	}

	@Override
	public boolean isCensoredEarly() {
		return wrappedRun.isCensoredEarly();
	}

	@Override
	public void kill() {
		wrappedRun.kill();
	}


	@Override
	public ParameterConfiguration getParameterConfiguration() {
		
		return wrappedRun.getParameterConfiguration();
	}


	@Override
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfiguration() {
		
		return wrappedRun.getAlgorithmExecutionConfiguration();
	}


	@Override
	public ProblemInstanceSeedPair getProblemInstanceSeedPair() {

		return wrappedRun.getProblemInstanceSeedPair();
	}


	@Override
	public ProblemInstance getProblemInstance() {
		return wrappedRun.getProblemInstance();
	}
	

	
}
