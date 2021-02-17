package ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration;

import java.io.File;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;

public class AlgorithmExecutionConfigHelper {

	public static AlgorithmExecutionConfiguration getSingletonExecConfig()
	{
		return new AlgorithmExecutionConfiguration("foo",  (new File(".")).getAbsolutePath() , ParameterConfigurationSpace.getSingletonConfigurationSpace(), false, false, 20);
	}

	/*
	public static AlgorithmExecutionConfig getParamEchoExecConfig()
	{
		//ParamEchoExecutor
		
		ParamConfigurationSpace configSpace = new ParamConfigurationSpace(TestHelper.getTestFile("paramFiles/paramEchoParamFile.txt"));
		
		
		StringBuilder b = new StringBuilder();
		b.append("java -cp ");
		b.append(System.getProperty("java.class.path"));
		b.append(" ");
		b.append(ParamEchoExecutor.class.getCanonicalName());
		return new AlgorithmExecutionConfig(b.toString(), System.getProperty("user.dir"), configSpace, false, false, 500);
		
	}
	*/
	
	/**
	 * Prefix used to signify a magic value for the AlgorithmExecutableString
	 * 
	 * This is primarily used in MySQLDBTAE for locating the wrapper
	 */
	
}
