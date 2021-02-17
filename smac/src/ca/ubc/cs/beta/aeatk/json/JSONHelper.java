package ca.ubc.cs.beta.aeatk.json;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;

public final class JSONHelper {

	public static String getJSON(AlgorithmExecutionConfiguration execConfig)
	{
		JSONConverter<AlgorithmExecutionConfiguration> jsonConverter = new JSONConverter<AlgorithmExecutionConfiguration>()
		{
			
		};
		
		return jsonConverter.getJSON(execConfig);
		
	}
	
	public static AlgorithmExecutionConfiguration getAlgorithmExecutionConfiguration(String json)
	{
		JSONConverter<AlgorithmExecutionConfiguration> jsonConverter = new JSONConverter<AlgorithmExecutionConfiguration>()
		{
			
		};
				
		return jsonConverter.getObject(json);
	}
}
