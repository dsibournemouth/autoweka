package ca.ubc.cs.beta.aeatk.misc.returnvalues;

/**
 * Class that stores return values for various conditions
 * <p>
 * <b>NOTE:</b>For the most part these shouldn't be changed
 * if there was ever a need to change these it might break some script
 * compatibility, however for the most part you should <b>NOT<b> change
 * as 0 is generally successful execution.
 * 
 * 
 * @author Steve Ramage 
 */
public final class AEATKReturnValues {

	
	/**
	 * Return value for SUCCESS
	 * SEE NOTE AT THE TOP OF FILE DO NOT CHANGE THIS VALUE FROM 0 
	 */
	public static final int SUCCESS = 0;
	
	public static final int PARAMETER_EXCEPTION = 1;
	
	public static final int TRAJECTORY_DIVERGENCE = 2;
	
	public static final int SERIALIZATION_EXCEPTION = 3;
	
	public static final int OH_THE_HUMANITY_EXCEPTION = 66;
	
	public static final int DEADLOCK_DETECTED = 101;
	
	public static final int OTHER_EXCEPTION = 255;

	public static final int UNCAUGHT_EXCEPTION = 128;

	
	private AEATKReturnValues()
	{
		throw new IllegalArgumentException();
	}
}
