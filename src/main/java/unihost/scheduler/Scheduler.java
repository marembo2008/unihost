/**
 * $Id: Scheduler.java 542 2014-02-07 16:09:43Z vollbsve $
 */
package unihost.scheduler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.DailyCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unihost.scheduler.job.CalculateAverageMeasurements;
import unihost.scheduler.job.CalculateMeasurementKPIs;
import unihost.scheduler.job.GenerateHostingReports;
import unihost.scheduler.job.PurgeMeasurementStatistics;
import unihost.scheduler.job.RetrieveServerStatus;
import unihost.util.Properties;

/**
 * http://quartz-scheduler.org/files/documentation/Quartz-2.1.x-Documentation.pdf
 *
 * @author Sven Vollbehr
 */
public class Scheduler {

  private final static Logger logger = LoggerFactory.getLogger(Scheduler.class);
  private final StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
  private org.quartz.Scheduler scheduler;

  public Scheduler() throws SchedulerException {
    this(false);
  }

  public Scheduler(boolean proxy) throws SchedulerException {
    try {
      java.util.Properties properties = Properties.asProperties("^org.quartz.*");
      if (proxy) {
        properties.put("org.quartz.scheduler.rmi.proxy", "true");
      } else {
        properties.put("org.quartz.scheduler.rmi.export", "true");
      }
      schedulerFactory.initialize(properties);
      scheduler = schedulerFactory.getScheduler();
    } catch (org.quartz.SchedulerException e) {
      logger.error(e.getMessage(), e);
      throw new SchedulerException(e);
    }
  }

  private Map<JobDetail, Set<? extends Trigger>> buildJobs() {
    Map<JobDetail, Set<? extends Trigger>> jobs = new HashMap<>();
    // Retrieve Server Status
    JobDetail retrieveServerStatusJob = newJob(RetrieveServerStatus.class)
            .withDescription("Retrieve server status monitoring data")
            .withIdentity("retrieveServerStatusJob", "unihost")
            .build();
    Set<Trigger> retrieveServerStatusJobTriggers = new HashSet<>();

    logger.info("Scheduling server status monitoring job for manually added server");

    retrieveServerStatusJobTriggers.add(newTrigger()
            .forJob(retrieveServerStatusJob)
            .withSchedule(
                    calendarIntervalSchedule()
                    .preserveHourOfDayAcrossDaylightSavings(true)
                    .withIntervalInSeconds(10)) // Every 10 second starting now
            .usingJobData("hostingServerHostName", "manually added server")
            .startNow()
            .withIdentity("retrieveServerStatusTrigger")
            //            .modifiedByCalendar("daily-calendar")
            .build());
    retrieveServerStatusJobTriggers.add(newTrigger()
            .forJob(retrieveServerStatusJob)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(10)) // Every 10 second starting now
            .usingJobData("hostingServerHostName-simpleschedule", "manually added server for simple-schedule")
            .startNow()
            .withIdentity("retrieveServerStatusTrigger-simpleSchedule")
            //            .modifiedByCalendar("daily-calendar")
            .build());
    jobs.put(retrieveServerStatusJob, retrieveServerStatusJobTriggers);
    buildJobs0(jobs, retrieveServerStatusJob, retrieveServerStatusJobTriggers);
    return jobs;
  }

  private void buildJobs0(Map<JobDetail, Set<? extends Trigger>> jobs, JobDetail retrieveServerStatusJob, Set<Trigger> retrieveServerStatusJobTriggers) {
    for (int i = 0; i < 10; i++) {

      logger.info("Scheduling server status monitoring job for server {}", "server " + i);

      retrieveServerStatusJobTriggers.add(newTrigger()
              .forJob(retrieveServerStatusJob)
              .withSchedule(cronSchedule("0 0/1 * * * ?").withMisfireHandlingInstructionIgnoreMisfires())// Every minute on minute start
              .usingJobData("hostingServerHostName", "server " + i)
              .withIdentity("status-monitoring" + i)
              .startNow()
              .build());
    }
    jobs.put(retrieveServerStatusJob, retrieveServerStatusJobTriggers);

    // Purge Measurement Statistics
    for (int i = 0; i < 10; i++) {
      logger.info("Scheduling measurement statistics purge job");
      JobDetail purgeMeasurementStatisticsJob = newJob(PurgeMeasurementStatistics.class)
              .withDescription("Purge measurement statistics data passed retention periods")
              .withIdentity("Scheduling measurement statistics purge job" + i)
              .build();
      Set<? extends Trigger> purgeMeasurementStatisticsTriggers = Collections.singleton((Trigger) newTrigger()
              .forJob(purgeMeasurementStatisticsJob)
              .withSchedule(cronSchedule("0 0 1 * * ? *").withMisfireHandlingInstructionIgnoreMisfires())// Every day at midnight on hour start
              .startNow()
              .withIdentity("Scheduling measurement statistics purge trigger" + i)
              .build());
      jobs.put(purgeMeasurementStatisticsJob, purgeMeasurementStatisticsTriggers);
    }
    for (int i = 0; i < 10; i++) {
      // Calculate Measurement KPIs
      logger.info("Scheduling measurement KPIs calculation job");
      JobDetail calculateMeasurementKPIsJob = newJob(CalculateMeasurementKPIs.class)
              .withDescription("Calculate measurement KPI's")
              .withIdentity("Scheduling measurement KPIs calculation job" + i)
              .build();
      Set<? extends Trigger> calculateMeasurementKPIsTriggers = Collections.singleton((Trigger) newTrigger()
              .forJob(calculateMeasurementKPIsJob)
              .withSchedule(cronSchedule("0 30 1 * * ? *").withMisfireHandlingInstructionIgnoreMisfires())// Every day at one thirty
              .startNow()
              .withIdentity("Scheduling measurement KPIs calculation trigger" + i)
              .build());
      jobs.put(calculateMeasurementKPIsJob, calculateMeasurementKPIsTriggers);
    }
    for (int i = 0; i < 10; i++) {
      // Calculate Average Measurements
      logger.info("Scheduling average measurement calculation job");
      JobDetail calculateAverageMeasurementsJob = newJob(CalculateAverageMeasurements.class)
              .withDescription("Calculate average measurement statistics")
              .withIdentity("Scheduling average measurement calculation job" + i)
              .build();

      Set<? extends Trigger> calculateAverageMeasurementsTriggers = Collections.singleton((Trigger) newTrigger()
              .forJob(calculateAverageMeasurementsJob)
              .withSchedule(cronSchedule("0 0 * * * ? *").withMisfireHandlingInstructionIgnoreMisfires())// Every hour at hour start
              .startNow()
              .withIdentity("Scheduling average measurement calculation trigger" + i)
              .build());
      jobs.put(calculateAverageMeasurementsJob, calculateAverageMeasurementsTriggers);
    }
    for (int i = 0; i < 10; i++) {
      // Generate Hosting Reports
      logger.info("Scheduling hosting report generation job");
      JobDetail generateHostingReportsJob = newJob(GenerateHostingReports.class)
              .withDescription("Generate Hosting Reports")
              .withIdentity("Scheduling hosting report generation job" + i)
              .build();

      Set<? extends Trigger> generateHostingReportsTriggers = Collections.singleton((Trigger) newTrigger()
              .forJob(generateHostingReportsJob)
              .withSchedule(cronSchedule("0 0 1 1 * ? *").withMisfireHandlingInstructionIgnoreMisfires()) // First day of every month
              .startNow()
              .withIdentity("Scheduling hosting report generation trigger" + i)
              .build());
      jobs.put(generateHostingReportsJob, generateHostingReportsTriggers);
    }
  }

  public void standby() throws SchedulerException {
    try {
      scheduler.standby();
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException(e);
    }
  }

  public void start() throws SchedulerException {
    try {
      DailyCalendar dc = new DailyCalendar("11:55:00", "12:30:00");
      scheduler.addCalendar("daily-calendar", dc, true, true);
      scheduler.scheduleJobs(buildJobs(), true);
      scheduler.start();
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException(e);
    }
  }

  public void shutdown() throws SchedulerException {
    try {
      if (scheduler.isStarted()) {
        scheduler.shutdown(true);
      }
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException(e);
    }
  }

  public void pauseAll() throws SchedulerException {
    try {
      if (!scheduler.isStarted()) {
        scheduler.start();
      }
      scheduler.pauseAll();
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException(e);
    }
  }

  public void resumeAll() throws SchedulerException {
    try {
      if (!scheduler.isStarted()) {
        scheduler.start();
      }
      scheduler.resumeAll();
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException(e);
    }
  }
}
