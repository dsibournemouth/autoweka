
package ca.ubc.cs.beta.aeatk.example.tae;

import ca.ubc.cs.beta.aeatk.misc.version.AbstractVersionInfo;

//This annotation generates the required files necessary for 
//SPI to work. See the Manual for more info on SPI.
//It is commented out because we don't actually want it to display when running 
//since this example is included in AEATK

//@ProviderFor(VersionInfo.class)
public class TargetAlgorithmEvaluatorRunnerVersionInfo extends AbstractVersionInfo {

	public TargetAlgorithmEvaluatorRunnerVersionInfo() {
		super("Target Algorithm Evaluator Runner Example", "Version 1.0", false);
	}

}
