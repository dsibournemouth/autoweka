package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * TargetAlgorithmEvaluatorDecorator that effectively drops every request after a shutdown has been triggered.
 * 
 * This is recommended because if a long running shutdown hook exists, the application can keep processing results
 * and internally the TAEs may be doing cleanup and hand back unexpected results like KILLED or CRASHED. So instead
 * we simply drop the results here
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class JVMShutdownBlockerTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	AtomicBoolean jvmShutdownDetected = new AtomicBoolean(false);
	
	AtomicBoolean jvmShutdownNoticeLogged = new AtomicBoolean(false);

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public JVMShutdownBlockerTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae) {
		super(tae);
		
		Thread shutdownHookDetection = new Thread(new Runnable()
		{

			@Override
			public void run() {
				jvmShutdownDetected.set(true);
			}
			
		});
		
	
		Runtime.getRuntime().addShutdownHook(shutdownHookDetection);
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		
	}
	
	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer) {
		
		if(jvmShutdownDetected.get())
		{
			if(jvmShutdownNoticeLogged.compareAndSet(false, true))
			{
				log.debug("JVM Shutdown detected, all algorithm execution requests and completion results are being dropped");
			}
			
			return;
		}
		
		TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				
				if(jvmShutdownDetected.get()) 
				{
					if(jvmShutdownNoticeLogged.compareAndSet(false, true))
					{
						log.debug("JVM Shutdown detected, all algorithm execution requests and completion results are being dropped");
					}
					return;
				}
					
				callback.onSuccess(runs);
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onFailure(RuntimeException e) {
				if(jvmShutdownDetected.get()) 
				{
					if(jvmShutdownNoticeLogged.compareAndSet(false, true))
					{
						log.debug("JVM Shutdown detected, all algorithm execution requests and completion results are being dropped");
					}
					
					return;
				}
				callback.onFailure(e);
				
			}
			
		};
		
		
		
		TargetAlgorithmEvaluatorRunObserver taeObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
				if(jvmShutdownDetected.get())
				{
					if(jvmShutdownNoticeLogged.compareAndSet(false, true))
					{
						log.debug("JVM Shutdown detected, all algorithm execution requests and completion results are being dropped");
					}
					
					return;
				}
				observer.currentStatus(runs);
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, taeCallback, (observer != null ? taeObs : null) );
		
		
	}

	
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}
	
	
	

}
