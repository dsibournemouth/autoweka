package weka.filters.unsupervised.attribute;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.Instances;

public class InterquartileRangeWithClass extends InterquartileRange {

	private static final long serialVersionUID = 5893046871425264000L;

	/**
	 * Determines the output format based on the input format and returns this.
	 * In case the output format cannot be returned immediately, i.e.,
	 * hasImmediateOutputFormat() returns false, then this method will called
	 * from batchFinished() after the call of preprocess(Instances), in which,
	 * e.g., statistics for the actual processing step can be gathered.
	 *
	 * @param inputFormat
	 *            the input format to base the output format on
	 * @return the output format
	 * @throws Exception
	 *             in case the determination goes wrong
	 * @see #hasImmediateOutputFormat()
	 * @see #batchFinished()
	 */
	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
		ArrayList<Attribute> atts;
		ArrayList<String> values;
		Instances result;
		int i;

		// attributes must be numeric
		m_Attributes.setUpper(inputFormat.numAttributes() - 1);
		m_AttributeIndices = m_Attributes.getSelection();
		for (i = 0; i < m_AttributeIndices.length; i++) {
			// ignore class
//			if (m_AttributeIndices[i] == inputFormat.classIndex()) {
//				m_AttributeIndices[i] = NON_NUMERIC;
//				continue;
//			}
			// not numeric -> ignore it
			if (!inputFormat.attribute(m_AttributeIndices[i]).isNumeric())
				m_AttributeIndices[i] = NON_NUMERIC;
		}

		// get old attributes
		atts = new ArrayList<Attribute>();
		for (i = 0; i < inputFormat.numAttributes(); i++)
			atts.add(inputFormat.attribute(i));

		if (!getDetectionPerAttribute()) {
			m_OutlierAttributePosition = new int[1];
			m_OutlierAttributePosition[0] = atts.size();

			// add 2 new attributes
			values = new ArrayList<String>();
			values.add("no");
			values.add("yes");
			atts.add(new Attribute("Outlier", values));

			values = new ArrayList<String>();
			values.add("no");
			values.add("yes");
			atts.add(new Attribute("ExtremeValue", values));
		} else {
			m_OutlierAttributePosition = new int[m_AttributeIndices.length];

			for (i = 0; i < m_AttributeIndices.length; i++) {
				if (m_AttributeIndices[i] == NON_NUMERIC)
					continue;

				m_OutlierAttributePosition[i] = atts.size();

				// add new attributes
				values = new ArrayList<String>();
				values.add("no");
				values.add("yes");
				atts.add(new Attribute(inputFormat.attribute(
						m_AttributeIndices[i]).name()
						+ "_Outlier", values));

				values = new ArrayList<String>();
				values.add("no");
				values.add("yes");
				atts.add(new Attribute(inputFormat.attribute(
						m_AttributeIndices[i]).name()
						+ "_ExtremeValue", values));

				if (getOutputOffsetMultiplier())
					atts.add(new Attribute(inputFormat.attribute(
							m_AttributeIndices[i]).name()
							+ "_Offset"));
			}
		}

		// generate header
		result = new Instances(inputFormat.relationName(), atts, 0);
		result.setClassIndex(inputFormat.classIndex());

		return result;
	}
}
