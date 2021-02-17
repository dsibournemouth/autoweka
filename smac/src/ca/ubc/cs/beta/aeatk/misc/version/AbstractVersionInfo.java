package ca.ubc.cs.beta.aeatk.misc.version;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Class that implements version info
 * @author Steve Ramage 
 *
 */
public abstract class AbstractVersionInfo implements VersionInfo {

	
	private final String name;
	private final String version;
	private static final Logger log = LoggerFactory.getLogger(AbstractVersionInfo.class);
	/**
	 * Creates an AbstractVersionInfo
	 * <p>
	 * Hopefully no code smell detecter picks up this attrocity. :)
	 * <p>
	 * The reason the class is like this is because generally the version is stored in a file in the classpath
	 * and I can't have two constructors that are both String, String.
	 * <p> 
	 * 
	 * @param name  name of the product
	 * @param arg2  argument 2, a filename if arg2IsFile is <code>true<code>, the version string if <code>false</code>
	 * @param fromFile whether arg2 is a file that should be loaded from the classpath, or the version string 
	 */
	public AbstractVersionInfo(String name, String arg2, boolean fromFile)
	{
		this.name =name;
		
		if(fromFile)
		{
			String version = null;
			try {
				String fileInClassPath = arg2;
				
				ClassLoader cl = this.getClass().getClassLoader();
				InputStream inputStream = cl.getResourceAsStream(fileInClassPath);
				if(inputStream == null)
				{
					inputStream = cl.getResourceAsStream(File.separator + fileInClassPath);
				}
				BufferedReader reader  =  new BufferedReader(new InputStreamReader(inputStream));
			
				
				version = reader.readLine();
			
				reader.close();
			} catch (Throwable t) {
				
				String env = System.getenv("SHOW_ERROR");
				if(env != null && env.toUpperCase().equals("TRUE"))
				{
					System.out.println(t);
					t.printStackTrace();
					log.error("Version Information Error: ", t);
				}
				log.debug("Could not retrieve version information set SHOW_ERROR=TRUE as an environment variable to see the error");
				
				if(version == null)
				{
					version="ERROR LOADING VERSION INFORMATION";
				}
				
			}
			
			this.version =  version;
		} else
		{
			this.version = arg2;
		}
	}
	
	@Override
	public final String getProductName() {
		return name;
	}

	@Override
	public final String getVersion() {
		return version;
	}

}
