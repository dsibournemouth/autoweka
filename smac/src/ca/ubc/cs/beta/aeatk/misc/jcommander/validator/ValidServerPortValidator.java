package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ValidServerPortValidator implements IParameterValidator , DomainDisplay{

	  public void validate(String name, String value)
	      throws ParameterException {
	    int n = Integer.parseInt(value);
	    if ((n < 1) || (n > 65535)){
	      throw new ParameterException("Port specified in " + name
	          + " should be between 1 and 65535 inclusive (found " + value +")");
	    }
	    
	  }
	 
	  @Override
	  public String getDomain() {

			return "[1,65535]";
	  }
	  

	}