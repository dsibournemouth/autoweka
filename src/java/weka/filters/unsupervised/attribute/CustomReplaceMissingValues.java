package weka.filters.unsupervised.attribute;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.UnsupervisedFilter;

public class CustomReplaceMissingValues extends PotentialClassIgnorer implements
		UnsupervisedFilter, OptionHandler {

	/** for serialization */
	private static final long serialVersionUID = -7002695636068379754L;

	private double[] m_ReplacementValues = null;

	public static final int REPLACE_NONE = 0;
	public static final int REPLACE_LAST_KNOWN_VALUE = 1;
	public static final int REPLACE_MEAN = 2;
	public static final int REPLACE_MEDIAN = 3;
	public static final int REPLACE_MAX = 4;
	public static final int REPLACE_MIN = 5;
	public static final int REPLACE_CONSTANT = 6;

	/** Attribute selection methods */
	public static final Tag[] TAGS_REPLACE = {
			new Tag(REPLACE_NONE, "No replacement"),
			new Tag(REPLACE_LAST_KNOWN_VALUE, "Last non-missing value"),
			new Tag(REPLACE_MEAN, "Mean"), new Tag(REPLACE_MEDIAN, "Median"),
			new Tag(REPLACE_MAX, "Maximum"), new Tag(REPLACE_MIN, "Minimum"),
			new Tag(REPLACE_CONSTANT, "Constant") };

	protected double m_ConstantValue = 0.0;
	
	/** The current replacement method */
	protected int m_ReplacementMethod = REPLACE_MEAN;

	
	/**
	 * Returns a string describing this filter
	 *
	 * @return a description of the filter suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {

		return "Replaces all missing values according to the selected strategy.";
	}
	
	@Override
	public Enumeration listOptions() {
		Vector result = new Vector();

		result.addElement(new Option("\tReplacement method (default: Mean)", "M", 1, "-M <num>"));
		
		result.addElement(new Option("\tConstant value (default: 0)", "C", 1, "-C <num>"));

		return result.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {
		String tmpStr;

		tmpStr = Utils.getOption('M', options);
		if (tmpStr.length() != 0){
			int method = Integer.parseInt(tmpStr);
			setReplacementMethod(new SelectedTag(method, TAGS_REPLACE));
		}
		else{
			setReplacementMethod(new SelectedTag(REPLACE_MEAN, TAGS_REPLACE));
		}
		
		tmpStr = Utils.getOption('C', options);
		if (tmpStr.length() != 0){
			setConstantValue(Double.parseDouble(tmpStr));
		}
		else{
			setConstantValue(0.0);
		}

	}

	@Override
	public String[] getOptions() {
		Vector<String> result;

		result = new Vector<String>();

		result.add("-M");
		result.add("" + getReplacementMethod());
		
		result.add("-C");
		result.add("" + getConstantValue());

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the Capabilities of this filter.
	 *
	 * @return the capabilities of this object
	 * @see Capabilities
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enableAllAttributes();
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enableAllClasses();
		result.enable(Capability.MISSING_CLASS_VALUES);
		result.enable(Capability.NO_CLASS);

		return result;
	}

	/**
	 * Sets the format of the input instances.
	 *
	 * @param instanceInfo
	 *            an Instances object containing the input instance structure
	 *            (any instances contained in the object are ignored - only the
	 *            structure is required).
	 * @return true if the outputFormat may be collected immediately
	 * @throws Exception
	 *             if the input format can't be set successfully
	 */
	public boolean setInputFormat(Instances instanceInfo) throws Exception {

		super.setInputFormat(instanceInfo);
		setOutputFormat(instanceInfo);
		m_ReplacementValues = null;
		return true;
	}

	/**
	 * Input an instance for filtering. Filter requires all training instances
	 * be read before producing output.
	 *
	 * @param instance
	 *            the input instance
	 * @return true if the filtered instance may now be collected with output().
	 * @throws IllegalStateException
	 *             if no input format has been set.
	 */
	public boolean input(Instance instance) {

		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}
		if (m_NewBatch) {
			resetQueue();
			m_NewBatch = false;
		}
		if (m_ReplacementValues == null) {
			bufferInput(instance);
			return false;
		} else {
			convertInstance(instance);
			return true;
		}
	}

	/**
	 * Signify that this batch of input to the filter is finished. If the filter
	 * requires all instances prior to filtering, output() may now be called to
	 * retrieve the filtered instances.
	 *
	 * @return true if there are instances pending output
	 * @throws IllegalStateException
	 *             if no input structure has been defined
	 */
	public boolean batchFinished() {

		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}

		if (m_ReplacementValues == null) {
			// Compute replacement value according to the selected strategy

			m_ReplacementValues = new double[getInputFormat().numAttributes()];
			for (int i = 0; i < getInputFormat().numAttributes(); i++) {
				if (getInputFormat().attribute(i).isNominal()) {
					// TODO
				} else if (getInputFormat().attribute(i).isNumeric()) {
					double[] values = getInputFormat()
							.attributeToDoubleArray(i);
					switch (m_ReplacementMethod) {
					case REPLACE_LAST_KNOWN_VALUE:
						m_ReplacementValues[i] = m_ConstantValue; // initialize in case first value is missing
						break;
					case REPLACE_MEAN:
						m_ReplacementValues[i] = Utils.mean(values);
						break;
					case REPLACE_MEDIAN:
						// answer from http://stackoverflow.com/a/11955900
						double[] tmpValues = values.clone();
						Arrays.sort(tmpValues);
						if (tmpValues.length % 2 == 0)
							m_ReplacementValues[i] = (tmpValues[tmpValues.length/2] + tmpValues[tmpValues.length/2 - 1])/2.0;
						else
							m_ReplacementValues[i] = tmpValues[tmpValues.length/2];
						break;
					case REPLACE_MAX:
						m_ReplacementValues[i] = values[Utils.maxIndex(values)];
						break;
					case REPLACE_MIN:
						m_ReplacementValues[i] = values[Utils.minIndex(values)];
						break;
					case REPLACE_CONSTANT:
						m_ReplacementValues[i] = m_ConstantValue;
						break;
					default:
						break;
					}
				}
			}

			// Convert pending input instances
			for (int i = 0; i < getInputFormat().numInstances(); i++) {
				Instance instance = getInputFormat().instance(i);
				
				if (m_ReplacementMethod == REPLACE_LAST_KNOWN_VALUE){
					for (int j = 0; j < getInputFormat().numAttributes(); j++) {
						// if not missing, update last known value
						if (!Utils.isMissingValue(instance.value(j)))
							m_ReplacementValues[j] = instance.value(j);
					}
				}
				convertInstance(instance);
			}
		}
		// Free memory
		flushInput();

		m_NewBatch = true;
		return (numPendingOutput() != 0);
	}

	/**
	 * Convert a single instance over. The converted instance is added to the
	 * end of the output queue.
	 *
	 * @param instance
	 *            the instance to convert
	 */
	private void convertInstance(Instance instance) {

		Instance inst = null;
		if (instance instanceof SparseInstance) {
			double[] vals = new double[instance.numValues()];
			int[] indices = new int[instance.numValues()];
			int num = 0;
			for (int j = 0; j < instance.numValues(); j++) {
				if (instance.isMissingSparse(j)
						&& (getInputFormat().classIndex() != instance.index(j))
						&& (instance.attributeSparse(j).isNominal() || instance
								.attributeSparse(j).isNumeric())) {
					if (m_ReplacementValues[instance.index(j)] != 0.0) {
						vals[num] = m_ReplacementValues[instance.index(j)];
						indices[num] = instance.index(j);
						num++;
					}
				} else {
					vals[num] = instance.valueSparse(j);
					indices[num] = instance.index(j);
					num++;
				}
			}
			if (num == instance.numValues()) {
				inst = new SparseInstance(instance.weight(), vals, indices,
						instance.numAttributes());
			} else {
				double[] tempVals = new double[num];
				int[] tempInd = new int[num];
				System.arraycopy(vals, 0, tempVals, 0, num);
				System.arraycopy(indices, 0, tempInd, 0, num);
				inst = new SparseInstance(instance.weight(), tempVals, tempInd,
						instance.numAttributes());
			}
		} else {
			double[] vals = new double[getInputFormat().numAttributes()];
			for (int j = 0; j < instance.numAttributes(); j++) {
				if (instance.isMissing(j)
						&& (getInputFormat().classIndex() != j)
						&& (getInputFormat().attribute(j).isNominal() || getInputFormat()
								.attribute(j).isNumeric())) {
					vals[j] = m_ReplacementValues[j];
				} else {
					vals[j] = instance.value(j);
				}
			}
			inst = new DenseInstance(instance.weight(), vals);
		}
		inst.setDataset(instance.dataset());
		push(inst);
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String replacementMethodTipText() {
		return "Set the method used to replace the missing values.";
	}

	/**
	 * Sets the method used to select attributes for use in the linear
	 * regression.
	 *
	 * @param method
	 *            the attribute selection method to use.
	 */
	public void setReplacementMethod(SelectedTag method) {

		if (method.getTags() == TAGS_REPLACE) {
			m_ReplacementMethod = method.getSelectedTag().getID();
		}
	}

	/**
	 * Gets the method used to select attributes for use in the linear
	 * regression.
	 *
	 * @return the method to use.
	 */
	public SelectedTag getReplacementMethod() {

		return new SelectedTag(m_ReplacementMethod, TAGS_REPLACE);
	}
	
	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String constantValueTipText() {
		return "Constant value to use when constant replacement is selected.";
	}

	/**
	 * Sets the method used to select attributes for use in the linear
	 * regression.
	 *
	 * @param method
	 *            the attribute selection method to use.
	 */
	public void setConstantValue(double value) {

		m_ConstantValue = value;
	}

	/**
	 * Gets the method used to select attributes for use in the linear
	 * regression.
	 *
	 * @return the method to use.
	 */
	public double getConstantValue() {

		return m_ConstantValue;
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: $");
	}

	/**
	 * Main method for testing this class.
	 *
	 * @param argv
	 *            should contain arguments to the filter: use -h for help
	 */
	public static void main(String[] argv) {
		runFilter(new CustomReplaceMissingValues(), argv);
	}

}
