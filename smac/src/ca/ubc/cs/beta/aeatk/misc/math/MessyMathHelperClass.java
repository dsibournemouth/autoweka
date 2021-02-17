package ca.ubc.cs.beta.aeatk.misc.math;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.six11.util.math.PCA.PrincipleComponent;
import org.apache.commons.math.stat.StatUtils;


/**
 * This class is a disaster that generates the MessyMathHelperClass of a matrix and does some other stuff
 * 
 * This should very much be cleaned up 
 * 
 * @author sjr
 *
 */
public class MessyMathHelperClass {

	public double[][] getPCA(double[][] matrix, int n)
	{
		
		
		org.six11.util.math.PCA pca = new org.six11.util.math.PCA(matrix);
		
		
		List<PrincipleComponent> pcaComp =  pca.getDominantComponents(pca.getNumComponents());
		
		
		double[][] pcaMatrix = new double[pcaComp.size()][];
		
		int i=0;
		for(PrincipleComponent p : pcaComp)
		{
			pcaMatrix[i] = p.eigenVector;
			i++;
		}
		
		
		pcaMatrix = flipxflipy(pcaMatrix);
		
		/*
		 * This use to be the following line
		 * 
		 * 
		 * It seemed to have bugs, I don't remember what this code is doing it looks like it's just copying
		 * 
		 * The Math.min() section use to just be 'n', but it seemed to cause an out of bounds check. I need to re-examine what this code is doing.
		 *  
		 * --scenarioFile /ubc/cs/project/arrow/seramage/smac-test/spear/spear-surrogate.txt --experimentDir /ubc/cs/project/arrow/seramage/smac-test/spear/ --skipInstanceFileCheck --seed 1337892636011
		 */
		
		double[][] pcaMatrixFinal = new double[matrix[0].length][Math.min(n, pcaMatrix[0].length) ];
		
		for(int j=0; j < Math.min(pcaMatrixFinal.length,pcaMatrix.length); j++)
		{	
			for(int k=0; k < Math.min(pcaMatrixFinal[j].length,pcaMatrix[j].length); k++)
			{
				pcaMatrixFinal[j][k] = pcaMatrix[j][k];
			}
		}
		
		return pcaMatrixFinal;
		
		//return pcaMatrix;
	}
	
	
	public double[][] flipxflipy(double[][] matrix)
	{
		double[][] flippedresult = new double[matrix.length][matrix[0].length];
		
		for(int i=0; i < matrix.length; i++)
		{
			for(int j = 0; j < matrix[0].length; j++)
			{
				flippedresult[i][j] = matrix[matrix.length - i - 1][matrix[0].length - j - 1];
			}
		}
		
		return flippedresult;
		
	}
	public double[] getPCACoeff(double[][] matrix, int n)
	{
		org.six11.util.math.PCA pca = new org.six11.util.math.PCA(matrix);
		
		List<PrincipleComponent> pcaComp =  pca.getDominantComponents(n);
		
		double[] pcaMatrix = new double[pcaComp.size()];
		
		int i=0;
		for(PrincipleComponent p : pcaComp)
		{
			pcaMatrix[i] = p.eigenValue;
			i++;
		}
		return pcaMatrix;
	}
	public static void main(String[] args)
	{
		
		
		MessyMathHelperClass pca = new MessyMathHelperClass();
		
		
		double [][] testscores = { {36, 62, 31, 76, 46, 12, 39, 30, 22, 9, 32, 40, 64,
            36, 24, 50, 42, 2, 56, 59, 28, 19, 36, 54, 14},
          {58, 54, 42, 78, 56, 42, 46, 51, 32, 40, 49, 62, 75,
           38, 46, 50, 42, 35, 53, 72, 50, 46, 56, 57, 35},
          {43, 50, 41, 69, 52, 38, 51, 54, 43, 47, 54, 51, 70,
           58, 44, 54, 52, 32, 42, 70, 50, 49, 56, 59, 38},
          {36, 46, 40, 66, 56, 38, 54, 52, 28, 30, 37, 40, 66,
           62, 55, 52, 38, 22, 40, 66, 42, 40, 54, 62, 29},
          {37, 52, 29, 81, 40, 28, 41, 32, 22, 24, 52, 49, 63,
           62, 49, 51, 50, 16, 32, 62, 63, 30, 52, 58, 20}};

		double[][] result = pca.getPCA(testscores, 7);
		
		
		System.out.println(Arrays.deepToString(testscores));
		System.out.println(Arrays.deepToString(result));
		System.out.println(Arrays.toString((pca.getPCACoeff(testscores, 7))));
		
		
		
	}

	public double[][] transpose(double[][] matrix)
	{
		return ArrayMathOps.transpose(matrix);
		/*
		double[][] transpose = new double[matrix[0].length][matrix.length];
		for(int i=0; i < transpose.length; i++)
		{
			for(int j=0; j < transpose[0].length; j++)
			{
				transpose[i][j] = matrix[j][i];
			}
		}
		
		return transpose;
		*/
	}
	
	public int[][] transpose(int[][] matrix)
	{
		int[][] transpose = new int[matrix[0].length][matrix.length];
		for(int i=0; i < transpose.length; i++)
		{
			for(int j=0; j < transpose[0].length; j++)
			{
				transpose[i][j] = matrix[j][i];
			}
		}
		
		return transpose;
		
	}
	public double[] getRowStdDev(double[][] matrix) {

		
		double[] stddev = new double[matrix.length];

		for(int i=0; i < stddev.length; i++)
		{
			stddev[i] = Math.sqrt(StatUtils.variance(matrix[i]));
		}
	
		return stddev;
	}
	
	public double[] getRowMeans(double[][] matrix) {
		double[] means = new double[matrix.length];
		for(int i=0; i < means.length; i++)
		{
			means[i] = StatUtils.mean(matrix[i]);
		}
	
		return means;
	}
	
	public int[] constantColumnsWithMissingValues(double[][] matrix)
	{
		
		if(matrix.length == 0)
		{
			return new int[0];
		}
		ArrayList<Integer> constColumnsList = new ArrayList<Integer>();
		
		
		for(int j=0; j < matrix[0].length; j++)
		{
			double[] values = new double[matrix.length];
			//List<Double> columnValues = new ArrayList<Double>();
			for (int i=0; i < matrix.length; i++)
			{
				if(matrix[i][j] == -512 || matrix[i][j] == -1024)
				{
					values[i] = Double.NaN; 
					
				} else
				{
					values[i] = matrix[i][j];
				}
				
				//System.out.println(j+ ":"+values[i]);
				
			}
			
			
			
			//double mean = ArrayMathOps.meanIgnoreNaNs(values);
			
			 //System.out.println("Mean: " + j + ":" + mean);
			double stdDev = ArrayMathOps.stdDevIgnoreNaNs(values);
			
			 //System.out.println("Stddev: " + j + ":" + stdDev);
			//double[] normalized=ArrayMathOps.normalize(values,mean, stdDev);
			
			
			
			/*
			for(double d : normalized)
			{
				if( j == 0)  System.out.println("Normalized: " +  d );
			}*/
			//double[] absNormal = ArrayMathOps.abs(normalized);
			
			//for(double d : absNormal)
			//{
			//	if( j == 48)  System.out.println("Abs Normalized: " +  d );
			//}
			//double max = ArrayMathOps.maxIgnoreNaNs(absNormal);
			
			//System.out.println("Max:" + j + ":" + stdDev);
			if(Double.isNaN(stdDev))
			{
				stdDev = Double.MIN_VALUE;
			}
			
			
			if(stdDev < Math.pow(10,-6))
			{
				constColumnsList.add(j);
			} else
			{
				//System.out.println("not constant: " + j);
			}

		}
		
		int[] constColumns = new int[constColumnsList.size()];
		for(int i=0; i < constColumnsList.size(); i++)
		{
			constColumns[i] = constColumnsList.get(i);
		}
		return constColumns;
		
	}
	
	
	
	/**
	 * Generic Constant Columns Operation
	 * (This is not the same as the matlab code)
	 * @param matrix
	 * @return return the indexes of columns that are constant
	 */
	/*
	public int[] constantColumns(double[][] matrix)
	{
		ArrayList<Integer> constColumnsList = new ArrayList<Integer>();
		
		
		for(int j=0; j < matrix[0].length; j++)
		{
			Set<Double> columnValues = new HashSet<Double>();
			for (int i=0; i < matrix.length; i++)
			{
				columnValues.add(matrix[i][j]);
			}
			
			if((columnValues.size() == 1) && (!columnValues.contains(-512.0)) && !columnValues.contains(-1024.0))
			{
				constColumnsList.add(j);
			}
		}
		
		int[] constColumns = new int[constColumnsList.size()];
		for(int i=0; i < constColumnsList.size(); i++)
		{
			constColumns[i] = constColumnsList.get(i);
		}
		return constColumns;
		
	}
	*/

	public Set<Integer> fromArray(int[] arr)
	{
		Set<Integer> vals = new HashSet<Integer>();
		for(int i=0; i < arr.length; i++)
		{
			vals.add(arr[i]);
		}
		return vals;
	}
	
	public double[][] copyMatrixAndKeepColumns(double[][] matrix, int[] columns)
	{
		Set<Integer> columnsToKeep = fromArray(columns);
		List<Integer> columnsToRemove = new ArrayList<Integer>(matrix[0].length);
		for(int i=0; i < matrix[0].length; i++)
		{
			if(columnsToKeep.contains(i)) continue;
			columnsToRemove.add(i);
		}
	
		
		int[] columnsToRemoveArr = new int[columnsToRemove.size()];
		for(int i=0; i < columnsToRemove.size(); i++)
		{
			columnsToRemoveArr[i] = columnsToRemove.get(i);
		}
		return copyMatrixAndRemoveColumns(matrix, columnsToRemoveArr);
	}
	/**
	 * Returns a double[][] with columns removed
	 * 
	 * @param matrix  array with no constant columns [MUST BE RECTANGULAR]
	 * @param columns	columns that we should remove 
	 * @return - NOTE: the return value will always be a different array than the input)
	 */
	public double[][] copyMatrixAndRemoveColumns(double[][] matrix , int[] columns)
	{
		
		double[][] outputMatrix;
		
		if(matrix.length == 0)
		{
			outputMatrix = new double[0][];
		} else if (matrix[0].length == 0)
		{
			outputMatrix = new double[matrix.length][0];
		} else
		{
		
		
			outputMatrix = new double[matrix.length][matrix[0].length - columns.length];
			
			int currentColumn = 0;
			Set<Integer> constantColumns = fromArray(columns);
			
			for(int j=0; j < matrix[0].length; j++)
			{
				if(constantColumns.contains(j)) continue;
				
				for(int i=0; i < matrix.length; i++)
				{
					outputMatrix[i][currentColumn] = matrix[i][j];
				}
				currentColumn++;
			}
		} 
	
		return outputMatrix;

	}

	public void log10(double[] responseValues) {
		for(int i=0; i < responseValues.length; i++)
		{
			responseValues[i] = Math.log10(responseValues[i]);
		}
	}

	//TODO Fix method name
	public int[] getSub(double[] stdDev) {

		List<Integer> idxs = new ArrayList<Integer>(stdDev.length);
		
		for(int i=0; i < stdDev.length; i++)
		{
			if(stdDev[i] > Math.pow(10, -5))
			{
				idxs.add(i); 
			}
		}
		
		int[] subs = new int[idxs.size()];
		for(int i=0; i < idxs.size(); i++)
		{
			subs[i] = idxs.get(i);
		}
		return subs;
	}

	public enum Operation
	{
		ADD(" + "),
		SUBTRACT(" - "),
		MULTIPLY(" x "),
		DIVIDE(" / ");
		
		Operation(String s)
		{
			//do nothing now
		}
		
		public double eval(double lop, double rightop)
		{
			switch(this)
			{
			case ADD:
				return lop + rightop;
			case SUBTRACT:
				return lop - rightop;
			case MULTIPLY:
				return lop * rightop;
			case DIVIDE:
				return lop / rightop;
			default:
				throw new IllegalStateException("Java is really stupid, this is UMPOSSIBLE!");
			}
		}
		
		
	}
	
	public void perColumnOperation(double[][] matrix, double[] rightop , Operation op) {
		
		if (matrix.length == 0) return;
		//It's not necessarily an error 
		
		if(matrix[0].length != rightop.length) 
		{
			throw new IllegalStateException("Columns in matrix do not equal number of right operands");
		}
		
		for(int i=0; i < matrix.length; i++)
		{
			for (int j=0; j < matrix[0].length; j++)
			{
				matrix[i][j] = op.eval(matrix[i][j], rightop[j]); 
			}
		}
		return;
		
		
	}

	public double[][] matrixMultiply(double[][] d1,
			double[][] d2) {
		
		if (d1[0].length != d2.length)
		{
			throw new IllegalArgumentException("Matrix size mismatch " + d1.length+" x " + d1[0].length  + " multiplied with "  + d2.length+ " x " + d2[0].length);
		}
		double[][] outputMatrix = new double[d1.length][d2[0].length];
		
		
		for(int i=0; i < d1.length; i++)
		{
			for(int j=0; j < d2[0].length; j++)
			{
				for(int k=0; k < d2.length; k++)
				{
					outputMatrix[i][j] += d1[i][k] *  d2[k][j];
				}
			}
		}
		
		
		return outputMatrix;
		
	}


	public void max(double[] responseValues, double minimumResponseValue) {
		for(int i=0; i < responseValues.length; i++)
		{
			responseValues[i] = Math.max(responseValues[i], minimumResponseValue);
		}
		return;
	}

	
}
