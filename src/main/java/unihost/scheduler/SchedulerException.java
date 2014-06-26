/*
 * $Id: SchedulerException.java 140 2013-02-13 19:58:15Z vollbsve $
 */
package unihost.scheduler;

/**
 * @author Sven Vollbehr
 */
public class SchedulerException extends org.quartz.SchedulerException
{
    public SchedulerException() {
        super();
    }

    public SchedulerException(String msg) {
        super(msg);
    }

    public SchedulerException(Throwable cause) {
        super(cause);
    }

    public SchedulerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
