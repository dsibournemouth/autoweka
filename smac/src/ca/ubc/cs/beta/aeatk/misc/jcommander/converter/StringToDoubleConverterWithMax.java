package ca.ubc.cs.beta.aeatk.misc.jcommander.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class StringToDoubleConverterWithMax implements IStringConverter<Double>{

	@Override
	public Double convert(String arg0) {

		try{
			return Double.valueOf(arg0);
		} catch(NumberFormatException e)
		{
			if(arg0.trim().toUpperCase().equals("MAX"))
			{
				return Double.MAX_VALUE;
			} else
			{
				throw new ParameterException("Could not parse as double " + arg0);
			}
		
		}
	}

}
