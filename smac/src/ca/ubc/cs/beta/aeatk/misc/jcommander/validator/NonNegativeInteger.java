package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class NonNegativeInteger implements IParameterValidator , DomainDisplay{

	  public void validate(String name, String value)
	      throws ParameterException {
	    int n = Integer.parseInt(value);
	    if (n < 0) {
	      throw new ParameterException("Parameter " + name
	          + " should be non-negative (found " + value +")");
	    }
	  }
	 
	  @Override
	  public String getDomain() {

			return "[0, " + Integer.MAX_VALUE + "]";
	  }
	  

	}