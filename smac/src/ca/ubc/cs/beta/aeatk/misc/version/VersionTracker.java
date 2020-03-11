package ca.ubc.cs.beta.aeatk.misc.version;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ca.ubc.cs.beta.aeatk.misc.spi.SPIClassLoaderHelper;

/**
 * Utility class that allows various related projects to log their versions and have them reported
 * @author sjr
 *
 */
public class VersionTracker {

	private static final Logger log = LoggerFactory.getLogger(VersionTracker.class);

	private static ClassLoader cl = SPIClassLoaderHelper.getClassLoader();
	

	private static SortedMap<String, String> init()
	{
			//System.err.println(VersionTracker.class.getClassLoader().getResource( VersionTracker.class.getCanonicalName().replaceAll("\\.","/") + ".class").getFile());
			boolean errorPrinted = false;
			Iterator<VersionInfo> versionInfo = ServiceLoader.load(VersionInfo.class, cl).iterator();
			SortedMap<String, String> versionMap = new TreeMap<String, String>();
			
			while(versionInfo.hasNext())
			{
				
				try { 
				VersionInfo info = versionInfo.next();
				
				versionMap.put(info.getProductName(),info.getVersion());
				} catch(Exception e)
				{
					log.warn("Error occured while loading version Information", e);
					
					if(!errorPrinted)
					{
						errorPrinted = true;
						System.err.println(SPIClassLoaderHelper.getDefaultSearchPath());
						System.err.println(VersionTracker.class.getClassLoader().getResource( VersionTracker.class.getCanonicalName().replaceAll("\\.","/") + ".java"));
					}
					
					
				}
			}
				
			
		return versionMap;
	}
	
	public static void main(String[] args)
	{
		logVersions();
	}

	
	
	/**
	 * Gets a map of all product versions
	 * @return map that will have an iterator in alphabetical order
	 */
	public static Map<String, String> getVersionMap()
	{
		return init();
	}

	
	
	/**
	 * Gets a string representation of all registered product versions 
	 * @return string of all versions
	 */
	public static String getVersionInformation()
	{
		SortedMap<String, String> versionMap = init();
		StringBuilder sb = new StringBuilder();
		
		for(Entry<String, String> ent : versionMap.entrySet())
		{
			sb.append(ent.getKey()).append(" ==> ").append(ent.getValue()).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * Logs the version number of all registered products
	 */
	public static void logVersions()
	{
		
		SortedMap<String, String> versionMap = init();
		if(versionMap.isEmpty())
		{
			log.warn("Unable to find ANY version information, if you made this JAR yourself chances are you did not setup SPI correctly. See the SMAC Manual Developer Reference for more information");
		}
		for(Entry<String, String> ent : versionMap.entrySet())
		{
			log.info(CommonMarkers.SKIP_CONSOLE_PRINTING,"Version of {} is {} ", ent.getKey(), ent.getValue());
		}
	}
	
	
	public static void setClassLoader(ClassLoader classLoader) {
		cl = classLoader;
		
	}
}
