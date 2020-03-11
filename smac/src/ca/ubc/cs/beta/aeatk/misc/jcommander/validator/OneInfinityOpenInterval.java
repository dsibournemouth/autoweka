package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class OneInfinityOpenInterval implements IParameterValidator, DomainDisplay {

	  public void validate(String name, String value)
	      throws ParameterException {
	    double n = Double.parseDouble(value);
	    if (n <= 1) {
	      throw new ParameterException("Parameter " + name
	          + " should be greater than one (found " + value +")");
	    }
	  }
	  
	  @Override
	  public String getDomain() {
			return "(1, " + Double.POSITIVE_INFINITY + ")";
	  }

	}