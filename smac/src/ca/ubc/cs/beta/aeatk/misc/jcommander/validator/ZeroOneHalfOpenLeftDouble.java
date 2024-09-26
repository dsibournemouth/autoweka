package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ZeroOneHalfOpenLeftDouble implements IParameterValidator, DomainDisplay {
	
	public void validate(String name, String value) throws ParameterException 
	{
		    double n = Double.parseDouble(value);
		    
    		if(n <= 0 || n > 1)
    		{
    			throw new ParameterException("Parameter " + name + " must have a value in [0,1) (A value from 0 up to but not including 1)");
    		}
  
		    
		  
	}
	
	 @Override
	  public String getDomain() {
			return "(0, 1]";
	  }
	 

}