package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc;


import java.io.File;

import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ValidPortValidator;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ValidServerPortValidator;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.CallStringEncodingMechanism;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.EncodingMechanism;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.JavaSerializationEncodingMechanism;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;

@UsageTextField(title="Inter-Process Communication Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator hands the requests off to another process. The current encoding mechanism is the same as on the command line, except that we do not specify the algo executable field. The current mechanism can only execute one request to the server at a time. A small code change would be required to handle the more general case, so please contact the developers if this is required. ", level=OptionLevel.ADVANCED)
public class IPCTargetAlgorithmEvaluatorOptions extends AbstractOptions {


	@Parameter(names="--ipc-report-persistent", description="Whether the TAE should be treated as persistent, loosely a TAE is persistent if we could ask it for the same request later and it wouldn't have to redo the work from scratch.")
	public boolean persistent;
	
	@Parameter(names="--ipc-mechanism", description="Mechanism to use for IPC")
	public IPCMechanism ipcMechanism = IPCMechanism.UDP;
	
	@Parameter(names="--ipc-encoding", description="How the message is encoded")
	public EncodingMechanismOptions encodingMechanism = EncodingMechanismOptions.CALL_STRING;
	
	@Parameter(names="--ipc-remote-host", description="Remote Host for some kinds of IPC mechanisms")
	public String remoteHost = "127.0.0.1";
	
	@Parameter(names="--ipc-remote-port", description="Remote Port for some kinds of IPC mechanisms", validateWith=ValidPortValidator.class)
	public int remotePort = 5050;
	
	@Parameter(names="--ipc-udp-packetsize", description="Remote Port for some kinds of IPC mechanisms", validateWith=ValidPortValidator.class)
	public int udpPacketSize = 4096;

	@Parameter(names="--ipc-local-port", description="Local server port for some kinds of IPC mechanisms (if 0, this will be automatically allocated by the operating system)", validateWith=ValidServerPortValidator.class)
	public int localPort = 0;
	
	@Parameter(names={"--ipc-exec-on-start-up","--ipc-exec"}, description="This script will be executed on start up of the IPC TAE. A final argument will be appended which is the server port if our IPCMechanism is REVERSE_TCP")
	public String execScript;

	@Parameter(names="--ipc-exec-output", description="If true we will log all output from the script")
	public boolean execScriptOutput;
	
	
	@Parameter(names="--ipc-reverse-tcp-pool-connections", description="If true we will pool all the connections instead of closing them")
	public boolean poolConnections;
	
	@UsageTextField(defaultValues="~/.aeatk/ipc-tae.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--ipc-default-file"}, description="file that contains default settings for IPC Target Algorithm Evaluator (it is recommended that you use this file to set the kill commands)")
	@ParameterFile(ignoreFileNotExists = true) 
	public File ipcDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "ipc-tae.opt");
	
	@UsageTextField(defaultValues="One more than the number of available processors", level=OptionLevel.ADVANCED)
	@Parameter(names="--ipc-async-threads", description="Number of asynchronous threads to use ")
	public int asyncThreads = Runtime.getRuntime().availableProcessors() + 1;
	
	public enum IPCMechanism 
	{
		UDP,
		TCP,
		REVERSE_TCP
	}
	
	
	public enum EncodingMechanismOptions
	{
		CALL_STRING(CallStringEncodingMechanism.class),
		JAVA_SERIALIZATION(JavaSerializationEncodingMechanism.class);
		
		private Class<?> cls;
		EncodingMechanismOptions(Class<?> cls)
		{
			this.cls = cls;
		}
		
		public EncodingMechanism getEncoder()
		{
			
			try {
				return (EncodingMechanism) cls.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException("Couldn't create new instance of serializer (" + this.name() + ")",e);
			}
		}
	}
	
	private static final long serialVersionUID = -7900348544680161087L;

}
