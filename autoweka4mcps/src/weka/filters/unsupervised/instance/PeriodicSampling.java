package weka.filters.unsupervised.instance;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

public class PeriodicSampling extends Filter implements UnsupervisedFilter,
		OptionHandler {

	private static final long serialVersionUID = -3547779597523813500L;

	protected int m_Step = 1;

	/**
	 * Returns a string describing this classifier
	 * 
	 * @return a description of the classifier suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Produces a subsample by selecting one of every N instances.";
	}

	@Override
	public Enumeration listOptions() {
		Vector result = new Vector();

		result.addElement(new Option("\tStep (default 1 -- no sampling)", "N", 1, "-N <num>"));

		return result.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {
		String tmpStr;

		tmpStr = Utils.getOption('N', options);
		if (tmpStr.length() != 0)
			setStep(Integer.parseInt(tmpStr));
		else
			setStep(1);

	}

	@Override
	public String[] getOptions() {
		Vector<String> result;

		result = new Vector<String>();

		result.add("-N");
		result.add("" + getStep());

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String stepTipText() {
		return "The step used for selecting the instances.";
	}

	/**
	 * Gets the step.
	 *
	 * @return the step
	 */
	public int getStep() {
		return m_Step;
	}

	/**
	 * Sets the step
	 *
	 * @param step
	 *            the new step.
	 */
	public void setStep(int step) {
		if (step<1)
			step = 1;
		m_Step = step;
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
	 *             if no input structure has been defined
	 */
	public boolean input(Instance instance) {

		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}
		if (m_NewBatch) {
			resetQueue();
			m_NewBatch = false;
		}
		if (isFirstBatchDone()) {
			push(instance);
			return true;
		} else {
			bufferInput(instance);
			return false;
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

		if (!isFirstBatchDone()) {
			// Do the subsample, and clear the input instances.
			createSubsample();
		}
		flushInput();

		m_NewBatch = true;
		m_FirstBatchDone = true;
		return (numPendingOutput() != 0);
	}

	/**
	 * Creates a subsample of the current set of input instances. The output
	 * instances are pushed onto the output queue for collection.
	 */
	protected void createSubsample() {
		int origSize = getInputFormat().numInstances();

		for (int i = 0; i < origSize; i += m_Step) {
			push((Instance) getInputFormat().instance(i).copy());
		}
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
		runFilter(new PeriodicSampling(), argv);
	}

}
