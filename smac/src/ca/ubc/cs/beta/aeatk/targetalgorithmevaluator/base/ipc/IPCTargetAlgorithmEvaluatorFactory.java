package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class IPCTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  
{

	@Override
	public String getName() {
		return "IPC";
	}

	@Override
	public IPCTargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AbstractOptions options) {
		IPCTargetAlgorithmEvaluatorOptions ipcOptions = (IPCTargetAlgorithmEvaluatorOptions) options;
		
		IPCTargetAlgorithmEvaluator tae =  new IPCTargetAlgorithmEvaluator(ipcOptions);
		
		return tae;
		
	}

	@Override
	public IPCTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new IPCTargetAlgorithmEvaluatorOptions();
	}
	
	

}
