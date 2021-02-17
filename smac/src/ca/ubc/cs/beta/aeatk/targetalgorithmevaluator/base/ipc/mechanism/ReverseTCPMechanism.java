package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.mechanism;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.ResponseParser;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.EncodingMechanism;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

public class ReverseTCPMechanism {


	private final Logger log = LoggerFactory.getLogger(getClass());
	private EncodingMechanism enc;
	public ReverseTCPMechanism(EncodingMechanism enc) 
	{
		this.enc = enc;
		
	}

	/**
	 * 
	 * @param rc
	 * @param execConfig
	 * @param port
	 * @param remoteAddr
	 * @param udpPacketSize
	 * @return
	 */
	public AlgorithmRunResult evaluateRun(InputStream in, OutputStream out, AlgorithmRunConfiguration rc) throws IOException
	{
		try 
		{
			OutputStream bout = out; 
			
			bout.write(enc.getOutputBytes(rc));
		
			
			bout.flush();
			
			
			//AlgorithmRunResult run = rtcp.evaluateRun(in,out, rc);
			
			StopWatch watch = new AutoStartStopWatch();
		
			return enc.getInputBytes(rc,in, watch);
		
		} finally
		{
			//clientSocket.close();
		}
	}
}
