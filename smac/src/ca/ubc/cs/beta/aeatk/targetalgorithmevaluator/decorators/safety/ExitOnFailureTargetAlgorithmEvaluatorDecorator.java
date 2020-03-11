package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Decorates callbacks so that exits (then halts if unsuccessful) the current process if on failure is ever called.
 * 
 * The primary aim of this decorator is to deal with JNI based Target Algorithm Evaluators which have corrupted memory,
 * the primary use case will be for workers that can be restarted externally with uncorrupted memory.
 * 
 * Usage of this decorator is discouraged except when the following two conditions hold:
 * 1) There is no external state that needs to be made consistent on Failure of a run.
 * 2) It is more important to ensure this process does terminate, than it is to ensure that it cleans up.
 * 
 * 
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class ExitOnFailureTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {
    
    private final static Logger log = LoggerFactory.getLogger(ExitOnFailureTargetAlgorithmEvaluatorDecorator.class);
    
    //Number of seconds to wait before halting.
    private final static int WAIT_TIME = 10;
    
    public ExitOnFailureTargetAlgorithmEvaluatorDecorator(
            TargetAlgorithmEvaluator tae) {
        super(tae);
    }
    
    
    private final static synchronized void exit()
    {
        Thread exitThread = new Thread()
        {
            @Override
            public void run()
            {
                Thread.currentThread().setName( this.getClass().getSimpleName() + ":System.Exit Thread ");
                log.warn("Exiting process.");
                System.exit(1);
            }
        };
        
        exitThread.start();
        
        for(int i=0;i<WAIT_TIME;i++)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //We are explicitly ignoring interrupts, we want to give the shutdown hooks time to fire.
                //So if we reset the flag we will keep timing out and then halt.
                //We don't particularly care about the shutdown and interruption policies of this thread.
            }
        }
        
        //Do not log here as we want to minimize our exposure to other parts of the code.
        System.err.println("Could not gracefully exit out of current thread in "+WAIT_TIME+" seconds, halting process. Abandon ship.");
        
        //Do a full termination.
        Runtime.getRuntime().halt(1);
    }
    
    @Override
    public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs, 
            final TargetAlgorithmEvaluatorCallback handler,
            final TargetAlgorithmEvaluatorRunObserver obs) {
        
        //Decorate the callback to exit on failure.
        TargetAlgorithmEvaluatorCallback decoratedCallback = new TargetAlgorithmEvaluatorCallback() {
            
            @Override
            public void onSuccess(List<AlgorithmRunResult> runs) {
                handler.onSuccess(runs);
            }
            
            @Override
            public void onFailure(final RuntimeException e) {
                try
                {
                    //Do this in another thread, we dont really care if this completes.
                    Thread onFailureNotifier = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            Thread.currentThread().setName( this.getClass().getSimpleName() + ":onFailureNotifying Thread ");
                            handler.onFailure(e);
                        }
                    };
                    
                    onFailureNotifier.start();
                    
                    
                }
                finally
                {
                    log.warn("On failure detected, exiting current process.");
                    exit();
                }
            }
        };
        
        this.tae.evaluateRunsAsync(runConfigs, decoratedCallback, obs);
        
    }
    
    @Override
    public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
        return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
    }

    @Override
    protected void postDecorateeNotifyShutdown() {
        //No shutdown necessary.
    }

}
