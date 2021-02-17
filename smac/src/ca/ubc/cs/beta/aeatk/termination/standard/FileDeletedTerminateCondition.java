package ca.ubc.cs.beta.aeatk.termination.standard;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;

public class FileDeletedTerminateCondition extends AbstractTerminationCondition {


	private final AtomicBoolean terminate = new AtomicBoolean(false);
	
	private final ScheduledExecutorService execService = Executors.newScheduledThreadPool(1,new SequentiallyNamedThreadFactory("Terminate Run FileWatcher", true));

	private final long POLL_FREQUENCY = 2;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	
	private final String filename;
	
	public FileDeletedTerminateCondition(final File fileToWatch)
	{
		
	
		this.filename = fileToWatch.getAbsolutePath();
		
		log.info("Terminating procedure if {} is deleted", fileToWatch);
		if(!fileToWatch.exists())
		{
			log.warn("File To Watch: {} does not exist, was it already deleted?", fileToWatch);
		}
		
		execService.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				if(!fileToWatch.exists())
				{
					if(!terminate.getAndSet(true))
					{
						log.info("File {} has been deleted, procedure set to terminate", fileToWatch);
					}
					
					
				}
				
			}
		},2, 2, TimeUnit.SECONDS);
		
		
	}
	@Override
	public boolean haveToStop() {
		return terminate.get();
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.emptySet();
	}

	@Override
	public String getTerminationReason() {
		if(haveToStop())
		{
			return "file being watched " + filename + " was deleted on disk.";
		} else
		{
			return "";
		}
	}

}
