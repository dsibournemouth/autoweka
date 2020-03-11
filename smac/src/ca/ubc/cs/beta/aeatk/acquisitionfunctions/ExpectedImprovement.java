package ca.ubc.cs.beta.aeatk.acquisitionfunctions;

import static ca.ubc.cs.beta.aeatk.acquisitionfunctions.AcquisitionFunctionHelper.*;
import static ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps.*;

/***
 * Standard Expected Improvement Function
 * 
 * Please see the log_exp_imp.m function (which is included at the bottom of the file)

 * @author Steve Ramage <seramage@cs.ubc.ca> (Ported from MATLAB)
 * @author Frank Hutter <fh@informatik.uni-freiburg.de> (Original Author?) 
 *  
 */
public class ExpectedImprovement implements AcquisitionFunction {

	private static final double MIN_VALUE = -Math.pow(10, 100);

	@Override
	public double[] computeAcquisitionFunctionValue(double f_min_samples, double[] predmean, double[] predvar, double standardErrors) {

		double[] predstddev = sqrt(predvar);
			
		double[] u = divide(subtract(f_min_samples,predmean), predstddev);
		
		double[] cdfU = AcquisitionFunctionHelper.normcdf(u);
		double[] pdfU = AcquisitionFunctionHelper.normpdf(u);
		
		double[] term1 = (times(subtract(f_min_samples,predmean),cdfU));
		double[] term2 = (times(predstddev,pdfU));
		
		double[] standardEI = add(term1, term2);
		
		double[] logEI = new double[standardEI.length];
		
		for(int i=0; i < predmean.length; i++)
		{
		
			double mu = predmean[i];
			double sigma = predstddev[i];
			
			double x = (f_min_samples - mu) / sigma;
			
			
			double result;
			if(Math.abs(f_min_samples - mu) == 0)
			{ //Degenerate case 1: first term vanishes
				
				if(sigma > 0)
				{
					result = Math.log(sigma) + normpdfln(x);
				} else
				{
					result = Double.NEGATIVE_INFINITY;
				}
			} else if(sigma == 0)  
			{ //Degenerate case 2: second term vanishes and first term has a special form
				if(mu < f_min_samples)
				{
					result = Math.log(f_min_samples-mu);
				} else
				{
					result = Double.NEGATIVE_INFINITY;
				}
				
			} else
			{ //Normal case
				//log(y+z) is tricky, we distinguish two cases:
				double b  = Math.log(sigma) + normpdfln(x);
				if(mu < f_min_samples)
				{ // When y > 0 , z > 0, we define a = ln(y), b = ln(z)
				  // Then y+z = exp[ max(a,b) + ln(1 + exp(-|b-a|)) ],
				  // and thus log(y+z) = max(a,b) + ln(1 + exp(-|b-a|))
					double a = Math.log(f_min_samples-mu) + normcdfln(x);
					result = Math.max(a, b) +  Math.log1p(Math.exp(-Math.abs(b-a)));
					
					//checkAssertions(result, standardEI[i], predmean[i], predvar[i]);
				} else
				{
				  // When y<0, z>0, we define a=ln(-y), b=ln(z), and it has to be true that b >= a in order to satisfy y+z>=0.
				  // Then y+z = exp[ b + ln(exp(b-a) -1) ],
				  // and thus log(y+z) = a + ln(exp(b-a) -1)
					double a = log(mu-f_min_samples) + normcdfln(x);
		            if (a >= b) //a>b can only happen due to numerical inaccuracies or approximation errors
		            {
		                result = Double.NEGATIVE_INFINITY;
		            } else
		            {
		                result = b + Math.log1p(-exp(a-b));
		                //checkAssertions(result, standardEI[i], predmean[i], predvar[i]);
		            }
					            
				}
			}
			
			logEI[i] = -Math.max(MIN_VALUE, result);
		}
		
		return logEI;
	}

	/*
	private final double NEG103 = -Math.pow(10, -3);
	private final double POS103 = Math.pow(10, -3);
	
	private void checkAssertions(double logEI, double EI, double predmean, double predvar)
	{
		
		double delta = Math.exp(logEI) - EI;
		if(Double.isNaN(logEI))
		{
			throw new IllegalStateException("Log Expected Improvement has a NaN value. This probably indicates a bug, and you should contact the developer if you get this error (hopefully you can reproduce it). Expected improvement was: " + EI + "predmean:" + predmean + " predvar" + predvar );
		} else if(delta < NEG103)
		{
			throw new IllegalStateException("Log-EI and EI have a difference " + delta + " expected it to be greater than -10^-3. Please contact the developer if you get this error (hopefully you can reproduce it). Expected improvement was: " + EI + "predmean:" + predmean + " predvar" + predvar );
		} else if(delta > POS103)
		{
			throw new IllegalStateException("Log-EI and EI have a difference " + delta + " expected it to be less than +10^-3. Please contact the developer if you get this error (hopefully you can reproduce it). Expected improvement was: " + EI + "predmean:" + predmean + " predvar" + predvar );
		}
		
	}
	*/
		
}



/* log_exp_imp.m
 * function log_expected_improvement = log_exp_imp(f_min, mu, sigma)
%sigma(find(sigma<1e-10)) = NaN; % To avoid division by zero.
% Tom Minka's normpdf takes row vectors -- column vectors are interpreted as multivariate.
expected_improvement = (f_min-mu) .* normcdf((f_min-mu)./sigma) + sigma .* normpdf(((f_min-mu)./sigma)')';

%Expected improvement often yields zero. Use more robust log expected improvement
%instead.

x = (f_min-mu)./sigma;
log_expected_improvement = zeros(length(mu),1);
for i=1:length(mu)
    if abs(f_min-mu(i)) == 0 %=== Degenerate case 1: first term vanishes.
        if sigma(i) > 0
            log_expected_improvement(i) = log(sigma(i)) + normpdfln(x(i));
        else
            log_expected_improvement(i) = -inf;
        end
    elseif sigma(i) == 0 %=== Degenerate case 2: second term vanishes and first term has a special form.
        if mu(i) < f_min
            log_expected_improvement(i) = log(f_min-mu(i));
        else
            log_expected_improvement(i) = -inf;
        end
    else %=== Normal case.
        b = log(sigma(i)) + normpdfln(x(i));
        %=== log(y+z) is tricky, we distinguish two cases:
        if f_min>mu(i)
            % When y>0, z>0, we define a=ln(y), b=ln(z).
            % Then y+z = exp[ max(a,b) + ln(1 + exp(-|b-a|)) ],
            % and thus log(y+z) = max(a,b) + ln(1 + exp(-|b-a|))
            a = log(f_min-mu(i)) + normcdfln(x(i));
            log_expected_improvement(i) = max(a,b) + log(1 + exp(-abs(b-a)));
            assert(imag(log_expected_improvement(i))==0);
            assert(exp(log_expected_improvement(i)) > expected_improvement(i)-1e-3);
            assert(exp(log_expected_improvement(i)) < expected_improvement(i)+1e-3);
        else
            % When y<0, z>0, we define a=ln(-y), b=ln(z), and it has to be true that b >= a in order to satisfy y+z>=0.
            % Then y+z = exp[ b + ln(exp(b-a) -1) ],
            % and thus log(y+z) = a + ln(exp(b-a) -1)
            a = log(mu(i)-f_min) + normcdfln(x(i));
            if a >= b %a>b can only happen due to numerical inaccuracies or approximation errors
                log_expected_improvement(i) = -inf;
            else
                log_expected_improvement(i) = b + log(1-exp(a-b));
                assert(imag(log_expected_improvement(i))==0);
                assert(exp(log_expected_improvement(i)) > expected_improvement(i)-1e-3);
                assert(exp(log_expected_improvement(i)) < expected_improvement(i)+1e-3);
            end
        end
    end
end
log_expected_improvement(find(log_expected_improvement==-inf)) = -1e100;
*/
