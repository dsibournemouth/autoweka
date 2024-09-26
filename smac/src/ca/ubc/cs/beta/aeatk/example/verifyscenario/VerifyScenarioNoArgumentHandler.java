package ca.ubc.cs.beta.aeatk.example.verifyscenario;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class VerifyScenarioNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		System.out.println("Verify Scenario Utility");
		System.out.println("\n\n  Usage:\n");
		System.out.println("\tverify-scenario --scenarios <scenario1> <scenario2> <scenario3> ....\n");
		System.out.println("  Skip instance check on disk:");
		System.out.println("\tverify-scenario --verify-instances false --scenarios <scenario1> <scenario2>\n");
		System.out.println("  To specify the remaining scenario options if the files are partial:");
		System.out.println("\tverify-scenario --restore-args \"\"  --scenarios <scenario1> <scenario2>");
		
		return true;
	}

	

}
