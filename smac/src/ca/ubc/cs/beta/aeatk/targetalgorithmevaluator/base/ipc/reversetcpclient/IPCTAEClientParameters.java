package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.reversetcpclient;

import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.logging.LoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters to construct a IPC TAE client object.
 * @author afrechet
 */
@UsageTextField(title="IPC TAE Client",description="Parameters required to start a SATFC IPC TAE responder.")
public class IPCTAEClientParameters extends AbstractOptions{
    
    @Parameter(names = "--ipc-tae-client-host",description = "IP address / DNS name of the IPC TAE host.")
    public String fHost = "localhost";
    
    @Parameter(names = "--ipc-tae-client-port",description = "Port to use to communicate with the IPC TAE host.",required=true)
    public int fPort;
    
    @Parameter(names = "--ipc-tae-client-retry-attempts",description = "Number of times to retry a failing connecting.")
    public int fRetryAttemps = 1800;
    
    
    @ParametersDelegate
    public TargetAlgorithmEvaluatorOptions fTAEOptions = new TargetAlgorithmEvaluatorOptions();
    
    @ParametersDelegate
    public LoggingOptions log = new ConsoleOnlyLoggingOptions();
    
    @ParametersDelegate
    public HelpOptions help = new HelpOptions();
    
    
}
