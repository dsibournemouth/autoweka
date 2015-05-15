package weka.filters.unsupervised.instance;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.AllFilter;
import weka.filters.CategorizedMultiFilter;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.UnsupervisedFilter;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.filters.unsupervised.attribute.InterquartileRangeWithClass;
import weka.filters.unsupervised.attribute.RemoveByName;

public class RemoveOutliers extends MultiFilter implements UnsupervisedFilter,
		OptionHandler {

	/** for serialization */
	private static final long serialVersionUID = -8775970062618940385L;

	protected Filter m_OutlierDetectionFilter = new InterquartileRange();

	protected SubsetByExpression m_OutlierRemovalFilter = new SubsetByExpression();
	
	protected RemoveByName m_RemoveColumnsFilter = new RemoveByName();

	@Override
	public String[] getOptions() {
		Vector<String> result;


		result = new Vector<String>();
		
		result.add("-O");
		result.add(getFilterSpec(m_OutlierDetectionFilter));
		
		return result.toArray(new String[result.size()]);
	}

	@Override
	public Enumeration listOptions() {
		Vector<Option> result = new Vector<Option>();

		result.addElement(new Option("\tOutlier detection filter.", "O", 1,
				"-O <classname [options]>"));
		
		return result.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {
		String filter;
		String[] options2;

		super.setOptions(options);
		
		String outlierDetectionOption = Utils.getOption("O", options);
		if (outlierDetectionOption.length() > 0) {
			options2 = Utils.splitOptions(outlierDetectionOption);
			filter = options2[0];
			options2[0] = "";
			m_OutlierDetectionFilter = (Filter) Utils.forName(Filter.class, filter,
					options2);
		} else {
			m_OutlierDetectionFilter = new AllFilter();
		}
		
		updateFilters();

	}
	
	private void updateFilters() {
		String expression = "true";
		String classDetector = m_OutlierDetectionFilter.getClass().getName();
		
		Filter[] flow = {};
		
		if (classDetector.equals("weka.filters.unsupervised.attribute.InterquartileRange")){
			if (getInputFormat()!=null){
				int numAttributes = getInputFormat().numAttributes();
				expression = "ATT" + (numAttributes+1) + " is 'no'";
			}
			((InterquartileRange)m_OutlierDetectionFilter).setExtremeValuesAsOutliers(true); //force Extreme Values as Outliers
			m_OutlierRemovalFilter.setExpression(expression);
			m_RemoveColumnsFilter.setExpression("Outlier|ExtremeValue");
			
			flow = new Filter[3];
			flow[0] = m_OutlierDetectionFilter;
			flow[1] = m_OutlierRemovalFilter;
			flow[2] = m_RemoveColumnsFilter;
		}
		else if(classDetector.equals("weka.filters.unsupervised.attribute.LOF") || classDetector.equals("weka.filters.unsupervised.attribute.BaggedLOF")){
			
			InterquartileRangeWithClass IQRFilter = new InterquartileRangeWithClass();
			if (getInputFormat()!=null){
				int numAttributes = getInputFormat().numAttributes();

				IQRFilter.setAttributeIndices("last"); // LOF column
				IQRFilter.setExtremeValuesAsOutliers(true);
				expression = "ATT" + (numAttributes+2) + " is 'no'"; // Outlier column
			}
			m_OutlierRemovalFilter.setExpression(expression);
			m_RemoveColumnsFilter.setExpression("BaggedLOF|LOF|Outlier|ExtremeValue");
			
			flow = new Filter[4];
			flow[0] = m_OutlierDetectionFilter;
			flow[1] = IQRFilter;
			flow[2] = m_OutlierRemovalFilter;
			flow[3] = m_RemoveColumnsFilter;
		}
		
		setFilters(flow);
	}
	
	@Override
	protected Instances process(Instances instances) throws Exception {
		updateFilters();
		return super.process(instances);
	}
	
	public String outlierDetectionFilterTipText(){
		return "Outlier detection filter";
	}
	
	public Filter getOutlierDetectionFilter(){
		return m_OutlierDetectionFilter;
	}
	
	public void setOutlierDetectionFilter(Filter filter){
		m_OutlierDetectionFilter = filter;
		updateFilters();
	}


	/**
	 * Main method for executing this class.
	 * 
	 * @param args
	 *            should contain arguments for the filter: use -h for help
	 */
	public static void main(String[] args) {
		runFilter(new RemoveOutliers(), args);
	}

}
