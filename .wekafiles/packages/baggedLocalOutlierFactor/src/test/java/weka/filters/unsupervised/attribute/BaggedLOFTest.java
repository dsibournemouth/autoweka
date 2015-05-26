package weka.filters.unsupervised.attribute;


import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.test.Regression;

import java.io.BufferedReader;
import java.io.FileReader;

public class BaggedLOFTest extends AbstractFilterTest {

    public BaggedLOFTest(String name) {
        super(name);
    }

    /**
     * Used to create an instance of a specific filter. The filter
     * should be configured to operate on a dataset that contains
     * attributes in this order:<p>
     * <p/>
     * String, Nominal, Numeric, String, Nominal, Numeric<p>
     * <p/>
     * Where the first three attributes do not contain any missing values,
     * but the last three attributes do. If the filter is for some reason
     * incapable of accepting a dataset of this type, override setUp() to
     * either manipulate the default dataset to be compatible, or load another
     * test dataset. <p>
     * <p/>
     *
     * @return a suitably configured <code>Filter</code> value
     */

    @Override
    /**
     * LOF and BaggedLOF need a few particular settings since the test data sets are small.
     * In getFilter, follow what LOFTest does by choosing small values of LOF's min and max.
     * Must set the random seed to make it repeatable. Num-slots is just for fun, and should make it faster.
     */
    public Filter getFilter() {

        BaggedLOF blof = new BaggedLOF();
        try {
            blof.setOptions(new String[]{"-min", "5", "-max", "10", "-num-slots", "10",
                    "-randomSeed", "1"});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blof;

    }

    /**
     * Generates data for the FilteredClassifier test
     * Overriding the base class method because BaggedLOF needs a larger data set.
     *
     * @throws Exception if generation of data fails
     * @return the dataset for the FilteredClassifier
     */
    @Override
    protected Instances getFilteredClassifierData() throws Exception {
        TestInstances test;
        Instances result;

        // NB: in order to make sure that the classifier can handle the data,
        //     we're using the classifier's capabilities to generate the data.
        test = TestInstances.forCapabilities(
                m_FilteredClassifier.getClassifier().getCapabilities());
        test.setClassIndex(TestInstances.CLASS_IS_LAST);
        test.setNumInstances(100);

        result = test.generate();

        return result;
    }

    // Copies LOFTest in removing string attributes, since BaggedLOF defines
    // its capabilities to be equivalent to LOF's. (Ideally though, dates should be treated the same way as
    // strings, since both are ignored for distance calculations. As are class attributes.)
    protected void setUp() throws Exception {
        super.setUp();

        m_Instances.deleteAttributeType(Attribute.STRING);
    }

    /**
     * Test running a second batch of instances (identical to the first) through the filter.
     */

    public void testSecondBatchAgainstRegression() {

        Regression reg = new Regression(this.getClass());
        Instances result = useFilterTwice();
        reg.println(result.toString());
        try {
            String diff = reg.diff();
            if (diff == null) {
                System.err.println("Warning: No reference available, creating.");
            } else if (!diff.equals("")) {
                fail("Regression test failed. Difference:\n" + diff);
            }
        } catch (java.io.IOException ex) {
            fail("Problem during regression testing.\n" + ex);
        }
    }

    protected Instances useFilterTwice() {

        Instances result = null;
        Instances icopy = new Instances(m_Instances);
        try {
            m_Filter.setInputFormat(icopy);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
        }
        try {
            Instances result0 = Filter.useFilter(icopy, m_Filter);
            result = Filter.useFilter(icopy, m_Filter);
            assertNotNull(result);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception thrown on useFilter(): \n" + ex.getMessage());
        }
        return result;
    }
}
