package ca.ubc.cs.beta.aeatk.acquisitionfunctions;
import static ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps.*;
/**
 * A simple acquisition function
 * (Probably look at the matlab code for the origin of this)
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class SimpleAcquisitionFunction implements AcquisitionFunction {

	
	@Override
	public double[] computeAcquisitionFunctionValue(double f_min_samples,
			double[] predmean, double[] predvar, double standardErrors) {
		if(predmean.length != predvar.length)
		{
			throw new IllegalArgumentException("Expected predmean and predvar to have the same length");
		}
		
		return times(-1,exp(times(-1,predmean)));
	}

}
