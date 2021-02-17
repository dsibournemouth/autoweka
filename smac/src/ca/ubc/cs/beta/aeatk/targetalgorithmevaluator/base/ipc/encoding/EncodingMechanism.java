package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding;

import java.io.IOException;
import java.io.InputStream;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;

public interface EncodingMechanism {

	public byte[] getOutputBytes(AlgorithmRunConfiguration runConfiguration);
	
	public AlgorithmRunResult getInputBytes(AlgorithmRunConfiguration rc, InputStream in, StopWatch watch) throws IOException;
}
