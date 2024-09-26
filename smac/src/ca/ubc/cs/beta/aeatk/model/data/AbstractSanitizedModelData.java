package ca.ubc.cs.beta.aeatk.model.data;

import java.util.Map;

/**
 * Abstract Sanitized Model Data 
 * 
 * This is a mixed-metaphor and supports both decorating and being a base type for nondecorated versions
 * 
 * This will probabaly be heavily refactored 
 * 
 * @author sjr
 *
 */
public abstract class AbstractSanitizedModelData implements SanitizedModelData{

	private final SanitizedModelData smd;

	public AbstractSanitizedModelData(SanitizedModelData smd)
	{
		this.smd = smd;	
	}
	
	protected AbstractSanitizedModelData()
	{
		this.smd = null;
	}
	
	@Override
	public double[][] getPrePCAInstanceFeatures() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getPrePCAInstanceFeatures();
		
	}

	@Override
	public double[][] getPCAVectors() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getPCAVectors();
	}

	@Override
	public double[] getPCACoefficients() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getPCACoefficients();
	}

	@Override
	public int[] getDataRichIndexes() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getDataRichIndexes();
	}

	@Override
	public double[] getMeans() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getMeans();
	}

	@Override
	public double[] getStdDev() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getStdDev();
	}

	@Override
	public double[][] getPCAFeatures() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getPCAFeatures();
	}

	@Override
	public double[][] getConfigs() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getConfigs();
	}

	@Override
	public double[] getResponseValues() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getResponseValues();
	}

	@Override
	public int[] getCategoricalSize() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getCategoricalSize();
	}
	
	@Override
	public Map<Integer, int[][]> getNameConditionsMapParentsArray() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getNameConditionsMapParentsArray();
	}; 
	
	@Override
	public Map<Integer, double[][][]> getNameConditionsMapParentsValues() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getNameConditionsMapParentsValues();
	}
	
	@Override
	public Map<Integer, int[][]> getNameConditionsMapOp() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getNameConditionsMapOp();
	}
	
	/*
	@Override
	public int[][] getCondParents() {

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getCondParents();
	}

	@Override
	public int[][][] getCondParentVals()
	{

		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getCondParentVals();
	}
	*/

	@Override
	public double transformResponseValue(double d)
	{
		return smd.transformResponseValue(d);
	}
	
	@Override
	public int[][] getThetaInstIdxs() 
	{
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getThetaInstIdxs();
	}

	@Override
	public boolean[] getCensoredResponses()
	{
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getCensoredResponses();	
	}

	@Override
	public int[] getConstantColumns() {
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.getConstantColumns();
	}

	@Override
	public boolean isEmptyFeatures()
	{
		if(this.smd == null) throw new UnsupportedOperationException("No Wrapped Object and no default implementation");
		return smd.isEmptyFeatures();
	}
	
}
