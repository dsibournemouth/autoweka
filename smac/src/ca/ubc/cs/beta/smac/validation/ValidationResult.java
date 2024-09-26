package ca.ubc.cs.beta.smac.validation;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

public class ValidationResult {

	private Double performance;
	private List<ProblemInstanceSeedPair> pisps;

	public ValidationResult(Double performance, List<ProblemInstanceSeedPair> pisps)
	{
		this.performance = performance;
		this.pisps = pisps;
	}

	public Double getPerformance() {
		return performance;
	}

	public List<ProblemInstanceSeedPair> getPISPS() {
		return pisps;
	}
	
	
}
