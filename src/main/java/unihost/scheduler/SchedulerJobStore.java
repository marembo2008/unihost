/**
 * $Id$
 */
package unihost.scheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;
import unihost.domain.quartz.QuartzJobDetail;
import unihost.domain.quartz.QuartzJobKey;
import unihost.domain.quartz.QuartzObject;
import unihost.domain.quartz.calendar.QuartzCalendar;
import unihost.domain.quartz.lock.LockException;
import unihost.domain.quartz.lock.DefaultSemaphore;
import unihost.domain.quartz.lock.Semaphore;
import unihost.domain.quartz.trigger.QuartzTrigger;
import unihost.domain.quartz.trigger.QuartzTriggerGroup;
import unihost.domain.quartz.trigger.QuartzTriggerKey;

public class SchedulerJobStore implements JobStore {

  private static final Logger LOG = Logger.getLogger(SchedulerJobStore.class.getName());
  private static final String TRIGGER_ACCESS_LOCK = "trigger_access_lock";
  private SchedulerSignaler signaler;
  private String instanceName;
  private String instanceId;
  private int threadPoolSize;
  protected long misfireThreshold = 10000l;
  private QuartzInstance instance;
  private Semaphore lock;
  private volatile boolean pauseRequested;

  @Override
  public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
    this.signaler = signaler;
  }

  private Semaphore getSemaphore() {
    return lock != null ? lock : (lock = new DefaultSemaphore(instance));
  }

  @Override
  public void schedulerStarted() throws SchedulerException {
    //we can start up the database connection.
    //By initializing entity factory.
    Class domainclass = Domain.class;
  }

  @Override
  public void schedulerPaused() {
    //what are we supposed to do here?
    LOG.info("scheduler paused...............");
  }

  @Override
  public void schedulerResumed() {
    //and when resumed?
    LOG.info("scheduler resumed................");
  }

  @Override
  public void shutdown() {
    //close the current threads entity manager, and then entity manager factory.
    //we need to close all the entity managers for the quartz scheduler, the problem is, how do we get hold of them?
  }

  @Override
  public boolean supportsPersistence() {
    //definitely we do
    return true;
  }

  @Override
  public long getEstimatedTimeToReleaseAndAcquireTrigger() {
    //What is the estimated time?
    return 10;
  }

  @Override
  public boolean isClustered() {
    //for now no.
    return false;
  }

  @Override
  public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException {
    //of course we store job first
    try {
      storeJob0(newJob, false);
      storeTrigger0(newJob.getKey(), newTrigger, false);
    } catch (Exception ex) {
      throw new JobPersistenceException("error storing job and trigger", ex);
    }
  }

  @Override
  public void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      storeJob0(newJob, replaceExisting);
    } catch (Exception ex) {
      throw new JobPersistenceException("error storing job and trigger", ex);
    }
  }

  private QuartzJobDetail storeJob0(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      QuartzJobDetail jobDetail = QuartzJobDetail.find(newJob.getKey(), instance);
      if (jobDetail != null) {
        if (!replaceExisting) {
          throw new ObjectAlreadyExistsException(newJob);
        } else {
          //we need to delete all the data about the job.
          jobDetail.delete();
        }
      }
      jobDetail = QuartzJobDetail.createInstance(newJob, instance);
      jobDetail.createOrUpdate();
      return jobDetail;
    } catch (Exception e) {
      throw new JobPersistenceException(e.getLocalizedMessage(), e);
    }
  }

  private QuartzTrigger storeTrigger0(JobKey jobKey, OperableTrigger newTrigger, boolean replaceExisting)
          throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      getSemaphore().lock(newTrigger.getKey().toString());
      //find the trigger if possible.
      QuartzTrigger trigger = QuartzTrigger.find(newTrigger.getKey(), instance);
      if (trigger != null) {
        if (!replaceExisting) {
          throw new ObjectAlreadyExistsException(newTrigger);
        } else {
          //delete all trigger information
          trigger.delete();
        }
      }
      trigger = QuartzTrigger.createInstance(newTrigger, instance);
      trigger.setTrigger(newTrigger);
      if (jobKey != null) {
        trigger.setJobKey(jobKey);
      }
      trigger.createOrUpdate();
      return trigger;
    } catch (Exception e) {
      throw new JobPersistenceException(e.getLocalizedMessage(), e);
    } finally {
      try {
        getSemaphore().release(newTrigger.getKey().toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)
          throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      LOG.log(Level.INFO,
              "storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs ({0}), boolean replace ({1}))",
              new Object[]{triggersAndJobs.size(), replace});
      for (Map.Entry<JobDetail, Set<? extends Trigger>> e : triggersAndJobs.entrySet()) {
        JobDetail newJob = e.getKey();
        try {
          getSemaphore().lock(newJob.getKey().toString());
          QuartzJobDetail data = QuartzJobDetail.find(newJob.getKey(), instance);
          if (data != null) {
            LOG.log(Level.INFO, "Job detail exists in job store {0}", newJob.getKey());
            if (!replace) {
              throw new ObjectAlreadyExistsException(newJob);
            } else {
              data.delete();
            }
          }
          data = QuartzJobDetail.createInstance(newJob, instance);
          data.createOrUpdate();
          //create the triggers.
          Set<? extends Trigger> triggers = e.getValue();
          //we need to create actual triggers from this.
          for (Trigger tr : triggers) {
            try {
              getSemaphore().lock(tr.getKey().toString());
              QuartzTrigger trigger = QuartzTrigger.find(tr.getKey(), instance);
              if (trigger != null) {
                LOG.log(Level.INFO, "Trigger exists in job store {0}", tr.getKey());
                if (!replace) {
                  throw new ObjectAlreadyExistsException(tr);
                } else {
                  trigger.delete();
                }
              }
              trigger = QuartzTrigger.createInstance(tr, instance);
              trigger.setJobKey(newJob.getKey());
              trigger.setTrigger(tr);
              trigger.createOrUpdate();
            } finally {
              try {
                getSemaphore().release(tr.getKey().toString());
              } catch (LockException ex) {
                Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
              }
            }
          }
        } finally {
          try {
            getSemaphore().release(newJob.getKey().toString());
          } catch (LockException ex) {
            Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public boolean removeJob(JobKey jobKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(jobKey.toString());
      QuartzJobDetail.removeJob(jobKey, instance);
      return true;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(jobKey.toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public boolean removeJobs(List<JobKey> jobKeys) throws JobPersistenceException {
    for (JobKey key : jobKeys) {
      removeJob(key);
    }
    return true;
  }

  @Override
  public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
    QuartzJobDetail jobDetail = QuartzJobDetail.find(jobKey, instance);
    if (jobDetail == null) {
      throw new JobPersistenceException("Job " + jobKey + " does not exists");
    }
    return jobDetail.getJobDetail();
  }

  @Override
  public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
    storeTrigger0(null, newTrigger, replaceExisting);
  }

  @Override
  public boolean removeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      QuartzTrigger.remove(triggerKey, instance);
      return true;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public boolean removeTriggers(List<TriggerKey> triggerKeys) throws JobPersistenceException {
    LOG.log(Level.INFO, "removeTriggers(List<TriggerKey> triggerKeys): '{'0'}'{0}", triggerKeys);
    for (TriggerKey key : triggerKeys) {
      removeTrigger(key);
    }
    return true;
  }

  @Override
  public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {
    try {
      getSemaphore().lock(triggerKey.toString());
      QuartzTrigger trigger = QuartzTrigger.find(triggerKey, instance);
      if (trigger == null) {
        throw new JobPersistenceException("Trigger " + triggerKey + " does not exists");
      } else {
        trigger.delete();
      }
      trigger.setTrigger(newTrigger);
      trigger.createOrUpdate();
      return true;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(triggerKey.toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public OperableTrigger retrieveTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    QuartzTrigger trigger = QuartzTrigger.find(triggerKey, instance);
    if (trigger == null) {
      throw new JobPersistenceException("Trigger " + triggerKey + " does not exists");
    }
    return (OperableTrigger) trigger.getTrigger();
  }

  @Override
  public boolean checkExists(JobKey jobKey) throws JobPersistenceException {
    return QuartzJobDetail.exists(jobKey, instance);
  }

  @Override
  public boolean checkExists(TriggerKey triggerKey) throws JobPersistenceException {
    return QuartzTrigger.exists(triggerKey, instance);
  }

  @Override
  public void clearAllSchedulingData() throws JobPersistenceException {
    QuartzObject.deleteAllQuartsSchedlingData();
  }

  @Override
  public void storeCalendar(String name, Calendar calendar, boolean replaceExisting, boolean updateTriggers)
          throws ObjectAlreadyExistsException, JobPersistenceException {
    try {
      LOG.info("Storing calendar...............");
      QuartzCalendar jc = QuartzCalendar.findCalendar(name, instance);
      if (jc != null) {
        if (!replaceExisting) {
          throw new ObjectAlreadyExistsException("calendar already exist");
        }
        jc.delete();
      }
      jc = QuartzCalendar.createInstance(calendar, name, instance);
      jc.setCalendar(calendar);
      jc.createOrUpdate();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, name, ex);
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public boolean removeCalendar(String calName) throws JobPersistenceException {
    try {
      getSemaphore().lock(calName);
      QuartzCalendar.removeCalendar(calName, instance);
      return true;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(calName);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
    QuartzCalendar qc = QuartzCalendar.findCalendar(calName, instance);
    return qc == null ? null : qc.getCalendar();
  }

  @Override
  public int getNumberOfJobs() throws JobPersistenceException {
    return QuartzJobDetail.getJobCount(instance);
  }

  @Override
  public int getNumberOfTriggers() throws JobPersistenceException {
    return QuartzTrigger.count(QuartzTrigger.class);
  }

  @Override
  public int getNumberOfCalendars() throws JobPersistenceException {
    return QuartzCalendar.count(QuartzCalendar.class);
  }

  @Override
  public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    //get all job keys
    List<QuartzJobKey> jjds = QuartzJobDetail.findJobKeys(instance);
    Set<JobKey> jks = new HashSet<>();
    for (QuartzJobKey jjk : jjds) {
      JobKey jk = jjk.getKey();
      if (matcher.isMatch(jk)) {
        jks.add(jk);
      }
    }
    return jks;
  }

  @Override
  public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    List<QuartzTriggerKey> jtks = QuartzTrigger.findTriggerKeys(instance);
    Set<TriggerKey> tks = new HashSet<>();
    for (QuartzTriggerKey jtk : jtks) {
      TriggerKey tk = jtk.getKey();
      if (matcher.isMatch(tk)) {
        tks.add(tk);
      }
    }
    return tks;
  }

  @Override
  public List<String> getJobGroupNames() throws JobPersistenceException {
    return QuartzJobDetail.findJobGroups(instance);
  }

  @Override
  public List<String> getTriggerGroupNames() throws JobPersistenceException {
    return QuartzTriggerGroup.findTriggerGroups(instance);
  }

  @Override
  public List<String> getCalendarNames() throws JobPersistenceException {
    return QuartzCalendar.findCalendarNames(instance);
  }

  @Override
  public List<OperableTrigger> getTriggersForJob(JobKey jobKey) throws JobPersistenceException {
    List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jobKey, instance);
    List<OperableTrigger> ots = new ArrayList<>();
    for (QuartzTrigger jt : jts) {
      ots.add((OperableTrigger) jt.getTrigger());
    }
    return ots;
  }

  @Override
  public Trigger.TriggerState getTriggerState(TriggerKey triggerKey) throws JobPersistenceException {
    Trigger.TriggerState state = QuartzTrigger.getTriggerState(triggerKey, instance);
    LOG.log(Level.INFO, "Trigger state {0}={1}", new Object[]{triggerKey, state});
    return state;
  }

  @Override
  public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(triggerKey.toString());
      LOG.info("pauseTrigger(TriggerKey triggerKey)");
      QuartzTrigger jt = QuartzTrigger.find(triggerKey, instance);
      if (jt == null) {
        throw new JobPersistenceException("Trigger does not exists");
      }
      jt.setTriggerState(Trigger.TriggerState.PAUSED);
      jt.setFireState(QuartzTrigger.TriggerFireState.STATE_PAUSED);
      jt.update();
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(triggerKey.toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    try {
      LOG.info("pauseTriggers(GroupMatcher<TriggerKey> matcher)");
      //find all the trigger keys.
      List<String> pausedGroups = new ArrayList<>();
      List<QuartzTriggerKey> jtks = QuartzTrigger.findTriggerKeys(instance);
      for (QuartzTriggerKey jtk : jtks) {
        try {
          getSemaphore().lock(jtk.getKey().toString());
          TriggerKey key = jtk.getKey();
          if (matcher.isMatch(key)) {
            QuartzTriggerGroup qtg = QuartzTriggerGroup.getTriggerGroup(key.getGroup(), instance);
            qtg.pauseGroup();
            pausedGroups.add(key.getGroup());
          }
        } finally {
          try {
            getSemaphore().release(jtk.getKey().toString());
          } catch (LockException ex) {
            Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
      return pausedGroups;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public void pauseJob(JobKey jobKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(jobKey.toString());
      LOG.info(" pauseJob(JobKey jobKey)");
      List<QuartzTrigger> triggers = QuartzTrigger.findTriggersForJobKey(jobKey, instance);
      for (QuartzTrigger t : triggers) {
        pauseTrigger(t.getTriggerKey());
      }
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(jobKey.toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Collection<String> pauseJobs(GroupMatcher<JobKey> groupMatcher) throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      LOG.info("pauseJobs(GroupMatcher<JobKey> groupMatcher) ");
      List<QuartzJobKey> jobKeys = QuartzJobDetail.findJobKeys(instance);
      List<String> groups = new ArrayList<>();
      for (QuartzJobKey jjk : jobKeys) {
        JobKey jk = jjk.getKey();
        if (groupMatcher.isMatch(jk)) {
          QuartzJobDetail jjd = QuartzJobDetail.find(jk, instance);
          //find the triggers.
          List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jk, instance);
          for (QuartzTrigger jt : jts) {
            jt.setTriggerState(Trigger.TriggerState.PAUSED);
            jt.setFireState(QuartzTrigger.TriggerFireState.STATE_PAUSED);
            jt.update();
          }
          jjd.update();
          groups.add(jk.getGroup());
        }
      }
      return groups;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      LOG.info("resumeTrigger(TriggerKey triggerKey)");
      /**
       * TODO(marembo). What happens if the trigger is in a group which is paused? Do we un-pause
       * the group, since the group is paused iff the all the triggers in the group is paused?
       */
      QuartzTrigger jt = QuartzTrigger.find(triggerKey, instance);
      if (jt == null) {
        throw new JobPersistenceException("Trigger does not exists");
      }
      jt.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
      jt.setTriggerState(Trigger.TriggerState.NORMAL);
      jt.update();
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    try {
      LOG.info("resumeTriggers(GroupMatcher<TriggerKey> matcher)");
      List<QuartzTriggerKey> jtks = QuartzTrigger.findTriggerKeys(instance);
      List<String> groups = new ArrayList<>();
      for (QuartzTriggerKey jtk : jtks) {
        TriggerKey key = jtk.getKey();
        if (matcher.isMatch(key)) {
          QuartzTriggerGroup qtg = QuartzTriggerGroup.getTriggerGroup(key.getGroup(), instance);
          qtg.resumeGroup();
          groups.add(key.getGroup());
        }
      }
      return groups;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
    Set<String> pausedGroups = new HashSet<>(QuartzTriggerGroup.findTriggerGroups(instance));
    LOG.log(Level.INFO, "Paused trigger groups{0}", pausedGroups);
    return pausedGroups;
  }

  @Override
  public void resumeJob(JobKey jobKey) throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jobKey, instance);
      for (QuartzTrigger jt : jts) {
        jt.setTriggerState(Trigger.TriggerState.NORMAL);
        jt.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
        jt.update();
      }
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      LOG.info("resumeJobs..............");
      List<QuartzJobKey> jjks = QuartzJobDetail.findJobKeys(instance);
      Set<String> groups = new HashSet<>();
      for (QuartzJobKey jjk : jjks) {
        JobKey jk = jjk.getKey();
        if (matcher.isMatch(jk)) {
          groups.add(jk.getGroup());
          QuartzJobDetail jjd = QuartzJobDetail.find(jk, instance);
          List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jk, instance);
          for (QuartzTrigger jt : jts) {
            jt.setTriggerState(Trigger.TriggerState.NORMAL);
            jt.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
            jt.update();
          }
          jjd.update();
        }
      }
      return groups;
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public void pauseAll() throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      pauseRequested = true;
      final Object sync = new Object();
      LOG.info("Pause all jobs");
      int count = 0;
      while (QuartzTrigger.findActiveTriggers(instance) && count < 5) {
        count++;
        synchronized (sync) {
          try {
            sync.wait(1000);
          } catch (InterruptedException ignore) {
          }
        }
      }
      QuartzTriggerGroup.pauseTriggerGroups(instance);
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public void resumeAll() throws JobPersistenceException {
    try {
      getSemaphore().lock(TRIGGER_ACCESS_LOCK);
      pauseRequested = false;
      QuartzTriggerGroup.resumeTriggerGroups(instance);
      List<QuartzTrigger> triggers = QuartzTrigger.findTriggers(instance);
      for (QuartzTrigger t : triggers) {
        try {
          getSemaphore().lock(t.getTriggerKey().toString());
          isExecuteOnMisfire(t);
        } finally {
          getSemaphore().release(t.getTriggerKey().toString());
        }
      }
      LOG.info("Resume all finished");
    } catch (Exception ex) {
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    } finally {
      try {
        getSemaphore().release(TRIGGER_ACCESS_LOCK);
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  protected boolean isExecuteOnMisfire(QuartzTrigger trigger) throws JobPersistenceException {
    try {
      LOG.log(Level.INFO, "applyMisfire: {0} state={1}", new Object[]{trigger.getTriggerKey(), trigger.getFireState()});
      long currentTime = System.currentTimeMillis();
      if (getMisfireThreshold() > 0) {
        currentTime -= getMisfireThreshold();
      }
      Date nextFireTime = trigger.getNextFireTime();
      if (nextFireTime == null || nextFireTime.getTime() > currentTime
              || trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
        return true;
      }
      //trigger has misfired.
      Calendar cal = null;
      if (trigger.getCalendarName() != null) {
        cal = retrieveCalendar(trigger.getTrigger().getCalendarName());
      }
      LOG.log(Level.INFO, "Trigger {0} has misfired......", trigger.getTriggerKey());
      signaler.notifyTriggerListenersMisfired((OperableTrigger) trigger.getTrigger());
      ((OperableTrigger) trigger.getTrigger()).updateAfterMisfire(cal);
      printDate(nextFireTime, "Next Fire time before update");
      Date nextFireTimeAfterUpdate = trigger.getTrigger().getNextFireTime();
      printDate(nextFireTimeAfterUpdate, "Next fire time after update");
      Date now = new Date();
      printDate(now, "Current time");
      if (trigger.getTrigger().getNextFireTime() == null) {
        signaler.notifySchedulerListenersFinalized(trigger.getTrigger());
        removeTrigger(trigger.getTriggerKey());
      }
      /**
       * If the misfire instruction updated the nextfiretime, we determine fire status based on the
       * nextfiretime set.
       */
      boolean executeIfMisfired = (now.after(nextFireTimeAfterUpdate) || now.equals(nextFireTimeAfterUpdate));
      if (!executeIfMisfired) {
        //change the fire state normal since we are not going to be executed.
        trigger.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
      }
      return executeIfMisfired;
    } finally {
      LOG.log(Level.INFO, "Updating misfire information: {0}", trigger.getTriggerKey());
      trigger.update();
    }
  }

  @Override
  public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException {
    if (pauseRequested) {
      return Collections.EMPTY_LIST;
    }
    long start = System.currentTimeMillis();
    try {
      List<OperableTrigger> triggers = new ArrayList<>();
      List<QuartzTrigger> acquiredTriggers = QuartzTrigger.findNextFireTriggers(noLaterThan, maxCount, timeWindow, instance);
      /**
       * we keep track of the jobs that allow concurrent execution.
       *
       * TODO(marembo) This should actually be moved to jpa test on load to load.
       */
      Set<JobKey> jobKeysAcquiredForTrigger = new HashSet<>();
      LOG.log(Level.INFO, "Count of acquired triggers: {0}", acquiredTriggers.size());
      for (QuartzTrigger jt : acquiredTriggers) {
        try {
          getSemaphore().lock(jt.getTriggerKey().toString());
          //at this point, the trigger state may have changed, so reload on acquiring lock.
          /**
           * We want to have a lock per trigger, so that we do not hold a general lock which may
           * starve other actions.
           */
          jt = QuartzTrigger.find(jt.getTriggerKey(), instance);
          if (jt.getFireState() != QuartzTrigger.TriggerFireState.STATE_NORMAL) {
            //state of trigger changed, we continue!
            continue;
          }
          LOG.log(Level.INFO,
                  "Trigger {0} state={1} thread={2}",
                  new Object[]{jt.getTriggerKey().getName(), jt.getFireState(), Thread.currentThread().getName()});
          JobKey jobKey = jt.getJobKey();
          QuartzJobDetail jobDetail = QuartzJobDetail.find(jobKey, instance);
          if (jobDetail.getJobDetail().isConcurrentExectionDisallowed()) {
            if (jobKeysAcquiredForTrigger.contains(jobKey)) {
              continue;
            } else {
              jobKeysAcquiredForTrigger.add(jobKey);
            }
          }
          if (!isExecuteOnMisfire(jt)) {
            continue;
          }
          //what state is the trigger if it is acquired?
          jt.setFireState(QuartzTrigger.TriggerFireState.STATE_ACQUIRED);
          jt.update();
          triggers.add((OperableTrigger) jt.getTrigger());
        } finally {
          getSemaphore().release(jt.getTriggerKey().toString());
        }
      }
      LOG.log(Level.INFO, "Triggers acquired: ......{0}", triggers);
      return triggers;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new JobPersistenceException("Error acquiring triggers", ex);
    } finally {
      long end = System.currentTimeMillis();
      LOG.log(Level.INFO, "Time to acquire trigger: {0}ms", (end - start));
    }
  }

  @Override
  public void releaseAcquiredTrigger(OperableTrigger trigger) {
    try {
      getSemaphore().lock(trigger.getKey().toString());
      LOG.log(Level.INFO, "releaseAcquiredTrigger(OperableTrigger trigger): '{'0'}'{0}", trigger);
      QuartzTrigger jt = QuartzTrigger.find(trigger.getKey(), instance);
      if (jt == null) {
        throw new RuntimeException("trigger does not exists");
      }
      if (jt.getFireState() != QuartzTrigger.TriggerFireState.STATE_ACQUIRED) {
        LOG.log(Level.WARNING, "Trigger {0} cannot be released. Invalid State: {1}", new Object[]{trigger.getKey(), jt.getFireState()});
        return;
      }
      jt.setTriggerState(Trigger.TriggerState.NORMAL);
      jt.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
      jt.update();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new RuntimeException(ex);
    } finally {
      try {
        getSemaphore().release(trigger.getKey().toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers) throws JobPersistenceException {
    try {
      LOG.log(Level.INFO, "triggersFired(List<OperableTrigger> triggers): {0}", triggers.size());
      List<TriggerFiredResult> l = new ArrayList<>();
      for (OperableTrigger ot : triggers) {
        try {
          getSemaphore().lock(ot.getKey().toString());
          LOG.log(Level.INFO, "triggerFired,................{0}", ot);
          QuartzTrigger jt = QuartzTrigger.find(ot.getKey(), instance);
          if (jt.getFireState() != QuartzTrigger.TriggerFireState.STATE_ACQUIRED) {
            LOG.log(Level.WARNING, "Fired trigger {0} is in an invalid state: {10}", new Object[]{jt.getTriggerKey(), jt.getFireState()});
            continue;
          }
          String calendarName = ot.getCalendarName();
          LOG.log(Level.INFO, "Trigger calendar....{0}", calendarName);
          Calendar cc = null;
          if (calendarName != null && !calendarName.trim().isEmpty()) {
            QuartzCalendar jc = QuartzCalendar.findCalendar(calendarName, instance);
            if (jc == null || jc.getCalendar() == null) {
              LOG.warning("Ignoring trigger with specified calendar, yet calendar does not exists");
              continue;
            }
            cc = jc.getCalendar();
          }
          ot.triggered(cc);
          //be sure we are not paused during the time we were acquired.
          if (jt.getFireState() != QuartzTrigger.TriggerFireState.STATE_ACQUIRED) {
            LOG.log(Level.WARNING, "Trigger {0} is in invalid state: {1} Cannot be fired!", new Object[]{jt.getTriggerKey(), jt.getFireState()});
            continue;
          }
          jt.setFireState(QuartzTrigger.TriggerFireState.STATE_EXECUTING);
          QuartzJobDetail jjd = QuartzJobDetail.find(ot.getJobKey(), instance);
          if (jjd == null) {
            LOG.log(Level.WARNING, "Fired trigger {0} without a specifid job detail!", jt.getTriggerKey());
            continue;
          }
          TriggerFiredBundle tfb = new TriggerFiredBundle(
                  jjd.getJobDetail(),
                  ot,
                  cc,
                  false,
                  new Date(),
                  ot.getPreviousFireTime(),
                  ot.getPreviousFireTime(),
                  ot.getNextFireTime()
          );
          l.add(new TriggerFiredResult(tfb));
          jt.update();
        } finally {
          getSemaphore().release(ot.getKey().toString());
        }
      }
      return l;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new JobPersistenceException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, Trigger.CompletedExecutionInstruction triggerInstCode) {
    try {
      getSemaphore().lock(trigger.getKey().toString());
      QuartzTrigger jt = QuartzTrigger.find(trigger.getKey(), instance);
      QuartzJobDetail jjd = QuartzJobDetail.find(jobDetail.getKey(), instance);
      try {
        LOG.log(Level.INFO, "triggeredJobComplete.........................{0}={1}", new Object[]{trigger.getKey(), jobDetail.getJobClass()});
        if (jt == null) {
          //then just throw runtimeexception
          LOG.log(Level.INFO, "Trigger {0} does not exist", trigger.getKey());
          throw new RuntimeException("Trigger: " + trigger.getKey() + " does not exists");
        }
        if (jjd == null) {
          //then just throw runtimeexception
          LOG.log(Level.INFO, "Job detail {0} for trigger {1} does not exist", new Object[]{jobDetail.getKey(), trigger.getKey()});
          throw new RuntimeException("Job " + jobDetail.getKey() + " no longer exists");
        }
        if (jt.getFireState() != QuartzTrigger.TriggerFireState.STATE_EXECUTING) {
          //just show warning, but continue with update if any.
          LOG.log(Level.WARNING, "Trigger is in an invalid state: {0}", jt.getFireState());
        }
        jjd.setJobDetail(jobDetail);
        jt.setTrigger(trigger);
        if (jt.getFireState() == QuartzTrigger.TriggerFireState.STATE_EXECUTING) {
          jt.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
        }
        LOG.log(Level.INFO, "CompletedExecutionInstruction: {0}", triggerInstCode);
        switch (triggerInstCode) {
          case DELETE_TRIGGER:
            jt.delete();
            break;
          case SET_ALL_JOB_TRIGGERS_COMPLETE: {
            List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jobDetail.getKey(), instance);
            for (QuartzTrigger jt_ : jts) {
              jt_.setTriggerState(Trigger.TriggerState.COMPLETE);
              jt_.setFireState(QuartzTrigger.TriggerFireState.STATE_COMPLETE);
            }
          }
          break;
          case SET_ALL_JOB_TRIGGERS_ERROR: {
            List<QuartzTrigger> jts = QuartzTrigger.findTriggersForJobKey(jobDetail.getKey(), instance);
            for (QuartzTrigger jt_ : jts) {
              jt_.setTriggerState(Trigger.TriggerState.ERROR);
              jt_.setFireState(QuartzTrigger.TriggerFireState.STATE_ERROR);
            }
          }
          break;
          case SET_TRIGGER_COMPLETE:
            jt.setTriggerState(Trigger.TriggerState.COMPLETE);
            jt.setFireState(QuartzTrigger.TriggerFireState.STATE_COMPLETE);
            break;
          case SET_TRIGGER_ERROR:
            jt.setTriggerState(Trigger.TriggerState.ERROR);
            jt.setFireState(QuartzTrigger.TriggerFireState.STATE_ERROR);
            break;
        }
        if (triggerInstCode != Trigger.CompletedExecutionInstruction.RE_EXECUTE_JOB) {
          signaler.signalSchedulingChange(0l);
        }
      } finally {
        if (jobDetail.isPersistJobDataAfterExecution()) {
          jjd.update();
        }
        jt.update();
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      if (RuntimeException.class.isAssignableFrom(ex.getClass())) {
        throw (RuntimeException) ex;
      }
      throw new RuntimeException(ex);
    } finally {
      try {
        getSemaphore().release(trigger.getKey().toString());
      } catch (LockException ex) {
        Logger.getLogger(SchedulerJobStore.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public void setInstanceId(String schedInstId) {
    instanceId = schedInstId;
    if (instanceId != null && !instanceId.trim().isEmpty()) {
      //find the job instance
      initInstance();
    }
  }

  private void initInstance() {
    try {
      instance = QuartzInstance.find(instanceId);
      if (instance == null) {
        instance = new QuartzInstance();
        instance.setInstanceId(instanceId);
      }
      instance.setInstanceName(instanceName);
      instance.setThreadPoolSize(threadPoolSize);
      instance.createOrUpdate();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, instanceId, ex);
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void setInstanceName(String schedName) {
    instanceName = schedName;
    if (instanceId != null && !instanceId.trim().isEmpty()) {
      initInstance();
    }
  }

  @Override
  public void setThreadPoolSize(int poolSize) {
    this.threadPoolSize = poolSize;
    if (instanceId != null && !instanceId.trim().isEmpty()) {
      initInstance();
    }
  }

  public void setMisfireThreshold(int misfireThreshold) {
    LOG.log(Level.INFO, "misfireThreshold: {0}", misfireThreshold);
    this.misfireThreshold = misfireThreshold;
  }

  public long getMisfireThreshold() {
    return misfireThreshold;
  }

  private static void printDate(Date date, String message) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String str = dateFormat.format(date);
    LOG.log(Level.INFO, "{0}: {1}", new Object[]{message, str});
  }
}
