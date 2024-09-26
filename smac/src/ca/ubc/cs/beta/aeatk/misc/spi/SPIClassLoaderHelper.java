package ca.ubc.cs.beta.aeatk.misc.spi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class SPIClassLoaderHelper {


private static final String defaultSearchPath;
	
	static{
		//==== This builds a giant string to search for other Target Algorithm Executors
		StringBuilder sb = new StringBuilder();
		String cwd = System.getProperty("user.dir");
		Set<String> files = new HashSet<String>();
		Set<String> directoriesToSearch = new TreeSet<String>();
		
		directoriesToSearch.add(cwd);
		
		String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
		
		String pluginDirectory = System.getProperty("user.dir");
		for(String location : classpath)
		{
			if(location.endsWith("aclib.jar") || location.endsWith("aeatk.jar"))
			{
				File f = new File(location);
				
				pluginDirectory = f.getParentFile().getAbsolutePath();
				
				if(f.getParentFile().getName().equals("lib"))
				{
					pluginDirectory = f.getParentFile().getParentFile().getAbsolutePath();	
				}
				break;
			}
				
		}
		
		pluginDirectory =new File(pluginDirectory) + File.separator + "plugins" + File.separator;
		
		directoriesToSearch.add(pluginDirectory);
		
		File pluginDir = new File(pluginDirectory);
		boolean errorOccured = false;
		//We will look in the plugins directory and all sub directories, but not further
		if(pluginDir.exists())
		{
			//System.out.println(pluginDirectory);
			//System.exit(0);
			if(!pluginDir.canRead())
			{
				System.err.println("WARNING: The plugin directory (" + pluginDir.getAbsolutePath()+ ") is not readable, plugins may not be available");
				System.err.flush();
				System.out.println("WARNING: The plugin directory (" + pluginDir.getAbsolutePath()+ ") is not readable, plugins may not be available");
				System.out.flush();
				errorOccured = true;
			} else {
				
				for(File f : pluginDir.listFiles())
				{
					
					if(f.isDirectory())
					{
						directoriesToSearch.add(f.getAbsolutePath());
					}
				}
			}
		}
		
		directoriesToSearch.add(new File(cwd) + File.separator + "plugins" + File.separator);
		
		directoriesToSearch.add(System.getProperty("java.class.path"));
		for(String dirName : directoriesToSearch)
		{
			File dir = new File(dirName);
			sb.append(dirName);
			sb.append(File.pathSeparator);
			if(dir.exists())
			{
				if(dir.canRead())
				{
					if(dir.isDirectory())
					{
						for(String fileName : dir.list())
						{
							
							if(fileName.trim().endsWith(".jar"))
							{
								//System.out.println(fileName);
								File jarFile = new File(dir.getAbsolutePath() + File.separator + fileName);
								if(!jarFile.canRead())
								{
									System.err.println("WARNING: The jar file (" + jarFile.getAbsolutePath()+ ") is not readable, plugins may not be available");
									System.err.flush();
									System.out.println("WARNING: The jar file (" + jarFile.getAbsolutePath()+ ") is not readable, plugins may not be available");
									System.out.flush();
									errorOccured = true;
								}
								
								if(!files.contains(fileName))
								{
									sb.append(dir.getAbsolutePath());
									sb.append(File.separator);
									sb.append(fileName);
									sb.append(File.pathSeparator);
									
									//System.out.println("Adding " + fileName);
									files.add(fileName);
								}
							}
						}
					}	
				}
			}
		}
		
		defaultSearchPath = sb.toString();
		if(errorOccured)
		{
			System.out.println("Warnings have occured sleeping for 30 seconds");
		
			try {
				for(int i=0; i < 30; i++)
				{
					Thread.sleep(1000);
					System.out.print(".");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	
	private static volatile ClassLoader c = null;
	/**
	 * Retrieves a modified class loader to do dynamically search for jars
	 * @return a class loader that will search the plugins directory as well
	 */
	public static ClassLoader getClassLoader()
	{
		if( c != null) return c;
		String pathtoSearch = defaultSearchPath;
		String[] paths = pathtoSearch.split(File.pathSeparator);
		
		ArrayList<URL> urls = new ArrayList<URL>(paths.length);
				
		for(String path : paths)
		{
			
			File f = new File(path);
			
			try {
				urls.add(f.toURI().toURL());
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.err.println("Couldn't parse path " + path);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
			
			
		}
		
		
		URL[] urlsArr = urls.toArray(new URL[0]);
		
		
		URLClassLoader ucl = new URLClassLoader(urlsArr, SPIClassLoaderHelper.class.getClassLoader());
		
		c = ucl;
		return ucl;
		
		
		
	}
	
	public static String getDefaultSearchPath()
	{
		return defaultSearchPath;
	}
	
	
}
