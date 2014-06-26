/*
 * $Id: RetrieveServerStatus.java 285 2013-05-20 21:22:41Z vollbsve $
 */
package unihost.scheduler.job;

import java.util.logging.Level;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Alrick Telfer
 * @author Sven Vollbehr
 * @author Timothy Polson
 */
public class RetrieveServerStatus implements Job {

  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(RetrieveServerStatus.class.getName());

  private String hostingServerHostName;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    hostingServerHostName = context.getMergedJobDataMap().getString("hostingServerHostName");
    LOG.log(Level.INFO, "Executing job {0} ({1}) on server {2}", new Object[]{context.getJobDetail().getKey(), context.getJobDetail().getDescription(), hostingServerHostName});
    LOG.log(Level.INFO, "Successfully executed job {0} ({1}) on server {2}", new Object[]{context.getJobDetail().getKey(), context.getJobDetail().getDescription(), hostingServerHostName});
  }
}
