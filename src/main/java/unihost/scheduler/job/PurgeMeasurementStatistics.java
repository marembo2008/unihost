/*
 * $Id: PurgeMeasurementStatistics.java 286 2013-05-20 21:37:34Z vollbsve $
 */
package unihost.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Timothy Polson
 * @author Sven Vollbehr
 */
@DisallowConcurrentExecution
public class PurgeMeasurementStatistics implements Job {

    private final static Logger logger = LoggerFactory.getLogger(PurgeMeasurementStatistics.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());

        logger.info("Successfully executed job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());
    }
}
