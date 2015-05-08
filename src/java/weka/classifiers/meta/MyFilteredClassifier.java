package weka.classifiers.meta;

import weka.core.Utils;
import weka.filters.Filter;

/**
 * Extension of FilteredClassifier for changing the default parameters.
 * Otherwise, the checking of applicability can fail if filter or classifier are not compatible with data. 
 *
 */
public class MyFilteredClassifier extends FilteredClassifier {
	  /**
	   * String describing default classifier.
	   * 
	   * @return the default classifier classname
	   */
	@Override
	  protected String defaultClassifierString() {
	    
	    return "weka.classifiers.functions.LinearRegression";
	  }

	  /**
	   * Default constructor.
	   */
	  public MyFilteredClassifier() {
		  super();
	    super.m_Classifier = new weka.classifiers.functions.LinearRegression();
	    super.m_Filter = new weka.filters.AllFilter();
	  }
	  
	  @Override
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

		    //super.setOptions(options);
		  }
}
