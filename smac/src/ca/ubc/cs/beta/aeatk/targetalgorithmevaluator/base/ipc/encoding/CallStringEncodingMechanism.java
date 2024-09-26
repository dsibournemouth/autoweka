package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.ResponseParser;

@ThreadSafe
public class CallStringEncodingMechanism implements EncodingMechanism {

	private static final Logger log = LoggerFactory.getLogger(CallStringEncodingMechanism.class);
	@Override
	public byte[] getOutputBytes(AlgorithmRunConfiguration rc) {

		ArrayList<String> list = new ArrayList<String>();
		list.add(rc.getProblemInstanceSeedPair().getProblemInstance().getInstanceName());
		list.add(rc.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation());
		list.add(String.valueOf(rc.getCutoffTime()));
		list.add(String.valueOf(Integer.MAX_VALUE));
		list.add(String.valueOf(rc.getProblemInstanceSeedPair().getSeed()));
		
		ParameterStringFormat f = ParameterStringFormat.NODB_SYNTAX;
		
		for(String key : rc.getParameterConfiguration().getActiveParameters()  )
		{
			
			
			if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
			{
				throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
			}
			list.add(f.getPreKey() + key);
			list.add(f.getValueDelimeter() + rc.getParameterConfiguration().get(key)  + f.getValueDelimeter());	
			
		}
		
		StringBuilder sb = new StringBuilder();
		for(String s : list)
		{
			if(s.matches(".*\\s+.*"))
			{
				sb.append("\""+s + "\"");
			} else
			{
				sb.append(s);
			}
			sb.append(" ");
		}
		
		sb.append("\n");
		try {
			return sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		
	}

	@Override
	public AlgorithmRunResult getInputBytes(AlgorithmRunConfiguration rc, InputStream in, StopWatch watch) throws IOException {
		
		
		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		
		String serverLine;
		
		
		Queue<String> last10Lines = new LinkedList<String>();
		while( (serverLine = bin.readLine() ) != null)
		{
			last10Lines.add(serverLine);
			if(last10Lines.size() > 10)
			{
				last10Lines.poll();
			}
			return ResponseParser.processLine(serverLine, rc,  watch.time() / 1000.0);
			
		}
		
		log.error("We didn't retrieve anything from the server that looked like it matched the required format, outputting the last 10 lines");
		for(String line : last10Lines)
		{
			log.error("> {}", line);
		}
		
		return new ExistingAlgorithmRunResult( rc, RunStatus.CRASHED, 0, 0, 0, 0, "No matching input from on input stream");
	};
	


}
