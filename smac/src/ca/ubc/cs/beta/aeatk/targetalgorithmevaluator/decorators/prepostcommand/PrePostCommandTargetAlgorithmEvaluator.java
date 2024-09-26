package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.prepostcommand;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Class that handles a pre command and a post command prior to running any algorithms
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class PrePostCommandTargetAlgorithmEvaluator extends	AbstractTargetAlgorithmEvaluatorDecorator {

	private final PrePostCommandOptions options;

	private final static Logger log = LoggerFactory.getLogger(PrePostCommandTargetAlgorithmEvaluator.class);
	
	
	public PrePostCommandTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae, PrePostCommandOptions options) {
		super(tae);
		this.options = options;
		
		runCommand(options.preCommand);
	}
	
	private void runCommand(String command)
	{
		if((command == null) || (command.trim().length() == 0))
		{
			return;
		}
		
		log.info("Running scenario command: cd {} ; {}", options.directory.getAbsolutePath(), command );
		try {
			final ExecutorService execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Pre Post Command Handler"));
			try {
				
				final Process proc = Runtime.getRuntime().exec(command,null, options.directory);
				
				final CountDownLatch cLatch = new CountDownLatch(1 + (options.logOutput ? 1 :0));
				

				Runnable standardErrorReader = new Runnable()
				{
	
					@Override
					public void run() 
					{
						
						try { 
						Scanner procIn = new Scanner(proc.getErrorStream());
						
						while(procIn.hasNext())
						{	
							log.warn("[PROCESS]  {}", procIn.nextLine());
						}
						
						procIn.close();
						} finally
						{
							cLatch.countDown();
						}
						
					}
					
				};
				
				execService.execute(standardErrorReader);
				
				if(options.logOutput)
				{
					Runnable standardOutReader = new Runnable()
					{
		
						@Override
						public void run() {
							
							try { 
							Scanner procIn = new Scanner(proc.getInputStream());
							
							while(procIn.hasNext())
							{	
								log.info("[PROCESS]  {}", procIn.nextLine());
							}
							
							procIn.close();
							} finally
							{
								cLatch.countDown();
							}
							
						}
						
					};
				
					execService.execute(standardOutReader);
				}	
			
				cLatch.await();
				int resultCode = proc.waitFor();
				
				if(resultCode != 0)
				{
					if(options.exceptionOnError)
					{
						throw new PrePostCommandErrorException("Got a non-zero return code ");
					}
					
					log.warn("Got a non-zero return code from process: {}", resultCode);
				}
				
			} finally
			{
				execService.shutdown();
			}
			
			
			
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			log.warn("Pre/Post Command Running was interrupted");
			return;
		} catch (IOException e)
		{ 
			throw new PrePostCommandErrorException("An error occurred while running command: " + command,e);
		}		
	}
	
	@Override
	public void postDecorateeNotifyShutdown()
	{
		
		runCommand(options.postCommand);
	}
	
}
