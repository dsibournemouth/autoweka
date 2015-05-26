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

/**
 *   BaggedLOF.java
 */

package weka.filters.unsupervised.attribute;

import weka.core.*;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
<!-- globalinfo-start -->
* A filter that applies the Bagged LOF (Local Outlier Factor) algorithm to compute an "outlier" score for each instance in the data.The filter appends to the input data a new attribute called "BaggedLOF".<br/>
* <br/>
* For more information, see:<br/>
* <br/>
* Aleksandar Lazarevic, Vipin Kumar: Feature Bagging for Outlier Detection. In: Proceedings of the Eleventh ACM SIGKDD International Conference on Knowledge Discovery in Data Mining, New York, NY, USA, 157-166, 2005.
* <p/>
<!-- globalinfo-end -->
 *
<!-- technical-bibtex-start -->
* BibTeX:
* <pre>
* &#64;conference{Lazarevic2005,
*    address = {New York, NY, USA},
*    author = {Aleksandar Lazarevic and Vipin Kumar},
*    booktitle = {Proceedings of the Eleventh ACM SIGKDD International Conference on Knowledge Discovery in Data Mining},
*    pages = {157-166},
*    publisher = {ACM},
*    series = {KDD '05},
*    title = {Feature Bagging for Outlier Detection},
*    year = {2005},
*    location = {Chicago, Illinois, USA},
*    URL = {http://doi.acm.org/10.1145/1081870.1081891}
* }
* </pre>
* <p/>
<!-- technical-bibtex-end -->

 *
 * Feature bagging is implemented like in Lazarevic & Kumar:
 * <ul>
 * <li>By default, 10 rounds of bagging.</li>
 * <li>Each bag uses d/2 to (d-1) attributes (number chosen uniformly), where d is the number of attributes in the input data</li>
 * <li>To compute d, BaggedLOF only counts attributes that would be used inside LOF's distance calculation:
 * non-class attributes that are nominal or numeric.</li>
 * <li>Features are chosen uniformly without replacement.</li>
 * <li>Two methods are available for combining the scores across runs: cumulative sum (default) or breadth first.</li>
 *</ul>
 * <p>
 * Filter options include three specific to BaggedLOF -- -numBags,
 * -breadthFirst, and -randomSeed -- as well as everything accepted by LOF
 * or by the superclass, SimpleBatchFilter.</p>
 *<p>
 * Try setting -num-slots to a value of 10 or higher for faster runtimes using
 * multi-threading.</p>
 *
<!-- options-start -->
* Valid options are: <p/>
* 
* <pre> -numBags &lt;num&gt;
*  Number of bags to use
*  (default = 10)</pre>
* 
* <pre> -breadthFirst
*  Combine scores from the bags using 'Breadth First' method
*  (default = 'Cumulative Sum')</pre>
* 
* <pre> -randomSeed &lt;num&gt;
*  Random seed value (optional integer, for repeatable debugging)</pre>
* 
* <pre> -min &lt;num&gt;
*  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
*  (default = 10)</pre>
* 
* <pre> -max &lt;num&gt;
*  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
*  (default = 40)</pre>
* 
* <pre> -A
*  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
* </pre>
* 
* <pre> -num-slots &lt;num&gt;
*  Number of execution slots.
*  (default 1 - i.e. no parallelism)</pre>
* 
* <pre> -output-debug-info
*  If set, filter is run in debug mode and
*  may output additional info to the console</pre>
* 
* <pre> -do-not-check-capabilities
*  If set, filter capabilities are not checked when input format is set
*  (use with caution).</pre>
* 
<!-- options-end -->
 *
 *
 *
 * @author Lisa Friedland (lisadfriedland@gmail.com)
 * @version $Revision: 1 $
 * @see weka.filters.unsupervised.attribute.LOF
 */
public class BaggedLOF extends SimpleBatchFilter
        implements OptionHandler, TechnicalInformationHandler, RevisionHandler {

    // Options (set to defaults)
    protected int m_num_bags = 10;
    protected boolean m_breadth_first = false;
    protected String[] m_lof_options = new String[]{};
    protected boolean m_using_random_seed = false;
    protected int m_randomSeed;

    // Stored member variables
    protected LOF m_singleTrainedLOF;
    protected LOF[] m_trainedLOFs;
    protected int[][] m_attrSubsets;
    protected int[] m_attrSubsetCutoffs;
    protected Random m_randomness;    // defaults to null

    private static final long serialVersionUID = -6688400061436319833L; // required since class extends Serializable

    /**
     * Processes the given data (may change the provided dataset) and returns the
     * modified version. This method is called in batchFinished().
     *
     * @param instances the data to process
     * @return the modified data
     * @throws Exception in case the processing goes wrong
     * @see #batchFinished()
     */
    @Override
    protected Instances process(Instances instances) throws Exception {

        if (!isFirstBatchDone()) {
            // first time through

            if (m_using_random_seed)        // reset the random generator every time we build a new model, so output is consistent
                setRandomSeed(m_randomSeed);

            return runBaggedLOFSingleDataset(instances, m_num_bags, m_randomness);
        } else {
            return addBaggedLOFAttributeUsingExistingModel(instances);
        }

    }

    protected Instances filterOutIgnoredAttributes(Instances data) {
        // Make a copy of data in which we keep only attributes that will be used in distance calcs & LOF.
        Instances dataWithActiveAttrs = new Instances(data);
        for (int attrInd = dataWithActiveAttrs.numAttributes() - 1; attrInd >= 0; attrInd--) {
            Attribute attr = dataWithActiveAttrs.attribute(attrInd);
            if (!isAttributeUsedInDistanceFunction(attr) || attrInd == dataWithActiveAttrs.classIndex()) {

                // if it's the class attribute, have to do extra work to get rid of it
                if (attrInd == dataWithActiveAttrs.classIndex()) {
                    dataWithActiveAttrs.setClassIndex(-1);   // i.e., unset the class attribute
                }
                dataWithActiveAttrs.deleteAttributeAt(attrInd);
            }
        }
        return dataWithActiveAttrs;
    }


    /**
     * It would be better to determine this dynamically, but right now this just
     * replicates what happens inside LOF, which uses a NormalizableDistance, which
     * ignores all other attribute types.
     * @param attr
     * @return
     */
    protected boolean isAttributeUsedInDistanceFunction(Attribute attr) {
        if (attr.type() == Attribute.NOMINAL || attr.type() == Attribute.NUMERIC)
            return true;
        else
            return false;
    }


    /**
     * Implemented like Lazarevic & Kumar:
     * -By default, 10 rounds of bagging (as per their Fig 11)
     * -Each bag: d/2 to (d-1) features (choose number uniformly)
     * -Choose features uniformly w/o replacement
     * -Combining: two methods possible: cumulative sum (default) or breadth first
     *
     * randomness argument is allowed to be null (used for debugging).
     */
    protected Instances runBaggedLOFSingleDataset(Instances data, int numTrials, Random randomness) throws Exception {

        // Make a copy of data in which we keep only attributes that will be used in distance calcs & LOF.
        Instances dataWithActiveAttrs = filterOutIgnoredAttributes(data);
        int numActiveAttrs = dataWithActiveAttrs.numAttributes();
        if (numActiveAttrs <= 1) {
            // There's really no bagging that can happen
            if (numActiveAttrs == 1) {
                // Run single LOF on original data (knowing only 1 attribute will be used)
                return addLOFAttributeToSingleDataset(data);
            } else {
                // 0 meaningful attributes
                throw new Exception("Can't run BaggedLOF: data contains only contains attributes that are ignored");
            }
        }

        int minNumAttrs = (int) Math.ceil(numActiveAttrs / 2.0);
        int maxNumAttrs = numActiveAttrs - 1;

        m_trainedLOFs = new LOF[numTrials];
        m_attrSubsets = new int[numTrials][];
        m_attrSubsetCutoffs = new int[numTrials];

        Instances[] LOFRuns = new Instances[numTrials];

        if (randomness == null)
            randomness = new Random();

        for (int t = 0; t < numTrials; t++) {
            // Choose random number of attributes
            int numAttrsToUse = minNumAttrs + randomness.nextInt(maxNumAttrs + 1 - minNumAttrs);

            // Choose attributes
            // in this array, 0 to (numAttrsToUse - 1) are the (0-based indices of) ones to keep
            int[] attrIndicesPlus = sampleWithoutReplacement(numAttrsToUse, numActiveAttrs, randomness);

            // Prepare an Instances having just those attributes plus the class label
            Instances instancesWithAttrSubset = subsetUsingTheseAttributes(dataWithActiveAttrs, attrIndicesPlus, numAttrsToUse);

            // Run LOF
            LOFRuns[t] = addLOFAttributeToSingleDataset(instancesWithAttrSubset);

            // Store for future use
            m_attrSubsets[t] = attrIndicesPlus;
            m_attrSubsetCutoffs[t] = numAttrsToUse;
            m_trainedLOFs[t] = m_singleTrainedLOF;
            
            if(Thread.interrupted())
      		break;

        }

        // Now, combine the scores and append the new attribute to the original data.
        if (m_breadth_first) {
            data = combineScoresBreadthFirst(data, LOFRuns, "BaggedLOF");
        } else {
            data = combineScoresCumulativeSum(data, LOFRuns, "BaggedLOF");
        }

        return data;
    }

    /**
     * Helper function that actually calls LOF. Sets member m_singleTrainedLOF.
     *
     * @param allData
     * @return
     * @throws Exception
     */
    protected Instances addLOFAttributeToSingleDataset(Instances allData) throws Exception {

        LOF lof = new LOF();

        // note: setOptions modifies its argument. So, only send in a copy of m_lof_options.
        lof.setOptions(m_lof_options.clone());
        lof.setInputFormat(allData);
        Instances newData = Filter.useFilter(allData, lof);

        // Store this in case we want to re-use it
        m_singleTrainedLOF = lof;

        return newData;
    }

    protected Instances addBaggedLOFAttributeUsingExistingModel(Instances testData) throws Exception {
        if (m_trainedLOFs.length == 0 ||
                (m_trainedLOFs.length != m_attrSubsets.length) || (m_trainedLOFs.length != m_attrSubsetCutoffs.length)) {
            throw new Exception("Bagged LOF models don't seem to have been trained");
        }

        Instances dataWithActiveAttrs = filterOutIgnoredAttributes(testData);
        int numActiveAttrs = dataWithActiveAttrs.numAttributes();
        if (numActiveAttrs != m_attrSubsets[0].length) {
            // Throw an error if the number of active attributes differs between runs.
            // -If only ignored attributes differ, like a class attribute that isn't always present, not an error.
            // -Though note: if a particular nominal attribute is only sometimes declared as the class attribute, error will be triggered.
            throw new Exception("Can't use BaggedLOF on data with a different number of attributes than model was trained with");
        }
        if (numActiveAttrs == 1) {
            // There's really no bagging that can happen. Run plain LOF.
            if (m_singleTrainedLOF == null) {
                throw new Exception("In BaggedLOF: LOF model hasn't been trained");
            } else {
                return Filter.useFilter(testData, m_singleTrainedLOF);
            }
        }

        
        int numTrials = m_trainedLOFs.length;
        Instances[] LOFRuns = new Instances[numTrials];

        for (int t = 0; t < numTrials; t++) {

            // take appropriate subset of attributes
            Instances instancesWithAttrSubset = subsetUsingTheseAttributes(dataWithActiveAttrs, m_attrSubsets[t], m_attrSubsetCutoffs[t]);

            // run LOF
            LOFRuns[t] = Filter.useFilter(instancesWithAttrSubset, m_trainedLOFs[t]);
        }

        // Now, combine the scores. Adds an attribute to testData (as well as to instances within LOFRuns).
        if (m_breadth_first) {
            testData = combineScoresBreadthFirst(testData, LOFRuns, "BaggedLOF");
        } else {
            testData = combineScoresCumulativeSum(testData, LOFRuns, "BaggedLOF");
        }

        return testData;
    }


    protected Instances subsetUsingTheseAttributes(Instances data, int[] attrSubset, int numAttrsToUse) {

        // Prepare an Instances having just those attributes plus the class label
        Instances instancesWithAttrSubset = new Instances(data);

        // delete the attributes we're not keeping.
        // How to delete them in a consistent order? Let's do the following (though inefficient):
        // for attrNum = numAttrs to 1:
        //      see if it's in the "keep" or "delete" list.
        //      delete if you're supposed to.

        for (int i = attrSubset.length - 1; i >= 0; i--) {    // 0-based attr num
            boolean deleteMe = false;
            for (int j = numAttrsToUse; j < attrSubset.length; j++) {  // 0-based index into attrIndicesPlus
                if (attrSubset[j] == i) {
                    deleteMe = true;
                    break;
                }
            }
            if (deleteMe) {
                instancesWithAttrSubset.deleteAttributeAt(i);
            }

        }
        return instancesWithAttrSubset;
    }


    /**
     * For each Instances in lofRuns, take its LOF attribute (in final position).
     * Combine them by summing them all.
     * Append the new score to data as an attribute.
     *
     * @param data
     * @param lofRuns
     * @return
     */
    protected Instances combineScoresCumulativeSum(Instances data, Instances[] lofRuns, String newAttrName) {

        Instances newData = new Instances(data);

        Attribute sumLOFAttr = new Attribute(newAttrName);
        newData.insertAttributeAt(sumLOFAttr, newData.numAttributes());
        for (int i = 0; i < newData.numInstances(); i++) {
            Instance inst = newData.instance(i);

            double sumLOF = 0;
            for (int t = 0; t < lofRuns.length; t++) {
                sumLOF += lofRuns[t].instance(i).value(lofRuns[t].numAttributes() - 1);
            }
            inst.setValue(newData.numAttributes() - 1, sumLOF);
            
            if(Thread.interrupted())
    		break;
        }
        return newData;
    }


    /**
     * For each Instances in lofRuns, take its LOF attribute.
     * Combine them using breadth first search. (Taking first element in one ranking, first in the next, etc.)
     * Append the new score to data as an attribute.
     * Also adds an attribute to all instances within LOFRuns.
     *
     * @param data
     * @param lofRuns
     * @return
     */
    protected Instances combineScoresBreadthFirst(Instances data, Instances[] lofRuns, String newAttrName) {

        // Number the items in each lofRun 0 to data.numInstances()-1 so that we can recognize them later
        // (Note: in each run, items are in the same order, so each is numbered the same in each run)
        Attribute itemIndexAttr = new Attribute("itemIndex");
        for (int t = 0; t < lofRuns.length; t++) {
            lofRuns[t].insertAttributeAt(itemIndexAttr, lofRuns[t].numAttributes());
            int counter = 0;
            for (int i = 0; i < data.numInstances(); i++) {
                lofRuns[t].instance(i).setValue(lofRuns[t].numAttributes() - 1, counter);
                counter++;
            }
            
            if(Thread.interrupted())
    		break;
        }

        for (int t = 0; t < lofRuns.length; t++) {
            lofRuns[t].sort(lofRuns[t].numAttributes() - 2);    // sorts by ascending order of LOF
        }

        Instances newData = new Instances(data);
        Attribute newLOFAttr = new Attribute(newAttrName);
        newData.insertAttributeAt(newLOFAttr, newData.numAttributes());

        boolean[] inFinalRanking = new boolean[data.numInstances()];
        int finalRank = data.numInstances();      // higher needs to be more outlier-ish, so start high and count down to 1


        for (int posInMiniRanking = data.numInstances() - 1; posInMiniRanking >= 0; posInMiniRanking--) {
            for (int t = 0; t < lofRuns.length; t++) {

                // Have we used item posInMiniRanking in lofRuns[t] yet?
                int itemIndex = (int) lofRuns[t].instance(posInMiniRanking).value(lofRuns[t].numAttributes() - 1);

                if (!inFinalRanking[itemIndex]) {
                    // get to itemIndex in newData
                    Instance inst = newData.instance(itemIndex);
                    // assign it finalRank
                    inst.setValue(newData.numAttributes() - 1, finalRank);

                    // update finalRank and inFinalRanking
                    finalRank--;
                    inFinalRanking[itemIndex] = true;

                }
                if(Thread.interrupted())
        		break;
            }
        }

        return newData;
    }

    // Code taken from http://introcs.cs.princeton.edu/java/14array/Sample.java.html
    // Returns an array of 0-based indices. The sample is found in the first numElements of that array.
    protected int[] sampleWithoutReplacement(int numWanted, int numElements, Random randomness) {

        // initialize permutation to 0, ..., numElements-1
        int[] perm = new int[numElements];
        for (int i = 0; i < numElements; i++)
            perm[i] = i;

        // create random sample in perm[0], perm[1], ..., perm[numWanted-1]
        for (int i = 0; i < numWanted; i++) {

            // random integer between i and numElements-1
            int r = i + randomness.nextInt(numElements - i);

            // swap elements at indices i and r
            int t = perm[r];
            perm[r] = perm[i];
            perm[i] = t;
        }

        return (perm);

    }

    /**
     * BaggedLOF can use any data that LOF can use.
     *
     * @return the capabilities of this object
     * @see weka.core.Capabilities
     */
    @Override
    public Capabilities getCapabilities() {
        return new LOF().getCapabilities();
    }


    /**
     * Returns a string describing this filter.
     *
     * @return a description of the filter suitable for displaying in the
     *         explorer/experimenter gui
     */
    @Override
    public String globalInfo() {
        return "A filter that applies the Bagged LOF (Local Outlier Factor) algorithm "
                + "to compute an \"outlier\" score for each instance in the data. "
                + "The filter appends to the input data a new attribute called \"BaggedLOF\"."
                + "\n\nFor more information, see:\n\n"
                + getTechnicalInformation().toString();
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     *
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;

        result = new TechnicalInformation(TechnicalInformation.Type.CONFERENCE);
        result.setValue(TechnicalInformation.Field.AUTHOR, "Aleksandar Lazarevic and Vipin Kumar");
        result.setValue(TechnicalInformation.Field.TITLE, "Feature Bagging for Outlier Detection");
        result.setValue(TechnicalInformation.Field.BOOKTITLE,
                "Proceedings of the Eleventh ACM SIGKDD International Conference on Knowledge Discovery in Data Mining");
        result.setValue(TechnicalInformation.Field.SERIES, "KDD '05");
        result.setValue(TechnicalInformation.Field.LOCATION, "Chicago, Illinois, USA");
        result.setValue(TechnicalInformation.Field.YEAR, "2005");
        result.setValue(TechnicalInformation.Field.PAGES, "157-166");
        result.setValue(TechnicalInformation.Field.PUBLISHER, "ACM");
        result.setValue(TechnicalInformation.Field.ADDRESS, "New York, NY, USA");
        result.setValue(TechnicalInformation.Field.URL, "http://doi.acm.org/10.1145/1081870.1081891");

        return result;
    }

    /**
     * Determines the output format based on the input format and returns this. In
     * case the output format cannot be returned immediately, i.e.,
     * immediateOutputFormat() returns false, then this method will be called from
     * batchFinished().
     *
     * @param inputFormat the input format to base the output format on
     * @return the output format
     * @throws Exception in case the determination goes wrong
     * @see #hasImmediateOutputFormat()
     * @see #batchFinished()
     */
    @Override
    protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
        Instances result = new Instances(inputFormat, 0);
        result.insertAttributeAt(new Attribute("BaggedLOF"), result.numAttributes());
        return result;
    }

    @Override
    protected boolean hasImmediateOutputFormat() {
        return true;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 1.0 $");
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector<Option>();
        newVector.add(new Option(
                "\tNumber of bags to use" +
                        "\n\t(default = 10)",
                "numBags", 1, "-numBags <num>"));
        newVector.add(new Option(
                "\tCombine scores from the bags using 'Breadth First' method" +
                        "\n\t(default = 'Cumulative Sum')",
                "breadthFirst", 0, "-breadthFirst"));
        newVector.add(new Option(
                "\tRandom seed value (optional integer, for repeatable debugging)",
                "randomSeed", 1, "-randomSeed <num>"));

        // Add everything LOF allows
        for (Enumeration<Option> e = new LOF().listOptions(); e.hasMoreElements(); ) {
            newVector.add(e.nextElement());
        }
        // Plus stuff from the superclass
        for (Enumeration<Option> e2 = super.listOptions(); e2.hasMoreElements(); ) {
            newVector.add(e2.nextElement());
        }
        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     *
     <!-- options-start -->
     * Valid options are: <p/>
     * 
     * <pre> -numBags &lt;num&gt;
     *  Number of bags to use
     *  (default = 10)</pre>
     * 
     * <pre> -breadthFirst
     *  Combine scores from the bags using 'Breadth First' method
     *  (default = 'Cumulative Sum')</pre>
     * 
     * <pre> -randomSeed &lt;num&gt;
     *  Random seed value (optional integer, for repeatable debugging)</pre>
     * 
     * <pre> -min &lt;num&gt;
     *  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
     *  (default = 10)</pre>
     * 
     * <pre> -max &lt;num&gt;
     *  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
     *  (default = 40)</pre>
     * 
     * <pre> -A
     *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
     * </pre>
     * 
     * <pre> -num-slots &lt;num&gt;
     *  Number of execution slots.
     *  (default 1 - i.e. no parallelism)</pre>
     * 
     * <pre> -output-debug-info
     *  If set, filter is run in debug mode and
     *  may output additional info to the console</pre>
     * 
     * <pre> -do-not-check-capabilities
     *  If set, filter capabilities are not checked when input format is set
     *  (use with caution).</pre>
     * 
     <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        super.setOptions(options);
        setIsBreadthFirst(Utils.getFlag("breadthFirst", options));

        String opt_numBags = Utils.getOption("numBags", options);
        if (opt_numBags.length() > 0) {
            setNumBags(Integer.parseInt(opt_numBags));
        }

        String opt_randSeed = Utils.getOption("randomSeed", options);
        if (opt_randSeed.length() > 0) {
            setRandomSeed(Integer.parseInt(opt_randSeed));
        }

        //m_lof_options = options; // May contain empty strings, but that's okay.
        // Actually, that's not okay--it makes a test fail. Redo it to hold just the non-empty ones.
        int countOptionFields = 0;
        for (String opt : options) {
            if (opt.length() > 0)
                countOptionFields++;
        }
        m_lof_options = new String[countOptionFields];
        int currLOFCtr = 0;
        for (String opt : options) {
            if (opt.length() > 0) {
                m_lof_options[currLOFCtr] = opt;
                currLOFCtr++;
            }
        }
        // Can't leave anything in options or Utils.checkForRemainingOptions() will throw an error
        for (int i = 0; i < options.length; i++) {
            if (options[i].length() > 0)
                options[i] = "";
        }

    }

    /**
     * Gets the current settings of the filter.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {
        // Combine options from superclass, LOF options, and those specific to this class
        String[] superOptions = super.getOptions();

        String[] totalOptions = new String[superOptions.length + m_lof_options.length + 5];
        System.arraycopy(superOptions, 0, totalOptions, 0, superOptions.length);
        System.arraycopy(m_lof_options, 0, totalOptions, superOptions.length, m_lof_options.length);

        if (m_using_random_seed) {
            totalOptions[totalOptions.length - 5] = "-randomSeed";
            totalOptions[totalOptions.length - 4] = "" + m_randomSeed;
        } else {
            totalOptions[totalOptions.length - 5] = totalOptions[totalOptions.length - 4] = "";
        }
        totalOptions[totalOptions.length - 3] = "-numBags";
        totalOptions[totalOptions.length - 2] = "" + getNumBags();
        if (getIsBreadthFirst()) {
            totalOptions[totalOptions.length - 1] = "-breadthFirst";
        } else { // else leave the final element empty, but not null
            totalOptions[totalOptions.length - 1] = "";
        }

        return totalOptions;
    }


    public boolean getIsBreadthFirst() {
        return m_breadth_first;
    }

    public void setIsBreadthFirst(boolean m_breadth_first) {
        this.m_breadth_first = m_breadth_first;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String isBreadthFirstTipText() {
        return "Use breadth first method to aggregate the individual LOF scores into a BaggedLOF score (default is cumulative sum)";
    }

    public int getNumBags() {
        return m_num_bags;
    }

    public void setNumBags(int m_num_bags) {
        this.m_num_bags = m_num_bags;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String numBagsTipText() {
        return "The number of bags to use (number of runs of LOF, each using a different random subset of attributes) ";
    }

    protected void setRandomSeed(int randomSeed) {
        m_randomSeed = randomSeed;
        m_randomness = new Random(randomSeed);
        m_using_random_seed = true;
    }


    public static void main(String[] args) {
        runFilter(new BaggedLOF(), args);
    }

}

