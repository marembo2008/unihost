/**
 * $Id: GenerateHostingReports.java 498 2013-10-24 05:49:32Z telfealr $
 */
package unihost.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alrick Telfer
 */
@DisallowConcurrentExecution
public class GenerateHostingReports implements Job {

    private final static Logger logger = LoggerFactory.getLogger(GenerateHostingReports.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());

        logger.info("Successfully executed job {} ({})", context.getJobDetail().getKey(), context.getJobDetail().getDescription());
    }
}
