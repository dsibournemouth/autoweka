package ca.ubc.cs.beta.aeatk.misc.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.stat.StatUtils;

import ca.ubc.cs.beta.aeatk.random.RandomUtil;
import ca.ubc.cs.beta.models.fastrf.utils.Hash;

/**
 * Utility class with Math operations on arrays
 * @author sjr
 *
 */
public class ArrayMathOps {

	/**
	 * Returns exp(x)
	 * @param x exponent to raise
	 * @return exp(x)
	 */
	public static double exp(double x)
	{
		return Math.pow(Math.E, x);
	}
	
	
	/**
	 * Returns log of x
	 * @param x input parameter
	 * @return	log(x)
	 */
	public static double log(double x)
	{
		return Math.log(x);
	}

	/**
	 * Raises x^y
	 * @param x base
	 * @param y	exponent
	 * @return	x^y
	 */
	public static double pow(double x, double y)
	{
		return Math.pow(x, y);
	}
	
	/**
	 * Computes the square root of x
	 * @param x input	
	 * @return sqrtx()
	 */
	public static double sqrt(double x)
	{
		return Math.sqrt(x);
	}
	
	
	/**
	 * Computes the square root of every element
	 * @param x array of values to square root
	 * @return square root of every value
	 */
	public static double[] sqrt(double[] x)
	{
		double[] y = new double[x.length];
		for(int i=0; i < x.length; i++)
		{
			y[i] = Math.sqrt(x[i]);
		}
		return y;
	}	
	
	/**
	 * Multiplies every element of x2, by the scalar x1
	 * @param x1 scalar value
	 * @param x2 array value
	 * @return	x1 * x2;
	 */
	public static double[] times( double x1, double[] x2)
	{
		double[] y = new double[x2.length];
		for(int i=0; i < x2.length; i++)
		{
			y[i] = x1*x2[i];
		}
		return y;
	}
	
	/**
	 * Multiplies every element of x2, by the element in x1
	 * @param x1 scalar value
	 * @param x2 array value
	 * @return	x1 * x2;
	 */
	public static double[] times( double[] x1, double[] x2)
	{
		double[] y = new double[x2.length];
		for(int i=0; i < x2.length; i++)
		{
			y[i] = x1[i]*x2[i];
		}
		return y;
	}	
	
	
	/**
	 * divide every element of x1, by x2
	 * @param x1 scalar value
	 * @param x2 array value
	 * @return	x1 * x2;
	 */
	public static double[] divide( double[] x1, double[] x2)
	{
		double[] y = new double[x1.length];
		for(int i=0; i < x1.length; i++)
		{
			y[i] = x1[i]/x2[i];
		}
		return y;
	}	
	
	
	/**
	 * Raises every element in exp, by the base 
	 * @param base    base to raise every exponent to
	 * @param exp     exponent
	 * @return   array of per element of base ^ exp[i]
	 */
	public static double[] pow(double base, double[] exp)
	{
		double[] y = new double[exp.length];
		for(int i=0; i < exp.length; i++)
		{
			y[i] = Math.pow(base, exp[i]);
		}
		return y;
	}
	
	/**
	 * Adds left operands and right operands
	 * @param operL
	 * @param operR
	 * @return
	 */
	public static double[] add(double[] operL, double[] operR) {
		double[] result = new double[operL.length];
		
		for(int i=0; i < result.length; i++)
		{
			result[i] = operL[i]+operR[i];
		}
		
		return result;
	}
	
	/**
	 * Subtract right from left 
	 * @param operL
	 * @param operR
	 * @return
	 */
	public static double[] subtract(double[] operL, double[] operR) {
		double[] result = new double[operL.length];
		
		for(int i=0; i < result.length; i++)
		{
			result[i] = operL[i]-operR[i];
		}
		
		return result;
	}
	

	/**
	 * Subtract right from left 
	 * @param operL
	 * @param operR
	 * @return
	 */
	public static double[] subtract(double operL, double[] operR) {
		double[] result = new double[operR.length];
		
		for(int i=0; i < result.length; i++)
		{
			result[i] = operL-operR[i];
		}
		
		return result;
	}
	

	/**
	 * Subtract right from left 
	 * @param operL
	 * @param operR
	 * @return
	 */
	public static double[] subtract(double[] operL, double operR) {
		double[] result = new double[operL.length];
		
		for(int i=0; i < result.length; i++)
		{
			result[i] = operL[i]-operR;
		}
		
		return result;
	}
	
	
	
	/**
	 * Computes exp(x) for every entry in x
	 * @param x array of values to raise to the exponent e
	 * @return resulting values
	 */
	
	public static double[] exp(double[] x)
	{
		return pow(Math.E, x);
	}
	
	/**
	 * Computes matrix^T for input matrix
	 * @param matrix a RECTANGULAR array to transpose
	 * @return the transpose of the matrix
	 */
	public static double[][] transpose(double[][] matrix)
	{
		double[][] transpose = new double[matrix[0].length][matrix.length];
		for(int i=0; i < transpose.length; i++)
		{
			for(int j=0; j < transpose[0].length; j++)
			{
				transpose[i][j] = matrix[j][i];
			}
		}
		
		return transpose;
	}
	
	/**
	 * Computes a hash code of the matrix in a way that is compatible with MATLAB
	 * 
	 * If nothing seems to be using this method and it seems to have outlived it's usefulness please feel free to delete it.
	 *
	 * @param matrix  double[][] to compute the hash code of 
	 * @return hashCode
	 */
	public static int matlabHashCode(double[][] matrix)
	{
		String s = Arrays.deepToString(matrix);
		if(s.length() > 250)
		{
			s = s.substring(0,249)+ "...";
		}
	
		//System.out.println("HASH=>" + s);
		return Math.abs(Hash.hash(matrix)) % 32462867;  //Some prime around 2^25 (to prevent overflows in computation)
		
		
		
		
	}
	
	/**
	 * Permutes a list of objects
	 * @param list list of objects to randomly permute
	 * @return a copy of the list with elements permuted
	 */
	public static <X> List<X> permute(List<X> list, Random rand)
	{
		List<X> newList = new ArrayList<X>(list.size());
		
		int[] perms = RandomUtil.getPermutation(list.size(), 0,rand);
		for(int i=0; i < list.size(); i++)
		{
			newList.add(list.get(perms[i]));
		}
		
		return newList;
	}
	
	/**
	 * Deep copies a matrix 
	 * @param matrix  matrix to copy
	 * @return a copy of the matrix
	 */
	public static double[][] copy(double[][] matrix)
	{
		double[][] newMatrix = new double[matrix.length][];
		for(int i=0; i < matrix.length; i++)
		{
			newMatrix[i] = matrix[i].clone();
		}
		return newMatrix;
	}

	/**
	 * Removes all NaN values from an array
	 * @param values a list of double values
	 * @return a copy of the values with all NaNs removed
	 */
	public static double[] stripNans(double[] values)
	{
		int count = 0;
		for(int i=0; i < values.length; i++)
		{
			if(!Double.isNaN(values[i]))
			{
				count++;
			}
		}
		
		
		double[] newVals = new double[count];
		count=0;
		for(int i=0; i < values.length; i++)
		{
			if(!Double.isNaN(values[i]))
			{
				newVals[count] = values[i];
				count++;
			}
		}
		
		return newVals;
		
		
	}
	
	/**
	 * Computes the mean of the values, ignoring NaNs
	 * 
	 * @param values values to compute the mean of
	 * @return mean
	 */
	public static double meanIgnoreNaNs(double[] values) {
		return StatUtils.mean(stripNans(values));
	}

	/**
	 * Computes the Standard Deviation of a list of values, ignoring NaNs
	 * @param values values to compute the standard deviation of
	 * @return standard deviation
	 */
	public static double stdDevIgnoreNaNs(double[] values) {
		return Math.sqrt(StatUtils.variance(stripNans(values)));
	}

	/**
	 * Normalizes values 
	 * 
	 * basically  each element in the output becomes (value - mean)/stdDev;
	 * @param values values to normalize
	 * @param mean   the mean of the values
	 * @param stdDev the standard deviation
	 * @return a copy of the normalized values.
	 */
	public static double[] normalize(double[] values, double mean, double stdDev) {
		values = values.clone();
		for(int i=0; i < values.length; i++)
		{
			if(Double.isNaN(values[i])) continue; 

			values[i] = (values[i] - mean);
			if(stdDev > 0)
			{
				values[i] /= stdDev;
			}
		}
		return values;
	}

	/**
	 * Computes the element wise absolute value of every element
	 * @param values elements to absolute value
	 * @return copy of the array with absolute value of elements
	 */
	public static double[] abs(double[] values) {
		values = values.clone();
		for(int i=0; i < values.length; i++)
		{
			values[i] = Math.abs(values[i]);
		}
		
		return values;
	}

	/**
	 * Returns the maximum value in the array.
	 * 
	 * @param values values to search
	 * @return maximum value
	 */
	public static double max(double[] values) {
		if(values.length == 0) return Double.NEGATIVE_INFINITY;
		double max = values[0];
		for(int i=1; i < values.length; i++)
		{
			if(values[i] > max)
			{
				max = values[i];
			}
		}
		
		return max;
		
	}

	
	/**
	 * Returns the maximum value in the array (ignoring NaNs)
	 * @param values values to search
	 * @return maximum values
	 */
	public static double maxIgnoreNaNs(double[] values) {
		return max(stripNans(values));
	}
	


	

}
