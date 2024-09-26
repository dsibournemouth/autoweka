package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;



import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Target Algorithm Evaluator Decorator that checks if duplicate runs are being submitted
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class CheckForDuplicateRunConfigDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	Logger log = LoggerFactory.getLogger(getClass());
	private final boolean throwException;
	
	
	public CheckForDuplicateRunConfigDecorator(
			TargetAlgorithmEvaluator tae, boolean throwException) {
		super(tae);
		this.throwException  = throwException;
	}


	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		checkRunConfigs(runConfigs);
		return tae.evaluateRun(runConfigs,obs);
	}


	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		checkRunConfigs(runConfigs);
		tae.evaluateRunsAsync(runConfigs, handler, obs);
		
	}
	
	public void checkRunConfigs(List<AlgorithmRunConfiguration> runConfigs)
	{
		Set<AlgorithmRunConfiguration> rcs = new HashSet<AlgorithmRunConfiguration>();
		
		rcs.addAll(runConfigs);
		
		if(rcs.size() != runConfigs.size())
		{
			log.error("Duplicate Run Configurations Requested this is almost certainly a bug");
			log.error("Duplicate Run Configs Follow:");
			for(AlgorithmRunConfiguration rc : findDuplicates(runConfigs))
			{
				log.error("\tDuplicate Run Config: {}", rc);
			}
			
			
			log.error("All Run Configs follow:");
			for(AlgorithmRunConfiguration rc : runConfigs)
			{
				log.error("\tRun Config: {} ", rc);
			}


			if(throwException)
			{
				throw new IllegalStateException("Duplicate Run Configurations cannot be part of the same call of evaluateRun()/evaluateRunAsync()");
			}
		}	

	}
	
	private Set<AlgorithmRunConfiguration> findDuplicates(List<AlgorithmRunConfiguration> listContainingDuplicates)
	{ 
	  final Set<AlgorithmRunConfiguration> setToReturn = new HashSet<AlgorithmRunConfiguration>(); 
	  final Set<AlgorithmRunConfiguration> set1 = new HashSet<AlgorithmRunConfiguration>();

	  for (AlgorithmRunConfiguration yourInt : listContainingDuplicates)
	  {
	   if (!set1.add(yourInt))
	   {
	    setToReturn.add(yourInt);
	   }
	  }
	  return setToReturn;
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
	
}
