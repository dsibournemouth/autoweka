package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.transform;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;
import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;

/**
 * TAE Decorator that allows users to supply arbitrary transforms (e.g. square the run time if SAT) 
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class TransformTargetAlgorithmEvaluatorDecorator extends AbstractForEachRunTargetAlgorithmEvaluatorDecorator {
	
	@SuppressWarnings("unused")
	private final TransformTargetAlgorithmEvaluatorDecoratorOptions options;
	
	//Variables associated to a run we calculate : S run result (SAT=1,UNSAT=-1,other=0), R runtime, Q quality, C cutoff.
	static final String[] variablesnames = {"S","R","Q","C"};
	
	private final Calculable SAT_runtime_calculable;
	private final Calculable SAT_quality_calculable;
	private final Calculable UNSAT_runtime_calculable;
	private final Calculable UNSAT_quality_calculable;
	private final Calculable TIMEOUT_quality_calculable;
	private final Calculable TIMEOUT_runtime_calculable;
	private final Calculable other_runtime_calculable;
	private final Calculable other_quality_calculable;
	
	private final List<Calculable> calculables;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final AtomicBoolean warningLogged = new AtomicBoolean(false);
	
	public TransformTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae,TransformTargetAlgorithmEvaluatorDecoratorOptions options) {
		
		super(tae);
		
		log.debug("Results from the Target Algorithm Evaluator are being transformed");
		//Get the necessary transforms from the options as calculable.
		try
		{
			this.SAT_runtime_calculable = new ExpressionBuilder(options.SAT_runtime_transform).withVariableNames(variablesnames).build();
			this.SAT_quality_calculable = new ExpressionBuilder(options.SAT_quality_transform).withVariableNames(variablesnames).build();
			this.UNSAT_runtime_calculable = new ExpressionBuilder(options.UNSAT_runtime_transform).withVariableNames(variablesnames).build();
			this.UNSAT_quality_calculable = new ExpressionBuilder(options.UNSAT_quality_transform).withVariableNames(variablesnames).build();
			this.TIMEOUT_runtime_calculable = new ExpressionBuilder(options.TIMEOUT_runtime_transform).withVariableNames(variablesnames).build();
			this.TIMEOUT_quality_calculable = new ExpressionBuilder(options.TIMEOUT_quality_transform).withVariableNames(variablesnames).build();
			this.other_runtime_calculable = new ExpressionBuilder(options.other_runtime_transform).withVariableNames(variablesnames).build();
			this.other_quality_calculable = new ExpressionBuilder(options.other_quality_transform).withVariableNames(variablesnames).build();
		}
		catch(UnparsableExpressionException e)
		{
			throw new ParameterException("Provided options contains an uncalculable string ("+e.getMessage()+").");
		} catch (UnknownFunctionException e) {
			throw new ParameterException("Provided options contains a calculable string with an unknown function ("+e.getMessage()+").");
		}
		
		calculables = Arrays.asList(SAT_runtime_calculable,SAT_quality_calculable,
				UNSAT_runtime_calculable,UNSAT_quality_calculable,
				TIMEOUT_quality_calculable,TIMEOUT_runtime_calculable,
				other_runtime_calculable,other_quality_calculable);
		
		this.options = options;
		
	}

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected AlgorithmRunResult processRun(AlgorithmRunResult run)
	{
		//Assign the variables according to the current run.
		int S;
		switch(run.getRunStatus())
		{
			case SAT:
				S=1;
				break;
			case UNSAT:
				S=-1;
				break;
			default:
				S=0;
				break;
		}
		double R = run.getRuntime();
		double Q = run.getQuality();
		double C = run.getAlgorithmRunConfiguration().getCutoffTime();
		
		//Set the variables
		for(Calculable calculable : calculables)
		{
			calculable.setVariable("S",S);
			calculable.setVariable("R",R);
			calculable.setVariable("Q",Q);
			calculable.setVariable("C",C);
		}
		
		//Compute modified values.
		double transformedRuntime;
		double transformed_quality;
		
		Calculable usedCalculable;
		switch(run.getRunStatus())
		{
			case SAT:
				transformedRuntime = SAT_runtime_calculable.calculate();
				transformed_quality = SAT_quality_calculable.calculate();
				usedCalculable = SAT_runtime_calculable;
				break;
			
			case UNSAT:
				transformedRuntime = UNSAT_runtime_calculable.calculate();
				transformed_quality = UNSAT_quality_calculable.calculate();
				usedCalculable = UNSAT_runtime_calculable;
				break;
			
			case TIMEOUT:
				transformedRuntime = TIMEOUT_runtime_calculable.calculate();
				transformed_quality = TIMEOUT_quality_calculable.calculate();
				usedCalculable = TIMEOUT_runtime_calculable;
				break;
				
			default:
				transformedRuntime = other_runtime_calculable.calculate();
				transformed_quality = other_quality_calculable.calculate();
				usedCalculable = other_runtime_calculable;
				break;
		}
		
		
		if(options.transformValidValuesOnly && transformedRuntime >= run.getAlgorithmRunConfiguration().getCutoffTime())
		{
			return new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), RunStatus.TIMEOUT, run.getAlgorithmRunConfiguration().getCutoffTime(), run.getRunLength(), transformed_quality, run.getResultSeed());
			
		} else
		{
			if(options.transformValidValuesOnly)
			{
				if(transformedRuntime < 0)
				{
					if(!warningLogged.getAndSet(true))
					{
						//log.warn("Transformation of Runtime seems to have resulted in a negative value this is illegal and you should ensure that the result is always above zero, original run: {}, transformed runtime: {}, transformation: \"{}\" ", run, transformedRuntime , usedCalculable.getExpression());
					}
					
					transformedRuntime = 0;
				}
				
			}
			return new ExistingAlgorithmRunResult( run.getAlgorithmRunConfiguration(), run.getRunStatus(), transformedRuntime, run.getRunLength(), transformed_quality, run.getResultSeed());
			
		}
	}
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}
