package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

public class TAEValidator implements DomainDisplay, IParameterValidator {

	@Override
	public String getDomain() {

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(String tae : TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators().keySet())
		{
			sb.append(tae);
			sb.append(", ");
		}
		sb.setCharAt(sb.length()-2, '}');
		return sb.toString();
	}

	@Override
	public void validate(String name, String value) throws ParameterException {
		//We don't do anything here actually, we are just doing this for the domain display 
		return; 
		
	}


}
