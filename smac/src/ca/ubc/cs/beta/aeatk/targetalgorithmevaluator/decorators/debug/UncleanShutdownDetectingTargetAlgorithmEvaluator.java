package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Detects an unclean shutdown, shutdowns where notify shutdown was not called on the TAE.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class UncleanShutdownDetectingTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator 
		{

	private final AtomicLong notifyShutdownInvoked = new AtomicLong(0);
	private static final int MESSAGE_REPEAT = 1;
	private static final int SLEEP_TIME_IN_MS = 0;
	private static final int SLEEP_TIME_BETWEEN_MESSAGES = 0;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final StackTraceElement[] taeCreationStackTrace;
	
	private final static Object stackTracePrintingLock = new Object();

	public UncleanShutdownDetectingTargetAlgorithmEvaluator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
		taeCreationStackTrace = (new Exception()).getStackTrace();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{

			@Override
			public void run() 
			{
				Thread.currentThread().setName("Unclean Target Algorithm Evaluator Shutdown Detector");
				
				long notifies = notifyShutdownInvoked.get();
				if(notifies == 0)
				{
					log.debug("Unclean Shutdown Detected, You must call notifyShutdown() on your TAE. You may have a broken TAE decorator that doesn't forward the notifyShutdown() correctly");
					synchronized(stackTracePrintingLock)
					{
						if(log.isDebugEnabled())
						{
							
							StringBuilder sb = new StringBuilder();
							
							for(StackTraceElement el : taeCreationStackTrace)
							{
								sb.append(el).append("\n");
								
							}
							
							log.debug("Target algorithm that wasn't shutdown was created here:\n{}", sb);
						}
					}
					
					
					
					
				} else if(notifies > 1)
				{
					log.warn("You called notifyShutdown() on your TAE more than once, this seems exceptionally weird");
					
				} else if(notifies < 0)
				{
					log.warn("You seem to have overflowed the counter we use to track the number of calls to notifyShutdown(), well played...");
					
				} else
				{
					return;
				}
					
					
			}
				
				
			
			
		}
		));
		
	}

	@Override
	public void postDecorateeNotifyShutdown()
	{
		//NOOP
	}
	
	@Override
	public void preDecorateeNotifyShutdown()
	{
		notifyShutdownInvoked.incrementAndGet();
	}
}
