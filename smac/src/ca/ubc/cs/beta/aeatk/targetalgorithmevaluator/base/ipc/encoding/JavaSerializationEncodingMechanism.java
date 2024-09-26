package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;

@ThreadSafe
public class JavaSerializationEncodingMechanism implements EncodingMechanism {

	private static final Logger log = LoggerFactory.getLogger(JavaSerializationEncodingMechanism.class);
	
	@Override
	public byte[] getOutputBytes(AlgorithmRunConfiguration runConfiguration) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(bout);
			out.writeObject(runConfiguration);
			out.close();
			
			
			return bout.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Object output stream should not have been able to throw an exception at this point",e);
		}
		
		
	}
	

	@Override
	public AlgorithmRunResult getInputBytes(AlgorithmRunConfiguration rc,	InputStream in, StopWatch watch) throws IOException {

		ObjectInputStream oin = new ObjectInputStream(in);
		
		AlgorithmRunResult runResult;
		try {
			runResult = (AlgorithmRunResult) oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Should have been able to find the class",e);
		}
		
		if(runResult.getAlgorithmRunConfiguration().equals(rc))
		{
			return runResult;
		} else
		{
			//log.error("Mismatch in run configurations detected local: {} vs remote: {} , Full Run: {} ", rc, runResult.getAlgorithmRunConfiguration(), runResult);
			throw new IllegalStateException("Remote gave us an answer for a RunConfiguration we weren't expecting");
		}
		
	}

}
