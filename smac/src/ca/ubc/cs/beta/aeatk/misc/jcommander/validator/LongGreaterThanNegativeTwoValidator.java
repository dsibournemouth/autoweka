package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

public class LongGreaterThanNegativeTwoValidator implements IParameterValidator, DomainDisplay {

	@Override
	public void validate(String name, String value) throws ParameterException {
		try {
		   long n = Long.parseLong(value);
		    if (n < -1) {
		      throw new ParameterException("Parameter " + name
		          + " should be positive (found " + value +")");
		    }
		} catch(NumberFormatException e)
		{
			throw new ParameterException("The value for parameter: (" + name + ") is not a valid long: (" + value + ")");
		}
		 
	}

	
	@Override
	public String getDomain() {
		return "[-1, " + Long.MAX_VALUE+ ")";
	}



}
