package ca.ubc.cs.beta.aeatk.misc.version;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(VersionInfo.class)
public class AEATKVersionInfo extends AbstractVersionInfo {

	public AEATKVersionInfo()
	{
		super("Algorithm Execution & Abstraction Toolkit", "aeatk-version.txt",true);
	}
}
