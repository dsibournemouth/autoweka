package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.analytic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;

public class AnalyticTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator implements
		TargetAlgorithmEvaluator {

	private final AnalyticFunctions func;

	private final Logger log = LoggerFactory.getLogger(getClass());
	public AnalyticTargetAlgorithmEvaluator( AnalyticFunctions func) {
		super();
		this.func = func;
	}

	@Override
	public boolean isRunFinal() {
		return true;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

	@Override
	protected void subtypeShutdown() {
		log.info("Global minima for Analytical Target Algorithm Evaluator Function: {} are near {}", func.name() ,  func.getMinima());
	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorRunObserver obs) {
		try{
			
			List<AlgorithmRunResult> ar = new ArrayList<AlgorithmRunResult>(runConfigs.size());
			
			for(AlgorithmRunConfiguration rc : runConfigs)
			{ 
				
				List<Double> vals = new ArrayList<Double>();
				
				for(int i=0; i < 1000; i++)
				{
					if(rc.getParameterConfiguration().containsKey("x" + i))
					{
						vals.add(Double.valueOf(rc.getParameterConfiguration().get("x" + i)));
					}
				}
				
				
				double time = func.evaluate(vals);
				
				for(String key : rc.getParameterConfiguration().keySet())
				{
					if(key.matches("x[0-9]+"))
					{
						continue;
					}
					
					Calculable calc = new ExpressionBuilder(key).withVariable("X", Double.valueOf(rc.getParameterConfiguration().get(key))).build();
					time+=calc.calculate();
				}
				
				String instInfo = rc.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation();
				if(instInfo.startsWith("Analytic-Instance-Cost:"))
				{
					try {
						time += Double.valueOf(instInfo.replace("Analytic-Instance-Cost:", ""));
					} catch(NumberFormatException e)
					{
						throw new NumberFormatException("Couldn't parse analytic instance cost from instance: " + rc.getProblemInstanceSeedPair().getProblemInstance());
					}
				}
								
				if(time >= rc.getCutoffTime())
				{
					ar.add(new ExistingAlgorithmRunResult(rc, RunStatus.TIMEOUT,  rc.getCutoffTime() ,-1,0, rc.getProblemInstanceSeedPair().getSeed()));
				} else
				{
					ar.add(new ExistingAlgorithmRunResult(rc, RunStatus.SAT,  time ,-1,0, rc.getProblemInstanceSeedPair().getSeed()));
				}
				this.runCount.incrementAndGet();
			}
			return ar;
		}
		catch(RuntimeException e){
			
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		} catch (UnknownFunctionException e) {
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		} catch (UnparsableExpressionException e) {
			throw new TargetAlgorithmAbortException("Error while evaluating function", e);
		}
	}
	

}
