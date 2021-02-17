package ca.ubc.cs.beta.aeatk.misc.version;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(VersionInfo.class)
public class OSVersionInfo extends AbstractVersionInfo {

	public OSVersionInfo() 
	{
		super("OS", System.getProperty("os.name") + " " +  System.getProperty("os.version") + " (" +  System.getProperty("os.arch") + ")", false);
	}

}
