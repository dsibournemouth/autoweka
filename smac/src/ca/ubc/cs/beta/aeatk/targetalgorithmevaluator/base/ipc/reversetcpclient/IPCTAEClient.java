package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.reversetcpclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.IPCTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import com.beust.jcommander.ParameterException;

/**
 * Client that responds to IPC TAE requests. The protocol is to establish a connection to the given host and port every time a new run config is needed, solve
 * the run config, send the result back and then close the connection.
 * 
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>
 */
public class IPCTAEClient {
    
	private static Logger log = null;
    
    private static final long SLEEP_TIME = 2;
    
    public static void main(String[] args) throws UnknownHostException, IOException {
        
        /*
         * Parse arguments.
         */
        IPCTAEClientParameters parameters = new IPCTAEClientParameters();
        
        Map<String, AbstractOptions> TAEOptionsMap = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
        try
        {
	        try
	        {
	        	parameters.fTAEOptions.turnOffCrashes();
	            JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters, TAEOptionsMap);
	        } catch (ParameterException aParameterException)
	        {
	            throw aParameterException;
	        }
        } finally
        {
        	parameters.log.initializeLogging();
        	log =  LoggerFactory.getLogger(IPCTAEClient.class);
        }
        /*
         * Construct the TAE.
         */
        
        try(TargetAlgorithmEvaluator tae = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(parameters.fTAEOptions, false, false, TAEOptionsMap, null))
        {
            
            int failedConnections = 1;
            
            boolean successfulRun = false;
            
            /*
             * Get a run to do.
             */
            log.info("IPC TAE Client started sending requests to: {}:{}", parameters.fHost,parameters.fPort);
            Socket clientSocket = new Socket(parameters.fHost, parameters.fPort);
            
            clientSocket.setTcpNoDelay(true);
            while(true)
            {
            	if (clientSocket.isClosed() || !clientSocket.isConnected())
            	{
            		clientSocket.close();
            		log.debug("Establishing connection to {}:{} ...",parameters.fHost,parameters.fPort);
            		clientSocket = new Socket(parameters.fHost, parameters.fPort);
            		clientSocket.setTcpNoDelay(true);
            	}
                
                try 
                {
               
                    //Receive the run config.
                    //log.debug("Receiving algorithm run configuration ...");
                    
                	StringBuilder sb = new StringBuilder();
                    AutoStartStopWatch watch = new AutoStartStopWatch();
                    AlgorithmRunConfiguration runConfig = receiveRunConfig(clientSocket);
                    
                    //Execute it.
                    log.debug("Solving run configuration ...");
                    List<AlgorithmRunResult> results = tae.evaluateRun(runConfig);
                    AlgorithmRunResult result = results.get(0);
                    sb.append("Eval time: " + watch.time()).append(" ");
                    //Send the run result.
                    log.debug("Sending back algorithm run result ...");
                    sendRunResult(result, clientSocket);
                    
                    // sb.append("Send time: " + watch.time()).append(" ");
                    //System.out.print("\n");
                    successfulRun = true;
                    
                
                } catch (ConnectException e)
                {
                    if(failedConnections > parameters.fRetryAttemps)
                    {
                        //log.error("No server detected at {}:{} after {} attemps, terminating.",parameters.fHost,parameters.fPort,parameters.fRetryAttemps);
                        return;
                    }
                    else if(!successfulRun)
                    {
                        failedConnections++;
                        //log.debug("No server detected at {}:{}, sleeping for {} seconds and trying again.",parameters.fHost,parameters.fPort,SLEEP_TIME);
                        try {
                            Thread.sleep(SLEEP_TIME*1000);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    else
                    {
                        log.info("Socket no longer accessible, processed a run and now connection refused at {}:{}, shutting down.",parameters.fHost,parameters.fPort);
                        return;
                    }
                }
                catch (SocketException e)
                {
                    log.info("Socket no longer accessible at {}:{}, shutting down.",parameters.fHost,parameters.fPort);
                    log.trace("Exception:",e);
                    return;
                }
                catch (IOException e) {
                    log.error("Unknown problem occured, please check that the --ipc-encoding is set to " + IPCTargetAlgorithmEvaluatorOptions.EncodingMechanismOptions.JAVA_SERIALIZATION+".");
                    log.error("Exception:",e);
                    clientSocket.close();
                }
            }
        }
    }
    
    /**
     * Send an algorithm run result through the provided socket.
     * @param aRunResult - algorithm run result.
     * @param aIPCTAEClSocket - socket to use for communication.
     */
    private final static void sendRunResult(AlgorithmRunResult aRunResult, Socket aIPCTAEClSocket) throws IOException
    {
        ObjectOutputStream oout;
        oout = new ObjectOutputStream(aIPCTAEClSocket.getOutputStream());
        oout.writeObject(aRunResult);
      
    }
    
    /**
     * Receive an algorithm run configuration through the provided socket.
     * @param aIPCTAEClientSocket - socket to use for communication.
     * @return an algorithm run configuration read from the socket's input stream.
     */
    private final static AlgorithmRunConfiguration receiveRunConfig(Socket aIPCTAEClientSocket) throws IOException
    {
        ObjectInputStream oin;
        oin = new ObjectInputStream(aIPCTAEClientSocket.getInputStream());
        AlgorithmRunConfiguration runConfig;
        try {
            runConfig = (AlgorithmRunConfiguration) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find class Algorithm Run Configuration.");
        }
        
        return runConfig;
    }
    
}
