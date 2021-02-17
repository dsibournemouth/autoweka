package ca.ubc.cs.beta.aeatk.misc.jcommander.converter;

import java.util.Arrays;

import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class OverallObjectiveConverter implements IStringConverter<OverallObjective> {

	@Override
	public OverallObjective convert(String arg0) {
		try { 
			
		if(arg0.toUpperCase().equals("PAR10"))
		{
			return OverallObjective.MEAN10;
		}
		
		if(arg0.toUpperCase().equals("PAR1000"))
		{
			return OverallObjective.MEAN1000;
		}
		
		return OverallObjective.valueOf(arg0.toUpperCase());
		} catch(IllegalArgumentException e)
		{
			throw new ParameterException("Illegal value specified for Overall Objective ("  + arg0 + "), allowed values are: " + Arrays.toString(OverallObjective.values()));
		}
	}

}
