package org.six11.util.math;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;


/**
 * Support class for Principle Components Analysis. This does some light lifting, but the real work
 * is done in the Jama code.
 * 
 * @author Gabe Johnson <johnsogg@cmu.edu>
 */
public class PCA {

  /**
   * Run an example of MessyMathHelperClass using data from disk, or canned data. If no file is specified, the canned
   * data is hard-coded, and is the same as the 10-sample data from Lindsay Smith's fantastic MessyMathHelperClass
   * tutorial:
   * 
   * <pre>
   * 2.5 2.4
   * 0.5 0.7
   * 2.2 2.9
   * 1.9 2.2
   * 3.1 3.0
   * 2.3 2.7
   * 2 1.6
   * 1 1.1
   * 1.5 1.6
   * 1.1 0.9
   * </pre>
   * 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    double[][] matrix;
    String source = null;
    if (args.length > 0) {
      source = "from disk";
      List<List<Double>> data = new ArrayList<List<Double>>();
      File inFile = new File(args[0]);
      BufferedReader br = new BufferedReader(new FileReader(inFile));
      int numElements = -1;
      while (br.ready()) {
        StringTokenizer line = new StringTokenizer(br.readLine());
        List<Double> vec = new ArrayList<Double>();
        data.add(vec);
        while (line.hasMoreTokens()) {
          vec.add(Double.parseDouble(line.nextToken()));
        }
        numElements = vec.size();
      }
      matrix = new double[data.size()][numElements];
      for (int i = 0; i < data.size(); i++) {
        List<Double> vec = data.get(i);
        for (int j = 0; j < vec.size(); j++) {
          matrix[i][j] = vec.get(j);
        }
      }
      br.close();
    } else {
      source = "from the Smith MessyMathHelperClass tutorial";
      matrix = new double[][] {
          {
              2.5, 2.4
          }, {
              0.5, 0.7
          }, {
              2.2, 2.9
          }, {
              1.9, 2.2
          }, {
              3.1, 3.0
          }, {
              2.3, 2.7
          }, {
              2, 1.6
          }, {
              1, 1.1
          }, {
              1.5, 1.6
          }, {
              1.1, 0.9
          }
      };
      bug(" **");
      bug(" ** Note: You can put data in a plain text file and mention its ");
      bug(" ** name on the command line if you want to process different data.");
      bug(" ** Just put each sample vector on a row, with scalar values whitespace-separated.");
      bug(" ** Example of three samples with 2 dimensions:");
      bug(" **");
      bug(" ** 2.3 9.01");
      bug(" ** 2.5 8.604");
      bug(" ** 2.22 8.24");
      bug(" **");
      bug(" ** ALSO: note the eigenvalues in the Smith tutorial have different sign than the");
      bug(" ** eigenvalues calculated here. This is not a problem, because the orientation should");
      bug(" ** not matter as long as the orientations are used consistently.");
    }
    Matrix originalData = new Matrix(matrix);
    bug("Here is the original data from " + source + ", before adjusting by each dimension mean:");
    originalData.print(8, 4);

    PCA pca = new PCA(matrix);

    int numComponents = pca.getNumComponents();
    bug("There are " + numComponents + " components");
    int k = 2;
    List<PrincipleComponent> mainComponents = pca.getDominantComponents(k);
    int counter = 1;
    bug("Showing top " + k + " principle components.");
    for (PrincipleComponent pc : mainComponents) {
      bug("Component " + (counter++) + ": " + pc);
    }
    Matrix features = PCA.getDominantComponentsMatrix(mainComponents);
    bug("Feature matrix (k=" + k + ") :");
    features.print(8, 4);

    Matrix featuresXpose = features.transpose();
    bug("Xposed feature matrix (k=" + k + ") :");
    featuresXpose.print(8, 4);

    double[][] matrixAdjusted = PCA.getMeanAdjusted(matrix, pca.getMeans());
    Matrix adjustedInput = new Matrix(matrixAdjusted);
    bug("Original input adjusted by dimension means (k=" + k + ") :");
    adjustedInput.print(8, 4);
    Matrix xformedData = featuresXpose.times(adjustedInput.transpose());
    bug("Transformed data into MessyMathHelperClass-space (k=" + k + ") :");
    xformedData.transpose().print(8, 4);

    k = 1;
    mainComponents = pca.getDominantComponents(k);
    counter = 1;
    bug("Showing top " + k + " principle components.");
    for (PrincipleComponent pc : mainComponents) {
      bug("Component " + (counter++) + ": " + pc);
    }
    features = PCA.getDominantComponentsMatrix(mainComponents);
    bug("Feature matrix (k=" + k + ") :");
    features.print(8, 4);

    featuresXpose = features.transpose();
    bug("Xposed feature matrix (k=" + k + ") :");
    featuresXpose.print(8, 4);

    matrixAdjusted = PCA.getMeanAdjusted(matrix, pca.getMeans());
    adjustedInput = new Matrix(matrixAdjusted);
    bug("Original input adjusted by dimension means (k=" + k + ") :");
    adjustedInput.print(8, 4);
    xformedData = featuresXpose.times(adjustedInput.transpose());
    bug("Transformed data into MessyMathHelperClass-space (k=" + k + ") :");
    xformedData.transpose().print(8, 4);

  }

  Matrix covMatrix;
  EigenvalueDecomposition eigenstuff;
  double[] eigenvalues;
  Matrix eigenvectors;
  SortedSet<PrincipleComponent> principleComponents;
  double[] means;

  public PCA(double[][] input) {
    means = new double[input[0].length];
    double[][] cov = getCovariance(input, means);
    covMatrix = new Matrix(cov);
    eigenstuff = covMatrix.eig();
    eigenvalues = eigenstuff.getRealEigenvalues();
    eigenvectors = eigenstuff.getV();
    double[][] vecs = eigenvectors.getArray();
    int numComponents = eigenvectors.getColumnDimension(); // same as num rows.
    principleComponents = new TreeSet<PrincipleComponent>();
    for (int i = 0; i < numComponents; i++) {
      double[] eigenvector = new double[numComponents];
      for (int j = 0; j < numComponents; j++) {
        eigenvector[j] = vecs[i][j];
      }
      principleComponents.add(new PrincipleComponent(eigenvalues[i], eigenvector));
    }
  }

  public double[] getMeans() {
    return means;
  }

  /**
   * Subtracts the mean value from each column. The means must be precomputed, which you get for
   * free when you make a MessyMathHelperClass instance (just call getMeans()).
   * 
   * @param input
   *          Some data, where each row is a sample point, and each column is a dimension.
   * @param mean
   *          The means of each dimension. This could be computed from 'input' directly, but for
   *          efficiency's sake, it should only be done once and the result saved.
   * @return Returns a translated matrix where each cell has been translated by the mean value of
   *         its dimension.
   */
  public static double[][] getMeanAdjusted(double[][] input, double[] mean) {
    int nRows = input.length;
    int nCols = input[0].length;
    double[][] ret = new double[nRows][nCols];
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nCols; col++) {
        ret[row][col] = input[row][col] - mean[col];
      }
    }
    return ret;
  }

  /**
   * Returns the top n principle components in descending order of relevance.
   */
  public List<PrincipleComponent> getDominantComponents(int n) {
    List<PrincipleComponent> ret = new ArrayList<PrincipleComponent>();
    int count = 0;
    for (PrincipleComponent pc : principleComponents) {
      ret.add(pc);
      count++;
      if (count >= n) {
        break;
      }
    }
    return ret;
  }

  public static Matrix getDominantComponentsMatrix(List<PrincipleComponent> dom) {
    int nRows = dom.get(0).eigenVector.length;
    int nCols = dom.size();
    Matrix matrix = new Matrix(nRows, nCols);
    for (int col = 0; col < nCols; col++) {
      for (int row = 0; row < nRows; row++) {
        matrix.set(row, col, dom.get(col).eigenVector[row]);
      }
    }
    return matrix;
  }

  public int getNumComponents() {
    return eigenvalues.length;
  }

  public static class PrincipleComponent implements Comparable<PrincipleComponent> {
    public double eigenValue;
    public double[] eigenVector;

    public PrincipleComponent(double eigenValue, double[] eigenVector) {
      this.eigenValue = eigenValue;
      this.eigenVector = eigenVector;
    }

    public int compareTo(PrincipleComponent o) {
      int ret = 0;
      if (eigenValue > o.eigenValue) {
        ret = -1;
      } else if (eigenValue < o.eigenValue) {
        ret = 1;
      }
      return ret;
    }

    public String toString() {
      return "Principle Component, eigenvalue: " + eigenValue + ", eigenvector: ["
          + Arrays.toString(eigenVector) + "]";
    }
  }

  public static double[][] getCovariance(double[][] input, double[] meanValues) {
    int numDataVectors = input.length;
    int n = input[0].length;

    double[] sum = new double[n];
    double[] mean = new double[n];
    for (int i = 0; i < numDataVectors; i++) {
      double[] vec = input[i];
      for (int j = 0; j < n; j++) {
        sum[j] = sum[j] + vec[j];
      }
    }
    for (int i = 0; i < sum.length; i++) {
      mean[i] = sum[i] / numDataVectors;
    }

    double[][] ret = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = i; j < n; j++) {
        double v = getCovariance(input, i, j, mean);
        ret[i][j] = v;
        ret[j][i] = v;
      }
    }
    if (meanValues != null) {
      System.arraycopy(mean, 0, meanValues, 0, mean.length);
    }
    return ret;
  }

  /**
   * Gives covariance between vectors in an n-dimensional space. The two input arrays store values
   * with the mean already subtracted. Read the code.
   */
  private static double getCovariance(double[][] matrix, int colA, int colB, double[] mean) {
    double sum = 0;
    for (int i = 0; i < matrix.length; i++) {
      double v1 = matrix[i][colA] - mean[colA];
      double v2 = matrix[i][colB] - mean[colB];
      sum = sum + (v1 * v2);
    }
    int n = matrix.length;
    double ret = (sum / (n - 1));
    return ret;
  }

  private static void bug(String what) {
    System.out.println(what);
  }

}
