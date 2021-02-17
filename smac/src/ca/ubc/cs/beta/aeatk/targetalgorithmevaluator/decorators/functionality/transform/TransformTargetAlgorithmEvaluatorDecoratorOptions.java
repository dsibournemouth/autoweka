package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.transform;


import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
/**
 *  Options for the transform target algorithm evaluator
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@UsageTextField(title="Transform Target Algorithm Evaluator Decorator Options", description="This Target Algorithm Evaluator Decorator allows you to transform the response value of the wrapper according to some rules. Expressions that can be used by exp4j (http://www.objecthunter.net/exp4j/), can be specified and will cause the returned runs to be transformed accordingly. The variables in the expression can be S which will be {-1 if the run was UNSAT, 1 if SAT, and 0 otherwise}, R which is the original reported runtime, Q which is the original reported quality, and C which was the requested cutoff time. Care should be taken when transforming values to obey wrapper semantics. If you don't know what you are doing, we recommend that SAT and UNSAT values should be kept in the range between 0 and cutoff, and the TIMEOUT value shouldn't be transformed at all. A very special thanks to the original author Alexandre Fréchette.", level=OptionLevel.ADVANCED)
public class TransformTargetAlgorithmEvaluatorDecoratorOptions extends AbstractOptions{
	/*
	 * Transforms
	 */
	
	@UsageTextField(defaultValues="false.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform", description="Set to true if you'd like to transform the result, if false the other transforms have no effect")
	public boolean transform = false;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--tae-transform-valid-values-only", description="If the transformation of runtime results in a value that is too large, the cutoff time will be returned, and the result changed to TIMEOUT. If the result is too small it will be set to 0")
	public boolean transformValidValuesOnly = true;
	
	//SAT case
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-SAT-runtime",description="Function to apply to an algorithm run's runtime if result is SAT.",validateWith = CalculableTransformValidator.class)
	public String SAT_runtime_transform = "R";
	
	
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-SAT-quality",description="Function to apply to an algorithm run's quality if result is SAT.",validateWith = CalculableTransformValidator.class)
	public String SAT_quality_transform = "Q";
	
	//UNSAT case
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-UNSAT-runtime",description="Function to apply to an algorithm run's runtime if result is UNSAT.",validateWith = CalculableTransformValidator.class)
	public String UNSAT_runtime_transform = "R";
	
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-UNSAT-quality",description="Function to apply to an algorithm run's quality if result is UNSAT.",validateWith = CalculableTransformValidator.class)
	public String UNSAT_quality_transform = "Q";
	
	//TIMEOUT case
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-TIMEOUT-runtime",description="Function to apply to an algorithm run's runtime if result is TIMEOUT.",validateWith = CalculableTransformValidator.class)
	public String TIMEOUT_runtime_transform = "R";
	
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-TIMEOUT-quality",description="Function to apply to an algorithm run's quality if result is TIMEOUT.",validateWith = CalculableTransformValidator.class)
	public String TIMEOUT_quality_transform = "Q";
	
	//Other case
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-other-runtime",description="Function to apply to an algorithm run's runtime if result is not SAT, UNSAT or TIMEOUT.",validateWith = CalculableTransformValidator.class)
	public String other_runtime_transform = "R";
	
	@UsageTextField(defaultValues="Identity transform.", level=OptionLevel.ADVANCED)
	@Parameter(names="--tae-transform-other-quality",description="Function to apply to an algorithm run's quality if result is not SAT, UNSAT or TIMEOUT.",validateWith = CalculableTransformValidator.class)
	public String other_quality_transform = "Q";

	
	
}
