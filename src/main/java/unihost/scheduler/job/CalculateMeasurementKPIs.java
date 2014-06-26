/*
 * $Id: CalculateMeasurementKPIs.java 258 2013-04-16 20:51:56Z vollbsve $
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
public class CalculateMeasurementKPIs implements Job {

    private final static Logger logger = LoggerFactory.getLogger(CalculateMeasurementKPIs.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());

        logger.info("Successfully executed job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());
    }
}
