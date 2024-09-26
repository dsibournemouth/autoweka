package ca.ubc.cs.beta.aeatk.acquisitionfunctions;
import static ca.ubc.cs.beta.aeatk.acquisitionfunctions.AcquisitionFunctionHelper.*;
import static ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps.*;
/**
 * The ExpectedExponentialImprovement
 * 
 * Most of this code was a direct copy paste from a .c file in the MATLAB version
 * 
 * 
 * @author Frank Hutter <hutter@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class ExpectedExponentialImprovement implements AcquisitionFunction {

	
	
	private double[] log_exp_exponentiated_imp( double[] fmin_samples, double[] mus, double[] sigmas ){
	    int i,s;
	    double cdfln_1, cdfln_2, c, d;
	    /* Formula from .m file: 
	     *  c = f_min + normcdfln((f_min-mu(i))/sigma(i));
	     *  d = (sigma(i)^2/2 + mu(i)) + normcdfln((f_min-mu(i))/sigma(i) - sigma(i));*/
	    
	    int numSamples = fmin_samples.length;
	    int numMus = mus.length;
	    
	    double[] log_expEI = new double[mus.length];
	    if (numSamples > 1){
	    	throw new IllegalArgumentException("log_exp_exponentiated_imp not yet implemented for numSamples>1; can do that based on logsumexp trick.");
	     
	    }

	    for (i=0; i<numMus; i++){
	        log_expEI[i] = 0;        
	        
	        for (s=0; s<numSamples; s++){
	            cdfln_1 = normcdfln((fmin_samples[s]-mus[i])/sigmas[i]);
	            cdfln_2 = normcdfln((fmin_samples[s]-mus[i])/sigmas[i] - sigmas[i]);
	            c = fmin_samples[s] + cdfln_1;
	            d = (sigmas[i]*sigmas[i]/2 + mus[i]) + cdfln_2;
	            if (c<=d){
	/*                if (c < d-1e-6){
	                    printf("c=%lf, d=%lf\n", c, d);
	                    mexErrMsgTxt("Error -- due to approx. errors with normcdfln?");
	                } else {*/
	                    log_expEI[i] = d;
	/*                }*/
	            } else {
	                log_expEI[i] = d + log(exp(c-d)-1); /* for multiple samples, would collect these values in array, and then apply logsumexp */
	            }
	        }
	    }
	    
	  
	   
	    return log_expEI;
	}
	@Override
	public double[] computeAcquisitionFunctionValue(double f_min_samples,
			double[] predmean, double[] predvar, double standardErrors) {
		
		if(predmean.length != predvar.length)
		{
			throw new IllegalArgumentException("Expected predmean and predvar to have the same length");
		}
		
		
		double log10 = Math.log(10.0);
		
		double[] fmin =  {log10*f_min_samples};
		double[] expImp = log_exp_exponentiated_imp(fmin, times(log10,predmean), times(log10,sqrt(predvar)));
		 //System.out.println(f_min_samples + "," + predmean[0] + "," + predvar[0] + "=> " + expImp[0]);
		
		for(int i=0; i < expImp.length; i++)
		{
			expImp[i] = -expImp[i];
		}
		
		return expImp;
		
	}

}
