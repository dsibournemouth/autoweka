package ca.ubc.cs.beta.aeatk.misc.jcommander.converter;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class WritableDirectoryConverter implements IStringConverter<File> {

	  public File convert(String value) {
		File f = new File(value);
		
		if (!f.isDirectory())
		{
			throw new ParameterException(value + " is not a valid Directory ");
		}
		
		if (!f.canRead())
		{
			throw new ParameterException(value + " is not readable");
		}
		
		if(!f.canWrite())
		{
			throw new ParameterException(value + " is not writable");
		}
		
		return f;
	     
	  }

	}
