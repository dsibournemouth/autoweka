package ca.ubc.cs.beta.aeatk.model.data;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;

/**
 * Fixes Conditional Values such that inactive Parameters are replaced by their default values
 * 
 * @author fhutter
 *
 */
public class DefaultValueForConditionalsMDS extends RawSanitizedModelData {

	public DefaultValueForConditionalsMDS(double[][] instanceFeatures,
			double[][] paramValues, double[] responseValues,
			int[] usedInstancesIdxs, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredRuns,
			ParameterConfigurationSpace configSpace) {
		super(instanceFeatures, paramValues, responseValues, usedInstancesIdxs, logModel, theta_inst_idxs, censoredRuns, configSpace);

		double[] defaultValues = configSpace.getDefaultConfiguration().toValueArray();
		
		//=== Replace NaNs by default values.
		for (int i = 0; i < paramValues.length; i++) {
			for (int j = 0; j < defaultValues.length; j++) {
				if (Double.isNaN(paramValues[i][j])){
					this.configs[i][j] = defaultValues[j]; // TODO: Steve will fix this, so there is no need for "this." anymore. 
				}
			}
		}
	}
	
	//=== This one is for calling by Matlab.
	public DefaultValueForConditionalsMDS(double[][] instanceFeatures,
			double[][] paramValues, double[] responseValues,
			int[] usedInstancesIdxs, boolean logModel,
			double[] defaultValues ,  int[][] theta_inst_idxs, boolean[] censoredRuns) {
		super(instanceFeatures, paramValues, responseValues, usedInstancesIdxs, logModel, theta_inst_idxs, censoredRuns);

		//=== Replace NaNs by default values.
		for (int i = 0; i < paramValues.length; i++) {
			for (int j = 0; j < defaultValues.length; j++) {
				if (Double.isNaN(paramValues[i][j])){
					this.configs[i][j] = defaultValues[j]; // TODO: Steve will fix this, so there is no need for "this." anymore. 
				}
			}
		}
	}

}
