package ca.ubc.cs.beta.aeatk.model.data;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;

public class MaskInactiveConditionalParametersWithDefaults extends	AbstractSanitizedModelData
{
	private final double[][] paramValues;
	public MaskInactiveConditionalParametersWithDefaults(SanitizedModelData mds, ParameterConfigurationSpace configSpace)
	{
		super(mds);
		double[] defaultValues = configSpace.getDefaultConfiguration().toValueArray();
		paramValues = mds.getConfigs();
		
		for (int i = 0; i < paramValues.length; i++) {
			for (int j = 0; j < defaultValues.length; j++) {
				if (Double.isNaN(paramValues[i][j])){
					paramValues[i][j] = defaultValues[j];  
				}
			}
		}
	}
	
	@Override
	public double[][] getConfigs()
	{
		double[][] retValue = new double[paramValues.length][0];
		for(int i=0; i < retValue.length; i++)
		{
			retValue[i]=paramValues[i].clone();
		}
		return retValue;
	}

}
