package ca.ubc.cs.beta.aeatk.misc.math.distribution;

import java.util.Random;

import net.sf.doodleproject.numerics4j.exception.ConvergenceException;
import net.sf.doodleproject.numerics4j.special.Erf;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import ca.ubc.cs.beta.aeatk.random.RandomUtil;

/**
 * Truncated Normal Distribution as defined in:
 * 
 * Bayesian Optimization With Censored Response Data
 * Frank Hutter, Holger Hoos, and Kevin Leyton-Brown
 * (http://www.cs.ubc.ca/~hutter/papers/11-NIPS-workshop-BO-with-censoring.pdf)
 * 
 * Not all methods are implemented
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class TruncatedNormalDistribution extends AbstractRealDistribution {

	
	private static final long serialVersionUID = -901332698348589236L;
	private final double mu;
	private final double variance;
	private final double sigma;
	private final double kappa;
	private final NormalDistribution norm;
	private final Random random;
	
	/**
	 * Creates the Distribution
	 * @param mean 			mean of the distribution
	 * @param variance		variance of the distribution
	 * @param kappa			minimum value of the distribution
	 * @param rand			random object used for tie breaking
	 */
	public TruncatedNormalDistribution(double mean, double variance, double kappa, Random rand)
	{
		this.mu = mean;
		this.variance = variance;
		this.kappa = kappa;
		this.sigma = Math.sqrt(variance);
		this.norm = new NormalDistribution();
		this.random = rand;
		
	}
	
	@Override
	public double cumulativeProbability(double x) {

		throw new UnsupportedOperationException("Not Implemented Yet");
	}

	@Override
	public double density(double x) {
		if(x < kappa)
		{	
			return 0.0;
		} else
		{
			double numerator = (1 / sigma) * norm.density((x - mu)/sigma);
			double denominator = (1 - norm.cumulativeProbability((mu-kappa)/sigma));
			return numerator / denominator;
		}
	}

	@Override
	public double getNumericalMean() {
		return mu;
	}

	@Override
	public double getNumericalVariance() {
		return variance;
	}

	@Override
	public double getSupportLowerBound() {
		throw new UnsupportedOperationException("Not Implemented Yet");
	}

	@Override
	public double getSupportUpperBound() {
		throw new UnsupportedOperationException("Not Implemented Yet");
	}

	@Override
	public boolean isSupportConnected() {
		throw new UnsupportedOperationException("Not Implemented Yet");	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		throw new UnsupportedOperationException("Not Implemented Yet");
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		throw new UnsupportedOperationException("Not Implemented Yet");
	}

	private final double EPSILON = Math.pow(10, -14);
	private double erfinv(double x)
	{

		try {
			return Erf.inverseErf(x);
		} catch(ConvergenceException e)
		{
			//If this crashes we will move back to right
			System.err.println("Unloggable Convergence Exception occurred with:" + x + " you can safely disregard this message as we will try again (We are more just curious in knowing the values)");
			x *= -1;
			double value = x;
			double eps = EPSILON;
			if(x == 0)
			{
				return 0;
			}
			if( x > 0)
			{
				while(value > 0)
				{

					value = x - eps;
					eps *= 2;
					try {
						return Erf.inverseErf(value);
					} catch(ConvergenceException e2)
					{
						System.err.println("Unloggable Convergence Exception occurred with:" + value + " you can safely disregard this message as we will try again");
					}
				}
			} else
			{
				while(value < 0)
				{

					value = x + eps;
					eps *= 2;
					try {
						return Erf.inverseErf(value);
					} catch(ConvergenceException e2)
					{
						System.err.println("Unloggable Convergence Exception occurred with:" + value + " you can safely disregard this message as we will try again");
					}
				}

			}
			
			return 0;
		}
	}
	
	@Override
	public double sample()
	{
		/*
		 * Stolen from mtatlab code
		 * rand_draw_truncated_normal.m 
		 * 
		 *PHIl = normcdf((a-mu)/sigma);
		 * PHIr = normcdf((b-mu)/sigma);
		 * u is essentially the percentile we want
		 *	samples = mu + sigma*( sqrt(2)*erfinv(2*(PHIl+(PHIr-PHIl)*u)-1) );

		 */
		double u = random.nextDouble();
		return inverseCDF(u);
	}
	
	public double inverseCDF(double u){
		double PHIl = norm.cumulativeProbability((kappa-mu)/sigma);
		double PHIr = norm.cumulativeProbability((Double.POSITIVE_INFINITY-mu)/sigma);
		double sample = mu + sigma*( Math.sqrt(2)*erfinv(2*(PHIl+(PHIr-PHIl)*u)-1) );
		
		return sample;
	}
	
	public double[] getValuesAtStratifiedShuffledIntervals(int numSamples){
		/* Matlab code:
         * inc = 1/(numSamples+1);
         * u = inc:inc:1-inc;
         * perm = randperm(numSamples);
         * u = u(perm);
         * samples = rand_draw_truncated_normal(model.y(cens_idx(i)), inf, mu(i), sigma(i), [1 numSamples], u);
		 */		
		
		//=== Get evenly spaced numbers in [0,1], offset such that the first number is the same distance from zero as from the second number.
		double increment = 1/(numSamples+1.0);
		double result[] = new double[numSamples];
		double current = increment;
		for (int i = 0; i < result.length; i++) {
			result[i] = inverseCDF(current);
			current += increment;
		}
		result = RandomUtil.getPermutationOfArray(result, random);
		return result;

/*		   yHal[j][k] = Math.min(tNorm.sample(),maxValue);
		   imputedValues_sum += yHal[j][k];
		   imputedValues_count++;
		   if(Double.isInfinite(yHal[j][k]))
		   {
			   System.out.println("Hello");
		   }
*/
	}
	
	
	public double[] getValuesAtStratifiedIntervals(int numSamples){
		/* Matlab code:
         * inc = 1/(numSamples+1);
         * u = inc:inc:1-inc;
         * perm = randperm(numSamples);
         * u = u(perm);
         * samples = rand_draw_truncated_normal(model.y(cens_idx(i)), inf, mu(i), sigma(i), [1 numSamples], u);
		 */		
		
		//=== Get evenly spaced numbers in [0,1], offset such that the first number is the same distance from zero as from the second number.
		double increment = 1/(numSamples+1.0);
		double result[] = new double[numSamples];
		double current = increment;
		for (int i = 0; i < result.length; i++) {
			result[i] = inverseCDF(current);
			current += increment;
		}
				return result;

/*		   yHal[j][k] = Math.min(tNorm.sample(),maxValue);
		   imputedValues_sum += yHal[j][k];
		   imputedValues_count++;
		   if(Double.isInfinite(yHal[j][k]))
		   {
			   System.out.println("Hello");
		   }
*/
	}
	
	
	@Override
	public double probability(double arg0) {

		return 0;
	}

}
