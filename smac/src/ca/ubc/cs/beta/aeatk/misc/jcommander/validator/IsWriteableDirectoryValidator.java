package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import java.io.File;

import ca.ubc.cs.beta.aeatk.misc.options.DomainDisplay;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * JCommander Validator that determines whether or not a directory is writable
 * @author sjr
 *
 */
public class IsWriteableDirectoryValidator implements IStringConverter<File> , DomainDisplay {

	  public File convert(String value) {
		 
		File f = new File(value);
		
		if (!f.isDirectory())
		{
			throw new ParameterException(value + " is not a directory");
		}
		
		if (!f.canWrite())
		{
			throw new ParameterException(value + " is not writable");
		}
		
		return f;
	     
	  }

	@Override
	public String getDomain() {
		return "{ writable directories }";
	} 

}
