
package ca.ubc.cs.beta.aeatk.example.satisfiabilitychecker;

import ca.ubc.cs.beta.aeatk.misc.version.AbstractVersionInfo;

//This annotation generates the required files necessary for 
//SPI to work. See the Manual for more info on SPI.
//It is commented out because we don't actually want it to display when running 
//since this example is included in AEATK

//@ProviderFor(VersionInfo.class)
public class SatisfiabilityCheckerVersionInfo extends AbstractVersionInfo {

	public SatisfiabilityCheckerVersionInfo() {
		super("Satisfiability Checker", "Version 1.0", false);
	}

}
