package ca.ubc.cs.beta.aeatk.acquisitionfunctions;

import static ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps.*;


/**
 * Helper functions for Acquisition Functions
 * 
 * @author Frank Hutter <hutter@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
class AcquisitionFunctionHelper {

	static final double PI =  3.14159265358979323846264338327950288419716939937510;
	static final double LOG2PI = 1.83787706640935;
	/* Approximation of Normal CDF from http://www.sitmo.com/doc/Calculating_the_Cumulative_Normal_Distribution */
	
	static double[] normcdf(double[] x)
	{
		double[] result = new double[x.length];
		for(int i=0; i < x.length; i++)
		{
			result[i] = normcdf(x[i]);
		}
		return result;
	}
	
	static double[] normpdf(double[] x)
	{
		double[] result = new double[x.length];
		for(int i=0; i < x.length; i++)
		{
			result[i] = normpdf(x[i]);
		}
		return result;
	}
	
	static double normcdf( double x)
	{
	     double b1 =  0.319381530;
	     double b2 = -0.356563782;
	     double b3 =  1.781477937;
	     double b4 = -1.821255978;
	     double b5 =  1.330274429;
	     double p  =  0.2316419;
	     double c  =  0.39894228;

	    if(x >= 0.0) {
	        double t = 1.0 / ( 1.0 + p * x );
	        return (1.0 - c * exp( -x * x / 2.0 ) * t * ( t *( t * ( t * ( t * b5 + b4 ) + b3 ) + b2 ) + b1 ));
	    } else {
	        double t = 1.0 / ( 1.0 - p * x );
	        return ( c * exp( -x * x / 2.0 ) * t * ( t *( t * ( t * ( t * b5 + b4 ) + b3 ) + b2 ) + b1 ));
	    }
	}
	
	/**
	 * Computer log of normal probability density function
	 * 
	 * Translated and shortened from Tom Minka's Matlab lightspeed.
	 * 
	 * @param x point to 
	 * @return
	 */
	static double normpdfln(double x)
	{
		return -0.5*(LOG2PI +x*x);
	}
	
	/* Compute log of normal cumulative density function.
	 * Translated and shortened from Tom Minka's Matlab lightspeed 
	 * implementation by Frank Hutter.
	 * More accurate than log(normcdf(x)) when x is small.
	 * The following is a quick and dirty approximation to normcdfln:
	 * normcdfln(x) =approx -(log(1+exp(0.88-x))/1.5)^2 */
	static double normcdfln( double x){
	    double y, z;
	    if( x > -6.5 ){
	        return log( normcdf(x) );
	    }
	    z = pow(x, -2);
	/*    c = [-1 5/2 -37/3 353/4 -4081/5 55205/6 -854197/7];
	    y = z.*(c(1)+z.*(c(2)+z.*(c(3)+z.*(c(4)+z.*(c(5)+z.*(c(6)+z.*c(7)))))));*/
	    y = z*(-1+z*(5.0/2+z*(-37.0/3+z*(353.0/4+z*(-4081.0/5+z*(55205.0/6+z*-854197.0/7))))));
	    return y - 0.5*log(2*PI) - 0.5*x*x - log(-x);
	}


	/* Univariate Normal PDF */
	@SuppressWarnings("unused")
	static double normpdf( double x)
	{
		//Use to be: 1/sqrt(2*PI) * exp(-x*x/2);
	    return exp(normpdfln(x));
	}
	


	static double[] Z(double[] predmean,double fmin , double[] predvar)
	{
		return divide(subtract(fmin,predmean), predvar);
	}
	
	
}
