/*
 * $Id: CalculateMeasurementProjections.java 369 2013-07-14 21:27:11Z vollbsve $
 */
package unihost.scheduler.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Timothy Polson
 */
public class CalculateMeasurementProjections implements Job {

    private final static Logger logger = LoggerFactory.getLogger(CalculateMeasurementProjections.class);
    private Integer initialPrecisionMinutes;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());

        logger.info("Successfully executed job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());
    }

}
