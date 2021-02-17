package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.transform;



import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;

public class CalculableTransformValidator implements IParameterValidator, DomainDisplay {

	@Override
	public String getDomain() {
		return "Calculable string using a run's associated variables: S run result (SAT=1,UNSAT=-1,other=0), R runtime, Q quality, C cutoff.";
	}

	@Override
	public void validate(String name, String value) throws ParameterException {
		
		Calculable calculable;
		try
		{
			calculable = new ExpressionBuilder(value).withVariableNames(TransformTargetAlgorithmEvaluatorDecorator.variablesnames).build();
			
		}catch(Exception e)
		{
			throw new ParameterException("Provided expression "+value+" for parameter "+name+" is not calculable ("+e.getMessage()+").");
		}
		
		//Try to build the calculable with some dummy variables.
		try
		{
			for(String variable : TransformTargetAlgorithmEvaluatorDecorator.variablesnames)
			{
				calculable.setVariable(variable, 1);
			}
			calculable.calculate();
		}
		catch(Exception e)
		{
			
			throw new ParameterException("Provided transform "+value+" for parameter "+name+" failed the evaluation step with dummy variables. It is almost certainly not calculable ("+e.getMessage()+").");
		}
		
	}

}
