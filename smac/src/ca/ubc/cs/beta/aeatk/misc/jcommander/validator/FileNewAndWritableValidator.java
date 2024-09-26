package ca.ubc.cs.beta.aeatk.misc.jcommander.validator;

import java.io.File;
import java.io.IOException;


import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * JCommander Validator that ensures that a file is new, and writable/creatable
 * @author sjr
 *
 */
public class FileNewAndWritableValidator implements IStringConverter<File> {

	  @Override
	  public File convert(String value) {
		 
		  
	  File f = new File(value);
		try {
			if (!f.createNewFile())
			{
				throw new ParameterException("File " + value + " already exists");
			}
		
			
				
			
		} catch (IOException e) {
		
			throw new ParameterException("Couldn't create new file " + value);
		}
		if (!f.isFile())
		{
			throw new ParameterException(value + " is not a directory");
		}
		
		if (!f.canWrite())
		{
			throw new ParameterException(value + " is not writable");
		}
		
		//This opens us to race conditions, but is the only way to work
		//with JCommander and TrueZIP
		
		if(!f.delete())
		{
			throw new ParameterException("File " + value + " is not deletable [we check to see if the file is creatable, and then delete it and then a library creates it])");
		}
		
		return f;
	     
	  } 

}
