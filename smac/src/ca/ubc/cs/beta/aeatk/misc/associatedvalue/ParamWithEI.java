package ca.ubc.cs.beta.aeatk.misc.associatedvalue;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;

/**
 * Subtype of <code>AssociatedValue</code> used by Local Search, essentially
 * the helper method {@link ParamWithEI#merge(double[], List)} is the only difference
 *  
 * @author sjr
 *
 */
public class ParamWithEI extends AssociatedValue<Double, ParameterConfiguration> {

	/**
	 * Standard Constructor
	 * @param t double to associate (represents expected improvement)
	 * @param v Param configuration
	 */
	public ParamWithEI(Double t, ParameterConfiguration v) {
		super(t, v);
	}
	
	/**
	 * Takes an array of expected improvement values, and a list of configurations and generates a list of ParamWithEI
	 *  
	 * @param x double array of expected improvement values
	 * @param c list of configuration
	 * @return list of ParamWithEI objects
	 */
	public static List<ParamWithEI> merge(double[] x, List<ParameterConfiguration> c)
	{
		if (x.length != c.size())
		{
			throw new IllegalArgumentException("List of double and number of configurations must be the same");
		}
		
		List<ParamWithEI> list = new ArrayList<ParamWithEI>();
		for(int i=0; i < x.length; i++)
		{
			list.add(new ParamWithEI(x[i],c.get(i)));
		}
		return list;
	}
}
