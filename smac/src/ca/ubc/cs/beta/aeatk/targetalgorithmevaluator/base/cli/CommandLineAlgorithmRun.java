package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.associatedvalue.Pair;
import ca.ubc.cs.beta.aeatk.misc.logback.MarkerFilter;
import ca.ubc.cs.beta.aeatk.misc.logging.LoggingMarker;
import ca.ubc.cs.beta.aeatk.misc.string.SplitQuotedString;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Executes a Target Algorithm Run via Command Line Execution
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class CommandLineAlgorithmRun implements Callable<AlgorithmRunResult>{

	
	private static final long serialVersionUID = -70897405824987641L;
	
	/**
	 * Regex that we hope to match
	 */
	
	
	//maybe merge these one day
	public static final String AUTOMATIC_CONFIGURATOR_RESULT_REGEX = "^\\s*Result\\s*of\\s*(this)?\\s*[Aa]lgorithm\\s*[rR]un\\s*:";
	
	public static final String OLD_AUTOMATIC_CONFIGURATOR_RESULT_REGEX = "^\\s*(Final)?\\s*[Rr]esult\\s+(?:([Ff]or)|([oO]f))\\s+(?:(HAL)|(ParamILS)|(SMAC)|([tT]his [wW]rapper)):";
	/**
	 * Compiled REGEX
	 */
	private static final Pattern pattern = Pattern.compile(AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
	
	private static final Pattern oldPattern = Pattern.compile(OLD_AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
	
	private static transient Logger log = LoggerFactory.getLogger(CommandLineAlgorithmRun.class);
	
	
	private Queue<String> outputQueue = new ArrayDeque<String>(MAX_LINES_TO_SAVE * 2);

	/**
	 * Stores the observer for this run
	 */
	private transient TargetAlgorithmEvaluatorRunObserver runObserver;

	/**
	 * Stores the kill handler for this run
	 */
	private transient KillHandler killHandler;
	
	public static final String PORT_ENVIRONMENT_VARIABLE = "AEATK_PORT";
	public static final String FREQUENCY_ENVIRONMENT_VARIABLE = "AEATK_CPU_TIME_FREQUENCY";
	public static final String CONCURRENT_TASK_ID = "AEATK_CONCURRENT_TASK_ID";
	
	
	
	/**
	 * This variable is public only for unit test purposes,
	 * this is not guaranteed to be the actual environment variable of child processes
	 */
	public static final String EXECUTION_UUID_ENVIRONMENT_VARIABLE_DEFAULT = "AEATK_EXECUTION_UUID"; 
	
	
	/**
	 * Stores a unique UUID for the run, used in environment variables.
	 */
	private final UUID uuid = UUID.randomUUID();
	
	static 
	{
		if(!System.getenv().containsKey(EXECUTION_UUID_ENVIRONMENT_VARIABLE_DEFAULT))
		{
			envVariableForChildren = EXECUTION_UUID_ENVIRONMENT_VARIABLE_DEFAULT;
		} else
		{
			int i=0;
			while( System.getenv().containsKey(EXECUTION_UUID_ENVIRONMENT_VARIABLE_DEFAULT + "_SUB_" + (i++)));
			envVariableForChildren = EXECUTION_UUID_ENVIRONMENT_VARIABLE_DEFAULT + "_SUB_" + (i++);
		}
	}
	
	private static final String envVariableForChildren;
	
	
	/**
	 * Marker for logging
	 */
	private static transient Marker fullProcessOutputMarker = MarkerFactory.getMarker(LoggingMarker.FULL_PROCESS_OUTPUT.name());
	
	public final static String COMMAND_SEPERATOR;

	static {
		log.trace("This version of SMAC hardcodes run length for calls to the target algorithm to {}.", Integer.MAX_VALUE);
		
		if(System.getProperty("os.name").toLowerCase().contains("win"))
		{
			COMMAND_SEPERATOR = "&";
		} else
		{
			COMMAND_SEPERATOR = ";";
		}
		
	}
	
	//private transient
	
	private final int observerFrequency;
	
	private AtomicBoolean processEnded = new AtomicBoolean(false);
	
	private transient final BlockingQueue<Integer> executionIDs;
	
	
	/**
	 * This field is transient because we can't save this object when we serialize.
	 * 
	 * If after restoring serialization you need something from this object, you should
	 * save it as a separate field. (this seems unlikely) 
	 * 
	 */
	private final transient CommandLineTargetAlgorithmEvaluatorOptions options;

	private final AlgorithmRunConfiguration runConfig;
	
	private static transient final AtomicBoolean jvmShutdownDetected = new AtomicBoolean(false);
	
	
	/**
	 * Watch that can be used to time algorithm runs 
	 */
	private	final StopWatch wallClockTimer = new StopWatch();
	
	
	protected void startWallclockTimer()
	{
		wallClockTimer.start();
	}
	
	private AtomicDouble wallClockTime = new AtomicDouble();
	
	protected void stopWallclockTimer()
	{
		wallClockTime.set(wallClockTimer.stop() / 1000.0);
	}
	
	protected long getCurrentWallClockTime()
	{
		return this.wallClockTimer.time();
	}
	
	
	private static final Set<Pair<CommandLineAlgorithmRun, Process>> outstandingRuns  = Collections.newSetFromMap(new ConcurrentHashMap<Pair<CommandLineAlgorithmRun, Process>,Boolean>());
	static
	{
		Thread shutdownThread = new Thread(new Runnable()
		{

			@Override
			public void run() {
				Thread.currentThread().setName("CLI Shutdown Thread");
				jvmShutdownDetected.set(true);
				if(outstandingRuns.size() > 0)
				{
					log.debug("Terminating approximately {} outstanding algorithm runs", outstandingRuns.size());
				}
				log.trace("Further runs will be instantly terminated");
				for(Pair<CommandLineAlgorithmRun, Process> p : outstandingRuns)
				{
					p.getFirst().killProcess(p.getSecond());
				}
			}
			
		});
		
		log.trace("Shutdown hook to terminate all outstanding runs enabled");
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}
	
	
	
	private volatile AlgorithmRunResult completedAlgorithmRun;
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param executionIDs 
	 */ 
	public CommandLineAlgorithmRun( AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluatorRunObserver obs, KillHandler handler, CommandLineTargetAlgorithmEvaluatorOptions options, BlockingQueue<Integer> executionIDs) 
	{
		//super( runConfig);
	

		
		this.runConfig = runConfig;
		this.runObserver = obs;
		this.killHandler = handler;
		this.observerFrequency = options.observerFrequency;
		
		if(observerFrequency < 25)
		{
			throw new IllegalArgumentException("Observer Frequency can't be less than 25 milliseconds");
		}
		
		this.options = options;

		this.executionIDs = executionIDs;
	}
	
	private static final int MAX_LINES_TO_SAVE = 1000;

	private volatile boolean wasKilled = false;
	
	@Override
	public synchronized AlgorithmRunResult call() 
	{
		
		Thread.currentThread().setName("CLI TAE (Master Thread - TBD)");
		if(killHandler.isKilled())
		{
			
			log.trace("Run has already been toggled as killed {}", runConfig);
			
			RunStatus rr = RunStatus.KILLED;
			
			AlgorithmRunResult run = new ExistingAlgorithmRunResult(runConfig, rr, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), "",0);
			try {
				runObserver.currentStatus(Collections.singletonList((AlgorithmRunResult) run));
			} catch(RuntimeException t)
			{
				log.error("Error occured while notify observer ", t);
				throw t;
			}
			
			return run;
			
		}
		
		
		//Notify observer first to trigger kill handler
		runObserver.currentStatus(Collections.singletonList((AlgorithmRunResult) new RunningAlgorithmRunResult(runConfig,  0,  0,0, runConfig.getProblemInstanceSeedPair().getSeed(), 0, killHandler)));
		
		
		if(jvmShutdownDetected.get())
		{
			
			String rawResultLine = "JVM Shutdown Detected";
			
			AlgorithmRunResult run = new ExistingAlgorithmRunResult(runConfig, RunStatus.KILLED, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(),"JVM Shutdown Detected, algorithm not executed",0);
			runObserver.currentStatus(Collections.singletonList(run));
			return run;
		}
		
		if(killHandler.isKilled())
		{
			
			log.trace("Run was killed", runConfig);
			String rawResultLine = "Kill detected before target algorithm invoked";
			
			AlgorithmRunResult run = new ExistingAlgorithmRunResult(runConfig,RunStatus.KILLED, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), "Kill detected before target algorithm invoked", 0);
			runObserver.currentStatus(Collections.singletonList(run));
			return run;
		}
		
		final Process proc;
		
		File execDir = new File(runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory());
		if(!execDir.exists()) 
		{
			throw new TargetAlgorithmAbortException("Algorithm Execution Directory: " + runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory() + " does not exist");
		}
		
		if(!execDir.isDirectory()) 
		{
			throw new TargetAlgorithmAbortException("Algorithm Execution Directory: " + runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory() + " is not a directory");
		}
		
		try 
		{
			Integer token;
			try 
			{
				token = executionIDs.take();
			} catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				AlgorithmRunResult run = ExistingAlgorithmRunResult.getAbortResult(runConfig, "Target CLI Thread was Interrupted");
				return run;
			}
			
			final Integer myToken = token;
			Thread.currentThread().setName("CLI TAE (Master Thread - #" + myToken +")" );
			
			try 
			{

				//Check kill handler again
				if(killHandler.isKilled())
				{
					log.trace("Run was killed", runConfig);
					
					AlgorithmRunResult run = new ExistingAlgorithmRunResult(runConfig, RunStatus.KILLED, 0, 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(),"Kill detected before target algorithm invoked",0);
					runObserver.currentStatus(Collections.singletonList(run));;
					return run;
				}
				
				int port = 0;
				final DatagramSocket serverSocket;
				if(options.listenForUpdates)
				{
					serverSocket = new DatagramSocket();
					port = serverSocket.getLocalPort();
				} else
				{
					serverSocket = null;
				}
				
				final AtomicDouble currentRuntime = new AtomicDouble(0);
				
				Runnable socketThread = new Runnable()
				{
					@Override
					public void run()
					{
						
						Thread.currentThread().setName("CLI TAE (Socket Thread - #"+myToken+")" );
						
						byte[] receiveData = new byte[1024];
						
						while(true)
						{
							try 
							{
								DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
								
								
								serverSocket.receive(receivePacket);
								
								InetAddress IPAddress = receivePacket.getAddress();
					               
								if (!InetAddress.getByName("localhost").equals(IPAddress))
								{
									log.warn("Received Request from Non-localhost, ignoring request from: {}", IPAddress.getHostAddress());
									continue;
								}
				               
				               Double runtime = Double.valueOf(new String(receivePacket.getData()));
				              
				               currentRuntime.set(runtime);
				               
							} catch(RuntimeException e)
							{
								log.trace("Got some runtime exception while processing data packet", e);
							} catch(SocketException e)
							{
								//Don't log this since this socket exception is what we
								//we expect since most of the time we will be blocked on the socket
								//when we are shutdown and interrupted.
								return;
							} catch (IOException e) {
								log.warn("Unknown IOException occurred ", e);
							}
							
						}
					}
					
					
				};
				
				this.startWallclockTimer();
				proc = runProcess(port, token);
				
				try 
				{
				outstandingRuns.add(new Pair<CommandLineAlgorithmRun, Process>(this, proc));

				final Process innerProcess = proc; 
				
				
				final Semaphore stdErrorDone = new Semaphore(0);
	
				
				Runnable standardErrorReader = new Runnable()
				{
	
					@Override
					public void run() {
						
						Thread.currentThread().setName("CLI TAE (STDERR Thread - #" + myToken + ")");
						try {
							try { 
								try (BufferedReader procIn = new BufferedReader(new InputStreamReader(innerProcess.getErrorStream())))
								{
									do{
										
										String line;
										boolean read = false;
										while(procIn.ready())
										{
											read = true;
											line = procIn.readLine();
											
											if(line == null)
											{
												
												return;
											}
											log.warn("[PROCESS-ERR]  {}", line);
											
										}
									
										
										if(!read)
										{
											Thread.sleep(50);
										}
										
									} while(!processEnded.get());
									
									
									StringBuilder sb = new StringBuilder();
									
									//In case something else has come in
									if(procIn.ready())
									{
										//Probably not the most efficient way to read
										char[] input = new char[10000];
										procIn.read(input);
										sb.append(String.valueOf(input));
										
									}
									
									if(sb.toString().trim().length() > 0)
									{
										log.warn("[PROCESS-ERR] {}", sb.toString().trim());
									}
								}
							} finally
							{
								
								stdErrorDone.release();
								log.trace("Standard Error Done");
							}
						} catch(InterruptedException e)
						{
							Thread.currentThread().interrupt();
							return;
						} catch(IOException e)
						{
							log.warn("Unexpected IOException occurred {}",e);
						}
						
						
					}
					
				};
					
			
				Runnable observerThread = new Runnable()
				{
	
					@Override
					public void run() {
						Thread.currentThread().setName("CLI TAE (Observer Thread - #" + myToken+ ")");
	
						while(true)
						{
						
							double currentTime = getCurrentWallClockTime() / 1000.0;
							
							runObserver.currentStatus(Collections.singletonList((AlgorithmRunResult) new RunningAlgorithmRunResult(runConfig,  Math.max(0,currentRuntime.get()),  0,0, runConfig.getProblemInstanceSeedPair().getSeed(), currentTime, killHandler)));
							try {
								
								
								
								//Sleep here so that maybe anything that wanted us dead will have gotten to the killHandler
								Thread.sleep(25);
								if(killHandler.isKilled())
								{
									wasKilled = true;
									log.trace("Trying to kill run: {} latest time: {} " , runConfig, currentRuntime.get());


									killProcess(proc);
									return;
								}
								Thread.sleep(observerFrequency - 25);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								break;
							}
							
						}
						
						
					}
					
				};
				
				ExecutorService threadPoolExecutor = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Command Line Target Algorithm Evaluator Thread "));
				try 
				{
					if(options.listenForUpdates)
					{
						threadPoolExecutor.execute(socketThread);
					}
					threadPoolExecutor.execute(observerThread);
					threadPoolExecutor.execute(standardErrorReader);
					BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					
					//Scanner procIn = new Scanner(proc.getInputStream());
					
					
					 
					if(jvmShutdownDetected.get())
					{ //Possible that this run started after the shutdown call was flagged, but before we put it in the map.
					
						killProcess(proc);
					}
					try
					{
						processRunLoop(read,proc);
					} finally
					{
						killProcess(proc);
					}
					
					if(completedAlgorithmRun == null)
					{
						if(wasKilled)
						{
							double currentTime = Math.max(0,currentRuntime.get());
							completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, RunStatus.KILLED, currentTime, 0,0, runConfig.getProblemInstanceSeedPair().getSeed(), "Killed Manually", this.getCurrentWallClockTime() / 1000.0 );
							
						} else if(jvmShutdownDetected.get())
						{
							double currentTime = Math.max(0,currentRuntime.get());
							completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig,  RunStatus.KILLED, currentTime, 0,0, runConfig.getProblemInstanceSeedPair().getSeed(), "JVM Shutdown Detected", this.getCurrentWallClockTime()  / 1000.0);							
						} else
						{
							double currentTime = Math.max(0,currentRuntime.get());
							completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, RunStatus.CRASHED, currentTime, 0,0, runConfig.getProblemInstanceSeedPair().getSeed(), "ERROR: Wrapper did not output anything that matched the expected output (\"Result of algorithm run:...\"). Please try executing the wrapper directly", this.getCurrentWallClockTime() / 1000.0);
						}
					}
					
					
					switch(completedAlgorithmRun.getRunStatus())
					{
						case ABORT:
						case CRASHED:

						
								log.error("The following algorithm call failed: cd \"{}\" " + COMMAND_SEPERATOR + "  {} ",new File(runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString( runConfig));
							
								if(outputQueue.size() > 0)
								{
									log.error("The last {} lines of output we saw were:", outputQueue.size());
									
									for(String s : outputQueue)
									{
										log.error("> "+s);
									}
								} else
								{
									log.debug("No output on standard out detected");
								}
								
								
							
						default:
							//Doesn't matter
						
					}
					
					outputQueue.clear();

					read.close();

					stdErrorDone.acquireUninterruptibly();
				} finally
				{
					//Close the listening socket
					if(serverSocket != null)
					{
						serverSocket.close();
					}
					threadPoolExecutor.shutdownNow();
					try {
						threadPoolExecutor.awaitTermination(24, TimeUnit.HOURS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

				} finally
				{
					if(proc != null)
					{
						proc.destroy();
					}
					
				}
				
				runObserver.currentStatus(Collections.singletonList(completedAlgorithmRun));
				log.debug("Run {} is completed", completedAlgorithmRun);
				
			} finally
			{
				if(!executionIDs.offer(token))
				{
					log.error("Developer Error: Couldn't offer run token back to pool, which violates an invariant. We will essentially block until it is accepted.");
					try {
						executionIDs.put(token);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				
				
			}

			return completedAlgorithmRun;
			
		} catch (IOException e1) 
		{

			//String execCmd = getTargetAlgorithmExecutionCommandAsString(execConfig,runConfig);
			log.error( "The following algorithm call failed: cd \"{}\" " + COMMAND_SEPERATOR + "  {} ",new File(runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString( runConfig));

			throw new TargetAlgorithmAbortException(e1);
			//throw new IllegalStateException(e1);
		}
	}
	
		
	/**
	 * Processes all the output of the target algorithm
	 * 
	 * Takes a line from the input and tries to parse it 
	 * 
	 * @param procIn Scanner of processes output stream
	 */
	public void processRunLoop(BufferedReader procIn, Process p)
	{
		
		int i=0; 
			try {
				boolean matchFound = false;
				try
				{
outerloop:		
					do{
					
						
						String line;
						boolean read = false;
						//TODO This ready call doesn't guarantee we can read a line
						
						while(procIn.ready())
						{
							read = true;
							line = procIn.readLine();
							
							if(line == null)
							{
								log.trace("Process has ended");
								processEnded.set(true);
								break outerloop;
							}
							outputQueue.add(line);
							if (outputQueue.size() > MAX_LINES_TO_SAVE)
							{
								outputQueue.poll();
							}
							
						
							
							if(wasKilled)
							{
								continue;
							}
							boolean matched = processLine(line);
							
							
							if(matched && matchFound)
							{
								log.error("Second output of matching line detected, there is a problem with your wrapper. You can try turning with log all process output enabled to debug: {} ", line);
								completedAlgorithmRun = ExistingAlgorithmRunResult.getAbortResult(runConfig, "duplicate lines matched");
								continue;
							}
							matchFound = matchFound | matched; 
						}
						
						if(completedAlgorithmRun != null && wasKilled)
						{
							if(completedAlgorithmRun.getWallclockExecutionTime() > 1)
							{ //For very short runs this might not matter.
								log.warn("Run was killed but we somehow completed this might be a race condition but our result is: {}. This is a warning just so that developers can see this having occurred and judge the correctness" ,completedAlgorithmRun.getResultLine());
							}
						}
						
						
						
						if(!procIn.ready() && exited(p))
						{
							//I assume that if the stream isn't ready and the process has exited that 
							//we have processed everything
							processEnded.set(true);
							break;
						}
						
						if(!read)
						{
							if(++i % 12000 == 0)
							{
								log.trace("Slept for 5 minutes waiting for pid {}  &&  (matching line found?: {} ) " ,getPID(p), matchFound);
							}
							Thread.sleep(25);
						}
						
					} while(!processEnded.get());
				} finally
				{
					procIn.close();
				} 
			} catch (IOException e) {
				
				if(!processEnded.get())
				{
					log.trace("IO Exception occurred while processing runs");
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			
			}
		
		
	}
	
	

	/**
	 * Starts the target algorithm
	 * @param token 
	 * @return Process reference to the executiong process
	 * @throws IOException
	 */
	private  Process runProcess(int port, Integer token) throws IOException
	{
		String[] execCmdArray = getTargetAlgorithmExecutionCommand(runConfig);
		
		
		if(options.logAllCallStrings())
		{
			//log.info( "Call (with token {}) : cd \"{}\" " + COMMAND_SEPERATOR + "  {} ", token, new File(runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory()).getAbsolutePath(), getTargetAlgorithmExecutionCommandAsString( runConfig));
		}
		
		
		ArrayList<String> envpList = new ArrayList<String>(System.getenv().size());
		for(Entry<String, String> ent : System.getenv().entrySet())
		{
			envpList.add(ent.getKey() + "=" + ent.getValue());
		}
		
		if(options.listenForUpdates)
		{
			envpList.add(PORT_ENVIRONMENT_VARIABLE  + "=" + port);
			envpList.add(FREQUENCY_ENVIRONMENT_VARIABLE + "=" + (this.observerFrequency / 2000.0));
			
		}
		
		envpList.add(CONCURRENT_TASK_ID + "=" + token);
		envpList.add(envVariableForChildren + "=" + uuid.toString());  
		String[] envp = envpList.toArray(new String[0]);

		Process proc = Runtime.getRuntime().exec(execCmdArray,envp, new File(runConfig.getAlgorithmExecutionConfiguration().getAlgorithmExecutionDirectory()));

		//log.debug("Process for {} started with pid: {} (Environment Variable: {})", this.runConfig, getPID(proc), uuid);

		return proc;
	}
	
	
	/**
	 * Gets the execution command string
	 * @return string containing command
	 */

	private String[] getTargetAlgorithmExecutionCommand( AlgorithmRunConfiguration runConfig)
	{

		AlgorithmExecutionConfiguration execConfig = runConfig.getAlgorithmExecutionConfiguration();
				
		String cmd = execConfig.getAlgorithmExecutable();
		//cmd = cmd.replace(AlgorithmExecutionConfiguration.MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX,"");
		
		
		String[] execCmdArray = SplitQuotedString.splitQuotedString(cmd);
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(execCmdArray));
		list.add(runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceName());
		list.add(runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation());
		list.add(String.valueOf(runConfig.getCutoffTime()));
		list.add(String.valueOf(Integer.MAX_VALUE));
		list.add(String.valueOf(runConfig.getProblemInstanceSeedPair().getSeed()));
		
		ParameterStringFormat f = ParameterStringFormat.NODB_SYNTAX;
		
		final String valueDelimiter = (options.paramArgumentsContainQuotes) ?  f.getValueDelimeter() : "";
		
		for(String key : runConfig.getParameterConfiguration().getActiveParameters() )
		{
			if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
			{
				throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
			}
			list.add(f.getPreKey() + key);
			
			
			list.add(valueDelimiter + runConfig.getParameterConfiguration().get(key)  + valueDelimiter);	
			
		}
		
		
		//execString.append(cmd).append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append();
		
		return list.toArray(new String[0]);
	}
	
	/**
	 * Gets the execution command string
	 * @return string containing command
	 */
	public static String getTargetAlgorithmExecutionCommandAsString( AlgorithmRunConfiguration runConfig)
	{

				
		AlgorithmExecutionConfiguration execConfig = runConfig.getAlgorithmExecutionConfiguration();
		String cmd = execConfig.getAlgorithmExecutable();
		//cmd = cmd.replace(AlgorithmExecutionConfiguration.MAGIC_VALUE_ALGORITHM_EXECUTABLE_PREFIX,"");
		
		
		String[] execCmdArray = SplitQuotedString.splitQuotedString(cmd);
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(execCmdArray));
		list.add(runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceName());
		list.add(runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation());
		list.add(String.valueOf(runConfig.getCutoffTime()));
		list.add(String.valueOf(Integer.MAX_VALUE));
		list.add(String.valueOf(runConfig.getProblemInstanceSeedPair().getSeed()));
		
		ParameterStringFormat f = ParameterStringFormat.NODB_SYNTAX;
		for(String key : runConfig.getParameterConfiguration().getActiveParameters()  )
		{
			
			
			if(!f.getKeyValueSeperator().equals(" ") || !f.getGlue().equals(" "))
			{
				throw new IllegalStateException("Key Value seperator or glue is not a space, and this means the way we handle this logic won't work currently");
			}
			list.add(f.getPreKey() + key);
			list.add(f.getValueDelimeter() + runConfig.getParameterConfiguration().get(key)  + f.getValueDelimeter());	
			
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
		
		
		//execString.append(cmd).append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append();
		
		return sb.toString();
	}


	
	
	/**
	 *	Process a single line of the output looking for a matching line (e.g. Result of algorithm run: ...)
	 *	@param line of program output
	 */
	public boolean processLine(String line)
	{
		Matcher matcher = pattern.matcher(line);
		
		Matcher matcher2 = oldPattern.matcher(line);
		String rawResultLine = "[No Matching Output Found]";
		
		if(options.logAllProcessOutput)
		{
			log.info("[PROCESS] {}" ,line);
		}
		

		if (matcher.find() || matcher2.find())
		{
		
			if(options.logAllCallResults() && !options.logAllProcessOutput)
			{
				log.info("[PROCESS] {}", line);
			}
			
			String fullLine = line.trim();
			String additionalRunData = "";
			try
			{
			
				String acExecResultString = line.substring(line.indexOf(":")+1).trim();
				
				String[] results = acExecResultString.split(",");
				for(int i=0; i < results.length; i++)
				{
					results[i] = results[i].trim();
				}
				
				rawResultLine = acExecResultString;
				
				RunStatus acResult =  RunStatus.getAutomaticConfiguratorResultForKey(results[0]);
				
				if(!acResult.permittedByWrappers())
				{
					throw new IllegalArgumentException(" The Run Result reported is NOT permitted to be output by a wrapper and is for internal SMAC use only.");
				}
				
					
					
				String runtime = results[1].trim();
				String runLength = results[2].trim();
				String bestSolution = results[3].trim();
				String seed = results[4].trim();
				if(results.length <= 5)
				{ //This is a good case

				} else if(results.length == 6)
				{
					additionalRunData = results[5].trim();
				} else
				{
					log.warn("Too many fields were encounted (expected 5 or 6) when parsing line (Additional Run Data cannot have commas): {}\n ",line);
				}
				
				double runLengthD = Double.valueOf(runLength);
				double runtimeD = Double.valueOf(runtime);
				double qualityD = Double.valueOf(bestSolution);
				long resultSeedD = Long.valueOf(seed);
				if(!MarkerFilter.log(fullProcessOutputMarker.getName()))
				{
					log.info("Algorithm Reported: {}" , line);
				}
				
				completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, acResult, runtimeD, runLengthD, qualityD, resultSeedD,  additionalRunData, this.getCurrentWallClockTime() / 1000.0);
				return true;
			} catch(NumberFormatException e)
			{	 //Numeric value is probably at fault
				
				completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, RunStatus.CRASHED, runConfig.getAlgorithmExecutionConfiguration().getAlgorithmMaximumCutoffTime(), 0, 0, 0, "ERROR: Couldn't parse output from wrapper (invalid number format): " + e.getMessage(), this.getCurrentWallClockTime() / 1000.0);
				
				//this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				Object[] args = { getTargetAlgorithmExecutionCommandAsString( runConfig), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely one of the values of runLength, runtime, quality could not be parsed as a Double, or the seed could not be parsed as a valid long", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
				return true;
					
			} catch(IllegalArgumentException e)
			{ 	//The RunResult probably doesn't match anything
				//this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				
				completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, RunStatus.CRASHED, runConfig.getAlgorithmExecutionConfiguration().getAlgorithmMaximumCutoffTime(), 0, 0, 0, "ERROR: Couldn't parse output from wrapper (not enough arguments): " + e.getMessage(), this.getCurrentWallClockTime() / 1000.0);
				
				ArrayList<String> validValues = new ArrayList<String>();
				for(RunStatus r : RunStatus.values())
				{
					if(r.permittedByWrappers())
					{
						validValues.addAll(r.getAliases());
					}
				}
				Collections.sort(validValues);
				
				String[] validArgs = validValues.toArray(new String[0]);
				
				
				Object[] args = { getTargetAlgorithmExecutionCommandAsString( runConfig), fullLine, Arrays.toString(validArgs)};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the Algorithm did not report a result string as one of: {}", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
				return true;
			} catch(ArrayIndexOutOfBoundsException e)
			{	//There aren't enough commas in the output
				
				completedAlgorithmRun = new ExistingAlgorithmRunResult(runConfig, RunStatus.CRASHED, runConfig.getAlgorithmExecutionConfiguration().getAlgorithmMaximumCutoffTime(), 0, 0, 0, "ERROR: Couldn't parse output from wrapper (problem with arguments): " + e.getMessage(), this.getCurrentWallClockTime() / 1000.0);
				
				//this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				Object[] args = { getTargetAlgorithmExecutionCommandAsString(runConfig), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the algorithm did not specify all of the required outputs that is <solved>,<runtime>,<runlength>,<quality>,<seed>", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
				return true;
			}
		}
		
		return false;
		
	}

	
	
	public static int getPID(Process p)
	{
		int pid = 0;
		
		try {
			Field f = p.getClass().getDeclaredField("pid");
			
			f.setAccessible(true);
			pid = Integer.valueOf(f.get(p).toString());
			f.setAccessible(false);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			return -1;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		if(pid > 0)
		{
			return pid;
		} else
		{
			return -1;
		}
	}
	
	public static boolean exited(Process p)
	{
		try 
		{
			p.exitValue();
			return true;
		} catch(IllegalThreadStateException e)
		{
			return false;
		}
	}
	
	private String replacePid(String input, int pid)
	{
		return input.replaceAll("%pid", String.valueOf(pid));
	}
	
	AtomicBoolean killPreviouslyCalled = new AtomicBoolean(false);
	private void killProcess(Process p)
	{
	
		if(killPreviouslyCalled.getAndSet(true))
		{
			return;
		} else
		{
			outstandingRuns.remove(new Pair<CommandLineAlgorithmRun, Process>(this,p));
		}
		
		
		try 
		{
			int pid = getPID(p);
			if(options.pgEnvKillCommand != null && options.pgEnvKillCommand.trim().length() > 1)
			{
				try {
					
					String killEnvCmd = options.pgEnvKillCommand + " " + envVariableForChildren + " " + uuid.toString() + " " + pid; 
					ProcessBuilder pb = new ProcessBuilder();
					
					pb.redirectErrorStream(true);
					pb.command(SplitQuotedString.splitQuotedString(killEnvCmd));
					
					Process p2 = pb.start();
					
					try 
					{
						try(BufferedReader read = new BufferedReader(new InputStreamReader(p2.getInputStream())))
						{
							String line = null;
							
							while((line = read.readLine()) != null)
							{
								log.trace("Kill environment {} output> {}", uuid.toString(), line);
							}
						}
					} finally
					{
						p2.destroy();
						
						try 
						{
							if(p2.waitFor() > 0)
							{
								log.warn("Kill script execution returned non-zero exit status: {} ", p2.exitValue());
							}
						} catch(InterruptedException e)
						{
							Thread.currentThread().interrupt();
							//Continue;
						}
							
					}
				} catch(IOException e)
				{
					
					log.error("Error while executing {} execute Kill Environment Command",e);
					
				}
			} else
			{
				try 
				{
					if(pid > 0)
					{
						String command = replacePid(options.pgNiceKillCommand,pid);
						log.trace("Trying to send SIGTERM to process group id: {} with command \"{}\"", pid,command);
						try {
							
							
							int retValPGroup = executeKillCommand(command);
							
							if(retValPGroup > 0)
							{
								log.trace("SIGTERM to process group failed with error code {}", retValPGroup);
								
								
								int retVal = executeKillCommand(replacePid(options.procNiceKillCommand,pid));
								
								if(retVal > 0)
								{
									Object[] args = {  pid,retVal};
									log.trace("SIGTERM to process id: {} attempted failed with return code {}",args);
								} else
								{
									log.trace("SIGTERM delivered successfully to process id: {}", pid, pid);
								}
							} else
							{
								log.trace("SIGTERM delivered successfully to process group id: {} ", pid);
							}
						} catch (IOException e) {
							log.error("Couldn't SIGTERM process or process group ", e);
						}
						
					
						
						
						int totalSleepTime = 0;
						int currSleepTime = 25;
						while(true)
						{
							
							if(exited(p))
							{
								return;
							}
							
							Thread.sleep(currSleepTime);
							totalSleepTime += currSleepTime;
							currSleepTime *=1.5;
							if(totalSleepTime > 3000)
							{
								break;
							}
							
						}
												
						log.trace("Trying to send SIGKILL to process group id: {}", pid);
						try {
							
							int retVal = executeKillCommand(replacePid(options.pgForceKillCommand,pid));
							
							if(retVal > 0)
							{
								log.trace("SIGKILL to pid: {} attempted failed with return code {}",pid, retVal);
								
								int retVal3 = executeKillCommand(replacePid(options.procForceKillCommand,pid));
								
								if(retVal3 > 0)
								{
									Object[] args = {  pid,retVal};
									log.trace("SIGKILL to process id: {} attempted failed with return code {}",args);
								} else
								{
									log.trace("SIGKILL delivered successfully to process id: {}", pid, pid);
								}
							} else
							{
								log.trace("SIGKILL delivered successfully to pid: {} ", pid);
							}
						} catch (IOException e) {
							log.error("Couldn't SIGKILL process or process group ", e);
							
						}
					
						
						
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				
			}
			
			
			p.destroy();
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("This shouldn't be possible", e);
				
			}
			
			if(p.exitValue() > 0)
			{
				if(p.exitValue() != 143 && p.exitValue() != 137 &&  p.exitValue() != 130) //
				{
					//log.debug("Process with pid {} and {} signaled non-zero exit status: {}", pid, uuid.toString(), p.exitValue() );
				}
			}
		
			
		} finally
		{
			this.processEnded.set(true);
			this.stopWallclockTimer();
			
		}
		
	}
	
	private int executeKillCommand(String command) throws IOException, InterruptedException
	{
		log.trace("Executing termination command: {}");
		ProcessBuilder pb = new ProcessBuilder();
		pb.redirectErrorStream(true);
		pb.command(SplitQuotedString.splitQuotedString(command));
		Process p2 = pb.start();
		
		
		try (BufferedReader read = new BufferedReader(new InputStreamReader(p2.getInputStream())))
		{
			String line = null;
			
			while((line = read.readLine()) != null)
			{
				//log.trace("Kill For environment {}: command \"{}\" output> {}", uuid.toString(), command, line);
			}
		
		}
		
		try 
		{
			return p2.waitFor();
		} finally
		{
			p2.destroy();
		}
		
	}
	
	
	
	
	
}
