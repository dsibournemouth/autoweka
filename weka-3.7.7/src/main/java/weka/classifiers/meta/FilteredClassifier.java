/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    FilteredClassifier.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;

/**
 <!-- globalinfo-start -->
 * Class for running an arbitrary classifier on data that has been passed through an arbitrary filter. Like the classifier, the structure of the filter is based exclusively on the training data and test instances will be processed by the filter without changing their structure.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  eg: "weka.filters.unsupervised.attribute.Remove -V -R 1,2"</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class FilteredClassifier 
  extends SingleClassifierEnhancer 
  implements Drawable {

  /** for serialization */
  private static final long serialVersionUID = -7114868260359395521L;
  
  /** The filter */
  protected Filter m_Filter = new weka.filters.AllFilter();

  /** The instance structure of the filtered instances */
  protected Instances m_FilteredInstances;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
 */
  public String globalInfo() {
    return   "Class for running an arbitrary classifier on data that has been passed "
      + "through an arbitrary filter. Like the classifier, the structure of the filter "
      + "is based exclusively on the training data and test instances will be processed "
      + "by the filter without changing their structure.";
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    return "weka.classifiers.rules.ZeroR";
  }

  /**
   * Default constructor.
   */
  public FilteredClassifier() {
    m_Classifier = new weka.classifiers.rules.ZeroR();
    m_Filter = new weka.filters.AllFilter();
  }
  
  public FilteredClassifier(Classifier classifier, Filter filter) throws Exception {
    m_Classifier = AbstractClassifier.makeCopy(classifier);
    m_Filter = Filter.makeCopy(filter);
  }

  /**
   * Returns the type of graph this classifier
   * represents.
   *  
   * @return the graph type of this classifier
   */   
  public int graphType() {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graphType();
    else 
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @throws Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + getClassifierSpec()
			     + " cannot be graphed");
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);
    newVector.addElement(new Option(
	      "\tFull class name of filter to use, followed\n"
	      + "\tby filter options.\n"
	      + "\teg: \"weka.filters.unsupervised.attribute.Remove -V -R 1,2\"",
	      "F", 1, "-F <filter specification>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -F &lt;filter specification&gt;
   *  Full class name of filter to use, followed
   *  by filter options.
   *  eg: "weka.filters.unsupervised.attribute.Remove -V -R 1,2"</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.J48)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.trees.J48:
   * </pre>
   * 
   * <pre> -U
   *  Use unpruned tree.</pre>
   * 
   * <pre> -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)</pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)</pre>
   * 
   * <pre> -R
   *  Use reduced error pruning.</pre>
   * 
   * <pre> -N &lt;number of folds&gt;
   *  Set number of folds for reduced error
   *  pruning. One fold is used as pruning set.
   *  (default 3)</pre>
   * 
   * <pre> -B
   *  Use binary splits only.</pre>
   * 
   * <pre> -S
   *  Don't perform subtree raising.</pre>
   * 
   * <pre> -L
   *  Do not clean up after the tree has been built.</pre>
   * 
   * <pre> -A
   *  Laplace smoothing for predicted probabilities.</pre>
   * 
   * <pre> -Q &lt;seed&gt;
   *  Seed for random data shuffling (default 1).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    // Same for filter
    String filterString = Utils.getOption('F', options);
    if (filterString.length() > 0) {
      String [] filterSpec = Utils.splitOptions(filterString);
      if (filterSpec.length == 0) {
	throw new IllegalArgumentException("Invalid filter specification string");
      }
      String filterName = filterSpec[0];
      filterSpec[0] = "";
      setFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));
    } else {
      setFilter(new weka.filters.AllFilter());
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 2];
    int current = 0;

    options[current++] = "-F";
    options[current++] = "" + getFilterSpec();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);
    return options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTipText() {
    return "The filter to be used.";
  }

  /**
   * Sets the filter
   *
   * @param filter the filter with all options set.
   */
  public void setFilter(Filter filter) {

    m_Filter = filter;
  }

  /**
   * Gets the filter used.
   *
   * @return the filter
   */
  public Filter getFilter() {

    return m_Filter;
  }
  
  /**
   * Gets the filter specification string, which contains the class name of
   * the filter and any options to the filter
   *
   * @return the filter string.
   */
  protected String getFilterSpec() {
    
    Filter c = getFilter();
    if (c == null) {
      return "weka.filters.AllFilter";
    }
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities	result;
    
    if (getFilter() == null)
      result = super.getCapabilities();
    else
      result = getFilter().getCapabilities();
    
    // the filtered classifier always needs a class
    result.disable(Capability.NO_CLASS);
    
    // set dependencies
    for (Capability cap: Capability.values())
      result.enableDependency(cap);
    
    return result;
  }

  /**
   * Build the classifier on the filtered data.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifiers have been set!");
    }

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    /*
    String fname = m_Filter.getClass().getName();
    fname = fname.substring(fname.lastIndexOf('.') + 1);
    util.Timer t = util.Timer.getTimer("FilteredClassifier::" + fname);
    t.start();
    */
    m_Filter.setInputFormat(data);  // filter capabilities are checked here
    data = Filter.useFilter(data, m_Filter);
    //t.stop();

    // can classifier handle the data?
    getClassifier().getCapabilities().testWithFail(data);

    m_FilteredInstances = data.stringFreeStructure();
    m_Classifier.buildClassifier(data);
  }

  /**
   * Classifies a given instance after filtering.
   *
   * @param instance the instance to be classified
   * @return the class distribution for the given instance
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance)
    throws Exception {

    /*
      System.err.println("FilteredClassifier:: " 
                         + m_Filter.getClass().getName()
                         + " in: " + instance);
    */
    if (m_Filter.numPendingOutput() > 0) {
      throw new Exception("Filter output queue not empty!");
    }
    /*
    String fname = m_Filter.getClass().getName();
    fname = fname.substring(fname.lastIndexOf('.') + 1);
    util.Timer t = util.Timer.getTimer("FilteredClassifier::" + fname);
    t.start();
    */
    if (!m_Filter.input(instance)) {
      if (!m_Filter.mayRemoveInstanceAfterFirstBatchDone()) {
        throw new Exception("Filter didn't make the test instance"
            + " immediately available!");
      } else {
        // filter has consumed the instance (e.g. RemoveWithValues
        // may do this). We will indicate no prediction for this
        // instance
        double[] unclassified = null;
        if (instance.classAttribute().isNumeric()) {
          unclassified = new double[1];
          unclassified[0] = Utils.missingValue();
        } else {
          // all zeros
          unclassified = new double[instance.classAttribute().numValues()];
        }
        m_Filter.batchFinished();
        return unclassified;
      }
    }
    m_Filter.batchFinished();
    Instance newInstance = m_Filter.output();
    //t.stop();
    /*
    System.err.println("FilteredClassifier:: " 
                       + m_Filter.getClass().getName()
                       + " out: " + newInstance);
    */
    return m_Classifier.distributionForInstance(newInstance);
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a representation of this classifier
   */
  public String toString() {

    if (m_FilteredInstances == null) {
      return "FilteredClassifier: No model built yet.";
    }
    
    String classifierSpec = "";
    try {
      classifierSpec = getClassifierSpec();
    }
    catch(Exception e){
      classifierSpec = "[ERROR] " + e.getMessage();
    }
    
    String classifierString = "";
    try{
      classifierString = m_Classifier.toString();
    }
    catch(Exception e){
      classifierString = "[ERROR] " + e.getMessage();
    }
    
    String filterSpec = "";
    try{
      filterSpec = getFilterSpec();
    }
    catch(Exception e){
      filterSpec = "[ERROR] " + e.getMessage();
    }
    
    String filterString = "";
    try {
      filterString = m_FilteredInstances.toString();
    }
    catch(Exception e){
      filterString = "[ERROR] " + e.getMessage();
    }
    
    String result = "FilteredClassifier using "
      + classifierSpec
      + " on data filtered through "
      + filterSpec
      + "\n\nFiltered Header\n"
      + filterString
      + "\n\nClassifier Model\n"
      + classifierString;
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }
  
  /**
   * Creates a given number of deep copies of the given classifier using serialization.
   *
   * @param model the classifier to copy
   * @param num the number of classifier copies to create.
   * @return an array of classifiers.
   * @exception Exception if an error occurs
   */
  public static FilteredClassifier [] makeCopies(FilteredClassifier model, int num) throws Exception {

    if (model == null) {
      throw new Exception("No model classifier set");
    }
    FilteredClassifier [] classifiers = new FilteredClassifier [num];
    for(int i = 0; i < classifiers.length; i++) {
      classifiers[i] = new FilteredClassifier(model.m_Classifier, model.m_Filter);
    }
    return classifiers;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv)  {
    runClassifier(new FilteredClassifier(), argv);
  }
}
