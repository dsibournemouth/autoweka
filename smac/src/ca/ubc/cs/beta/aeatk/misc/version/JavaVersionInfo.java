package ca.ubc.cs.beta.aeatk.misc.version;

import org.mangosdk.spi.ProviderFor;


@ProviderFor(VersionInfo.class)
public class JavaVersionInfo extends AbstractVersionInfo {

	public JavaVersionInfo() 
	{
		super("Java Runtime Environment", System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")", false);
	}

}
