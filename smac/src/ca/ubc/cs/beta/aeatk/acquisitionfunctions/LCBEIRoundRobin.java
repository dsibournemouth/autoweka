package ca.ubc.cs.beta.aeatk.acquisitionfunctions;

import com.beust.jcommander.ParameterException;

public class LCBEIRoundRobin implements AcquisitionFunction{

	@Override
	public double[] computeAcquisitionFunctionValue(double f_min_samples,
			double[] predmean, double[] predvar, double standardErrors) {
		/**
		 * This is a place holder for something that will be fixed later,
		 * that is once we seperate the first argument into a sample k and f_min_samples.
		 * 
		 * In any event the way this is implemented in dSMAC is kind of annoying and hacky.
		 */
		throw new ParameterException("This aquisition function is not available presently");
	}

}
