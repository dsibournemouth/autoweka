package ca.ubc.cs.beta.aeatk.state.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class AutoAsMaxConverter implements IStringConverter<Integer> {

	

	@Override
	public Integer convert(String value) {
		if(value.contains("AUTO"))
		{
			return Integer.MAX_VALUE;
		} else
		{
			try {
				return Integer.valueOf(value);
			} catch(NumberFormatException e)
			{
				throw new ParameterException("Couldn't parse " + value + " as integer");
			}
		}
		
		
			
	}

}
