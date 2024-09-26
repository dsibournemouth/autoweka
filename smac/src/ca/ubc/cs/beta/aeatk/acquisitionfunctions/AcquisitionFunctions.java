package ca.ubc.cs.beta.aeatk.acquisitionfunctions;
/**
 * Enumeration that outlines the various expected improvement functions
 * @author sjr
 *
 */
public enum AcquisitionFunctions implements AcquisitionFunction {
	/**
	 * The standard expected improvement function
	 */
	EXPONENTIAL(ExpectedExponentialImprovement.class),
	/**
	 * A simple expected improvement function
	 */
	SIMPLE(SimpleAcquisitionFunction.class),
	/**
	 * Lower Confidence Bound
	 */
	LCB(LowerConfidenceBound.class),
	
	/**
	 * Standard EI Improvement Function
	 */
	
	EI(ExpectedImprovement.class),
	
	/**
	 * 
	 */
	
	LCBEIRR(LCBEIRoundRobin.class)
	;
	
	/**
	 * SPO Improvement Function (NOT IMPLEMENTED)
	 */
	//SPO,
	 /*
	 * EIh Improvement Function (NOT IMPLEMENTED)
	 */
	//EIh;
	
	
	
	private AcquisitionFunction internal; 
			
	AcquisitionFunctions(Class<? extends AcquisitionFunction> c)
	{
		
		if(c == null)
		{
			throw new IllegalArgumentException("This Expected Improvement Function is not implemented at the moment: " + this.toString());
		} else
		{
			Class<?>[] args = {};
			try {
				internal = c.getConstructor(args).newInstance();
			} catch (Exception e) {
				
				throw new IllegalStateException("Expected improvement function doesn't have the correct constructor");
			}
		}
		
	}
	
	AcquisitionFunctions()
	{
		internal = null;
	}
	
	public AcquisitionFunction getFunction()
	{
		return internal;
	}

	@Override
	public double[] computeAcquisitionFunctionValue(double f_min_samples,
			double[] predmean, double[] predvar, double standardErrors) {
		return internal.computeAcquisitionFunctionValue(f_min_samples, predmean, predvar, standardErrors);
	}
}
