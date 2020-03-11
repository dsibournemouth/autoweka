package ca.ubc.cs.beta.aeatk.misc.file;

import java.io.File;

public class HomeFileUtils {
	
	private static volatile boolean slept = false;
	public static File getHomeFile(String filename)
	{
		//You can delete this check eventually if you feel like it. It isn't actually important.
		if(filename.contains(".aclib") && !slept)
		{
			System.out.flush();
			System.err.flush();
			
			System.err.println("Detected request for out of date filename: " + filename + " please get an updated build as aclib has been renamed to aeatk. Sleeping for 5 seconds");
			
			try 
			{ 
				Thread.sleep(5000);
			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			
			slept = true;
			
		}
		return new File(System.getProperty("user.home") + File.separator + filename);
	}
}
