package ca.ubc.cs.beta.aeatk.acquisitionfunctions;

public interface AcquisitionFunction {

	/**
	 * Computes the negative of the acquisition function for each predmean, predvar 
	 * 
	 * NOTE: We are generally minimizing this function, so many implementations will take a negative of their result. 
	 * See the unit tests and other implementations for details, unfortunately it's not the best interface nor the best documented.
	 * 
	 * @param f_min_samples		the minimum empirical cost found so far
	 * @param predmean			predicted mean of the samples
	 * @param predvar			predicted variance of the samples
	 * @param standardErrors 	numberOfStandardErrorsToSampleWith (only applicable with LCB)
	 * 
	 * @return					array of values which correspond to the expected improvement for the corresponding entries in predmean and predvar
	 */
	public double[] computeAcquisitionFunctionValue(double f_min_samples, double[] predmean, double[] predvar, double standardErrors);
	
}
