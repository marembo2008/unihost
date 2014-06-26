/**
 * $Id: Bootstrap.java 351 2013-07-08 11:16:17Z vollbsve $
 */
package unihost.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sven Vollbehr
 */
public class Bootstrap {

  private final static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

  public static void main(String... args) throws Exception {
    /* Initialize logging */
//        java.util.Properties properties = Properties.asProperties("^log4j.*");
//        properties.setProperty("log4j.appender.FILE.File", "log.quartz");
//        PropertyConfigurator.configure(properties);

    /* Initialize job scheduler */
    if (args.length == 0 || (args.length > 0 && args[0].equals("start"))) {
      Long startTime = System.nanoTime();
      logger.info("Starting up Unihost Job Scheduler {}", "TEST");

      // Start job scheduler
      try {
        Scheduler scheduler = new Scheduler();
        scheduler.start();
      } catch (SchedulerException e) {
        logger.error(e.getMessage(), e);
        e.printStackTrace(System.err);
      }

      logger.info("Unihost Job Scheduler started in {}ms", (System.nanoTime() - startTime) / 1000000L);
    } else if (args.length > 0) {
      // Shutdown, pause, or resume the scheduler
      try {
        Scheduler scheduler = new Scheduler(true); // new Scheduler(true);
        switch (args[0]) {
          case "standby":
            scheduler.shutdown();
            logger.info("Unihost Job Scheduler on standby");
            break;
          case "shutdown":
            scheduler.shutdown();
            logger.info("Unihost Job Scheduler stopped");
            break;
          case "pause":
            scheduler.pauseAll();
            logger.info("Unihost Job Scheduler paused");
            break;
          case "resume":
            scheduler.resumeAll();
            logger.info("Unihost Job Scheduler resumed");
            break;
          default:
            System.err.println("Unrecognized command");
            break;
        }
      } catch (SchedulerException e) {
        logger.error(e.getMessage(), e);
        e.printStackTrace(System.err);
      }
    }
  }
}
