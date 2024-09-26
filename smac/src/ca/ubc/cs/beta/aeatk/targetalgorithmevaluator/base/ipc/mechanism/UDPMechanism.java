package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.mechanism;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.ResponseParser;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.EncodingMechanism;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

public class UDPMechanism {
	
	private EncodingMechanism enc;

	public UDPMechanism(EncodingMechanism enc)
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
	public AlgorithmRunResult evaluateRun(AlgorithmRunConfiguration rc,  int port, String remoteAddr, int udpPacketSize) 
	{
		try {
			
			try(DatagramSocket clientSocket = new DatagramSocket()) 
			{
		
				InetAddress IPAddress = InetAddress.getByName(remoteAddr);
				
				byte[] sendData = enc.getOutputBytes(rc);
				if (sendData.length > udpPacketSize)
				{
					   throw new IllegalStateException("Response is too big to send to client, please adjust packetSize argument in both client and server " + sendData.length + " > " + udpPacketSize);	   
				}
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				byte[] receiveData = new byte[udpPacketSize];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				
				StopWatch watch = new AutoStartStopWatch();
				clientSocket.send(sendPacket);
				
				clientSocket.receive(receivePacket);
				watch.stop();
				
				receiveData = receivePacket.getData();
				
				
				InputStream bin = new ByteArrayInputStream(receiveData);
				
				
				return enc.getInputBytes(rc, bin, watch);
			}
			

		} catch (SocketException e1) {
			throw new TargetAlgorithmAbortException("TAE Aborted due to socket exception",e1);
		} catch(IOException e1)
		{
			throw new TargetAlgorithmAbortException("TAE Aborted due to IOException",e1);
		}
		
		
		
	}

}
