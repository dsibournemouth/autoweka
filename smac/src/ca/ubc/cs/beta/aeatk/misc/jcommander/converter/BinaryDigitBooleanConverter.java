package ca.ubc.cs.beta.aeatk.misc.jcommander.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class BinaryDigitBooleanConverter implements IStringConverter<Boolean>{

	@Override
	public Boolean convert(String arg0) {

		
		if(arg0.trim().toLowerCase().equals("true"))
		{
			return true;
		}
		
		if(arg0.trim().toLowerCase().equals("false"))
		{
			return false;
		}
		
		if(arg0.trim().toLowerCase().equals("1"))
		{
			return true;
		}
		
		if(arg0.trim().toLowerCase().equals("0"))
		{
			return false;
		}
		
		throw new ParameterException("Could not parse boolean" + arg0);
			
	}

}
