package ca.ubc.cs.beta.aeatk.algorithmrunconfiguration;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ec.util.MersenneTwister;

public class AlgorithmRunConfigurationHelper {

	public static AlgorithmRunConfiguration getRandomSingletonRunConfig(AlgorithmExecutionConfiguration execConfig)
	{
		return new AlgorithmRunConfiguration(new ProblemInstanceSeedPair(new ProblemInstance("Random"), (long) (Math.random()*100000)),124.0, ParameterConfigurationSpace.getSingletonConfigurationSpace().getRandomParameterConfiguration(new MersenneTwister()), execConfig);
		
	}

}
