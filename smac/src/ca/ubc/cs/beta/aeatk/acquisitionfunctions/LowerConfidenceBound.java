package ca.ubc.cs.beta.aeatk.acquisitionfunctions;


import static ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps.*;

public class LowerConfidenceBound implements AcquisitionFunction {
	
	
	public LowerConfidenceBound()
	{
		
	}
	
	@Override
	public double[] computeAcquisitionFunctionValue(double k,
			double[] predmean, double[] predvar, double standardErrors) {

			if(predmean.length != predvar.length)
			{
				throw new IllegalArgumentException("Expected predmean and predvar to have the same length");
			}
			return	add( predmean,	times(-standardErrors,sqrt(predvar)));
	}


}
