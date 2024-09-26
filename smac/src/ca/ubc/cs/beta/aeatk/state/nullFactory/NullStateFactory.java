package ca.ubc.cs.beta.aeatk.state.nullFactory;

import java.io.File;
import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.exceptions.StateSerializationException;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.state.StateDeserializer;
import ca.ubc.cs.beta.aeatk.state.StateFactory;
import ca.ubc.cs.beta.aeatk.state.StateSerializer;

public class NullStateFactory implements StateFactory {


	@Override
	public StateSerializer getStateSerializer(String id, int iteration)
			throws StateSerializationException {
		return new NullStateSerializer();
	}

	@Override
	public StateDeserializer getStateDeserializer(String id,
			int restoreIteration, ParameterConfigurationSpace configSpace,
			List<ProblemInstance> instances, AlgorithmExecutionConfiguration execConfig, RunHistory rh)
			throws StateSerializationException {
		throw new UnsupportedOperationException("Can not deseialize a null state");
	}

	@Override
	public void purgePreviousStates() {
	}

	@Override
	public void copyFileToStateDir(String name, File f) {
	}

}
