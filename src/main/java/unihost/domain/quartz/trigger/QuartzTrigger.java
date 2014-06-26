/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.AbstractTrigger;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;
import unihost.domain.quartz.QuartzJobData;
import unihost.domain.quartz.calendar.QuartzCalendar;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NamedQueries({
  @NamedQuery(name = "QuartzTrigger.findTriggersByJobKey",
          query = "SELECT t FROM QuartzTrigger t WHERE t.jobKeyName = :jobKeyName AND t.jobKeyGroup = :jobKeyGroup AND t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.findTriggerState",
          query = "SELECT t.triggerState FROM QuartzTrigger t "
          + "WHERE t.group.name = :triggerGroup AND t.name = :triggerName AND t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.findTrigger",
          query = "SELECT t FROM QuartzTrigger t WHERE t.group.name = :triggerGroup AND t.name = :triggerName AND t.instance.uuid = :instance"),
  /**
   * We obtain triggers which is not currently executing, and only if the job to be executed by the
   * trigger is currently not being executed by any other trigger.
   */
  @NamedQuery(name = "QuartzTrigger.findNextFireTriggersWithNextFireTimeNoLaterThan",
          query = "SELECT t FROM QuartzTrigger t "
          + "WHERE t.startTime <= :now AND t.nextFireTime <= :noLaterThan AND t.group.state = :groupState "
          + "AND t.fireState = :fireState AND t.instance.uuid = :instance "
          + "ORDER BY t.nextFireTime ASC"),
  @NamedQuery(name = "QuartzTrigger.findNextFireTriggers",
          query = "SELECT t FROM QuartzTrigger t WHERE t.startTime <= nows AND (t.endTime IS NULL OR t.endTime >= :nowe) "
          + "AND t.fireState = :fireState AND t.group.state = :groupState AND t.instance.uuid = :instance ORDER BY t.fireState, t.nextFireTime ASC"),
  @NamedQuery(name = "QuartzTrigger.findTriggers",
          query = "SELECT t FROM QuartzTrigger t WHERE t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.countTriggersByFireState",
          query = "SELECT COUNT(t) FROM QuartzTrigger t WHERE t.fireState <> :fireState"),
  @NamedQuery(name = "QuartzTrigger.findTriggerKeys",
          query = "SELECT t.triggerKey FROM QuartzTrigger t WHERE t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.countTrigger",
          query = "SELECT COUNT(t) FROM QuartzTrigger t WHERE t.group.name = :triggerGroup AND t.name = :triggerName AND t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.countTriggers",
          query = "SELECT COUNT(t) FROM QuartzTrigger t WHERE t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.findTriggerWithCalendar",
          query = "SELECT t FROM QuartzTrigger t WHERE t.calendarName = :name AND t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.countTriggerWithCalendar",
          query = "SELECT COUNT(t) FROM QuartzTrigger t WHERE t.calendarName = :name AND t.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTrigger.pauseTriggersInGroup",
          query = "UPDATE QuartzTrigger t SET t.fireState = :fireState WHERE t.group.state = :state AND t.instance.uuid = :instance")
})
@NamedNativeQueries({
  @NamedNativeQuery(name = "QuartzTrigger.updateTriggersState",
          query = "UPDATE U_SCHEDULER_TRIGGERS AS t SET t.groupPaused = ?1, t.fireState = ?2")
})
@Table(name = "U_SCHEDULER_TRIGGERS")
public abstract class QuartzTrigger extends unihost.domain.quartz.QuartzObject implements Serializable {

  private static final Logger LOG = Logger.getLogger(QuartzTrigger.class.getName());

  public static enum TriggerFireState {

    STATE_NORMAL,
    STATE_ACQUIRED,
    STATE_EXECUTING,
    STATE_COMPLETE,
    STATE_PAUSED,
    STATE_BLOCKED,
    STATE_PAUSED_BLOCKED,
    STATE_ERROR;
  }
  private static final long serialVersionUID = 1344483493L;
  private Long startTime;
  private Long endTime;
  private Long nextFireTime;
  private Long previousFireTime;
  private Long finalFireTime;
  @Column(name = "TRIGGER_NAME")
  private String name;
  private QuartzTriggerKey triggerKey;
  @OneToOne
  private QuartzInstance instance;
  private String fireInstanceId;
  private String jobKeyName;
  private String jobKeyGroup;
  @ManyToOne(cascade = {CascadeType.PERSIST})
  private QuartzTriggerGroup group;
  /**
   * Job Data Map specific to this quartz trigger.
   */
  @OneToOne(cascade = CascadeType.ALL)
  private QuartzJobData jobData;
  @Column(name = "DESCRIPTION")
  private String description;
  @Column(name = "PRIORITY")
  private int priority;
  @OneToOne(cascade = CascadeType.ALL)
  private QuartzCalendar calendar;
  private int misfireInstruction;
  private TriggerState triggerState = TriggerState.NONE;
  private TriggerFireState fireState;
  private String calendarName;
  @Transient
  private TriggerKey triggerKey_;
  /**
   * Generally to be initialized on load by the subclasses.
   */
  @Transient
  protected Trigger trigger_;

  protected QuartzTrigger() {
    fireState = TriggerFireState.STATE_NORMAL;
  }

  @PrePersist
  @PreUpdate
  void onSave() {
    if (trigger_ != null) {
      edit0();
      LOG.log(Level.INFO, "Updating trigger {0} state={1}, thread={2}", new Object[]{triggerKey_, fireState, Thread.currentThread().getName()});
    }
  }

  @PostLoad
  void onLoad() {
    initTrigger();
    triggerKey_ = new TriggerKey(triggerKey.getName(), triggerKey.getGroup());
    //set the various trigger options now.
    AbstractTrigger at = (AbstractTrigger) trigger_;
    at.setCalendarName(calendarName);
    at.setDescription(description);
    at.setKey(triggerKey_);
    at.setMisfireInstruction(misfireInstruction);
    at.setPriority(priority);
    if (startTime != null) {
      at.setStartTime(new Date(startTime));
    }
    if (endTime != null) {
      at.setEndTime(new Date(endTime));
    }
    if (nextFireTime != null) {
      at.setNextFireTime(new Date(nextFireTime));
    }
    if (previousFireTime != null) {
      at.setPreviousFireTime(new Date(previousFireTime));
    }
    at.setGroup(triggerKey_.getGroup());
    at.setName(triggerKey_.getName());
    at.setFireInstanceId(fireInstanceId);
    at.setJobGroup(jobKeyGroup);
    at.setJobKey(new JobKey(jobKeyName, jobKeyGroup));
    at.setJobName(jobKeyName);
    if (jobData != null) {
      at.setJobDataMap(jobData.getDataMap());
    }
  }

  private void edit0() {
    this.description = trigger_.getDescription();
    this.triggerKey = QuartzTriggerKey.getInstance(trigger_.getKey());
    if (group == null) {
      group = QuartzTriggerGroup.getTriggerGroup(trigger_.getKey().getGroup(), instance);
      if (group == null) {
        group = new QuartzTriggerGroup(instance, triggerKey.getGroup());
      }
    }
    this.name = triggerKey.getName();
    this.misfireInstruction = trigger_.getMisfireInstruction();
    this.priority = trigger_.getPriority();
    this.calendarName = trigger_.getCalendarName();
    AbstractTrigger at = (AbstractTrigger) trigger_;
    this.fireInstanceId = at.getFireInstanceId();
    if (at.getJobDataMap() != null) {
      if (jobData == null) {
        jobData = new QuartzJobData(at.getJobDataMap());
      } else {
        jobData.setDataMap(at.getJobDataMap());
      }
    }
    this.startTime = at.getStartTime() == null ? null : at.getStartTime().getTime();
    this.endTime = at.getEndTime() == null ? null : at.getEndTime().getTime();
    this.previousFireTime = at.getPreviousFireTime() == null ? null : at.getPreviousFireTime().getTime();
    this.nextFireTime = at.getNextFireTime() == null ? null : at.getNextFireTime().getTime();
    this.finalFireTime = at.getFinalFireTime() == null ? null : at.getFinalFireTime().getTime();
    edit(trigger_);
  }

  @SuppressWarnings("null")
  public final void setTrigger(Trigger trigger) {
    this.trigger_ = trigger;
  }

  /**
   * Most important, initializes the trigger type.
   */
  protected abstract void initTrigger();

  /**
   * Sets the relevant trigger type instance of this trigger.
   *
   * @param trigger
   */
  protected abstract void edit(Trigger trigger);

  /**
   * Returns true if this trigger can be fired now.
   *
   * @return
   */
  public abstract boolean canFireNow();

  public void setFireState(TriggerFireState fireState) {
    this.fireState = fireState;
  }

  public TriggerFireState getFireState() {
    return fireState;
  }

  public Date getNextFireTime() {
    if (nextFireTime == null) {
      return null;
    }
    return new Date(nextFireTime);
  }

  public void setTriggerState(TriggerState triggerState) {
    this.triggerState = triggerState;
  }

  public TriggerState getTriggerState() {
    return triggerState;
  }

  public TriggerKey getTriggerKey() {
    return triggerKey_;
  }

  public QuartzInstance getInstance() {
    return instance;
  }

  public void setJobKey(JobKey jobKey) {
    this.jobKeyGroup = jobKey.getGroup();
    this.jobKeyName = jobKey.getName();
  }

  public JobKey getJobKey() {
    return new JobKey(jobKeyName, jobKeyGroup);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public String getFireInstanceId() {
    return fireInstanceId;
  }

  public void setFireInstanceId(String fireInstanceId) {
    this.fireInstanceId = fireInstanceId;
  }

  public String getCalendarName() {
    return calendarName;
  }

  public void setCalendarName(String calendarName) {
    this.calendarName = calendarName;
  }

  public QuartzCalendar getCalendar() {
    return calendar;
  }

  public void setCalendar(QuartzCalendar calendar) {
    this.calendar = calendar;
  }

  public int getMisfireInstruction() {
    return misfireInstruction;
  }

  public void setMisfireInstruction(int misfireInstruction) {
    this.misfireInstruction = misfireInstruction;
  }

  /**
   * Returns the concrete quartz trigger instance.
   *
   * @return
   */
  public final Trigger getTrigger() {
    return trigger_;
  }

  public static QuartzTrigger find(TriggerKey key, QuartzInstance instance) {
    List<QuartzTrigger> triggers = Domain.getEntityManager().
            createNamedQuery("QuartzTrigger.findTrigger", QuartzTrigger.class)
            .setParameter("triggerGroup", key.getGroup())
            .setParameter("triggerName", key.getName())
            .setParameter("instance", instance.getUuid())
            .getResultList();
    return (QuartzTrigger) (triggers.isEmpty() ? null : triggers.get(0).refresh());
  }

  public static QuartzTrigger createInstance(Trigger newtTrigger, QuartzInstance instance) throws JobPersistenceException {
    Class triggerClass = newtTrigger.getClass();
    QuartzTrigger trigger = null;
    if (SimpleTrigger.class.isAssignableFrom(triggerClass)) {
      trigger = new QuartzSimpleTrigger();
    } else if (CronTrigger.class.isAssignableFrom(triggerClass)) {
      trigger = new QuartzCronTrigger();
    } else if (CalendarIntervalTrigger.class.isAssignableFrom(triggerClass)) {
      trigger = new QuartzCalendarIntervalTrigger();
    } else if (DailyTimeIntervalTrigger.class.isAssignableFrom(triggerClass)) {
      trigger = new QuartzDailyTimeIntervalTrigger();
    } else {
      throw new JobPersistenceException("Unable to instantiate trigger type");
    }
    trigger.instance = instance;
    return trigger;
  }

  public static void remove(TriggerKey key, QuartzInstance instance) throws JobPersistenceException {
    QuartzTrigger trigger = find(key, instance);
    if (trigger == null) {
      throw new JobPersistenceException("Trigger " + key + " does not exists");
    }
    Domain.getEntityManager().remove(trigger);
  }

  public static boolean exists(TriggerKey key, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.countTrigger", Long.class)
            .setParameter("triggerGroup", key.getGroup())
            .setParameter("triggerName", key.getName())
            .setParameter("instance", instance.getUuid())
            .getSingleResult() > 0;
  }

  public static boolean hasTriggers(String calendarName, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.countTriggerWithCalendar", Long.class)
            .setParameter("name", calendarName)
            .setParameter("instance", instance.getUuid())
            .getSingleResult() > 0;
  }

  public static int getTriggerCount(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.countTriggers", Long.class)
            .setParameter("instance", instance.getUuid())
            .getSingleResult().intValue();
  }

  public static List<QuartzTriggerKey> findTriggerKeys(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.findTriggerKeys")
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  public static List<QuartzTrigger> findTriggers(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.findTriggers")
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  /**
   * Returns triggers that are not in normal state.
   *
   * @param instance
   * @return
   */
  public static boolean findActiveTriggers(QuartzInstance instance) {
    for (QuartzTrigger t : findTriggers(instance)) {
      if (t.getFireState() == TriggerFireState.STATE_ACQUIRED
              || t.getFireState() == TriggerFireState.STATE_EXECUTING) {
        return true;
      }
    }
    return false;
//    return Domain.getEntityManager()
//            .createNamedQuery("QuartzTrigger.countTriggersByFireState", Long.class)
//            .setParameter("fireState", TriggerFireState.STATE_NORMAL)
//            .getSingleResult().intValue() > 0;
  }

  public static List<QuartzTrigger> findTriggersForJobKey(JobKey jobKey, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.findTriggersByJobKey")
            .setParameter("jobKeyName", jobKey.getName())
            .setParameter("jobKeyGroup", jobKey.getGroup())
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  public static TriggerState getTriggerState(TriggerKey key, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.findTriggerState", TriggerState.class)
            .setParameter("triggerGroup", key.getGroup())
            .setParameter("triggerName", key.getName())
            .setParameter("instance", instance.getUuid())
            .getSingleResult();
  }

  public static List<QuartzTrigger> findNextFireTriggers(long noLaterThan, int maxCount, long timeWindow, QuartzInstance instance) {
    /**
     * TODO(marembo): What is timewindow?
     */
    LOG.log(Level.INFO,
            "findNextFireTriggers(long noLaterThan: {0}, int maxCount: {1}, long timeWindow: {2})",
            new Object[]{noLaterThan, maxCount, timeWindow});
    if (maxCount <= 0) {
      maxCount = 1;
    }
    Calendar c = Calendar.getInstance();
    LOG.log(Level.INFO, "Now: {0}", c.getTimeInMillis());
    List<QuartzTrigger> triggers;
    if (noLaterThan > 0) {
      triggers = Domain.getEntityManager()
              .createNamedQuery("QuartzTrigger.findNextFireTriggersWithNextFireTimeNoLaterThan")
              .setParameter("noLaterThan", noLaterThan)
              .setParameter("now", c.getTimeInMillis())
              .setParameter("fireState", TriggerFireState.STATE_NORMAL)
              .setParameter("groupState", QuartzTriggerGroup.GroupState.NORMAL)
              .setParameter("instance", instance.getUuid())
              .setMaxResults(maxCount)
              .getResultList();
    } else {
      triggers = Domain.getEntityManager()
              .createNamedQuery("QuartzTrigger.findNextFireTriggers")
              .setParameter("fireState", TriggerFireState.STATE_NORMAL)
              .setParameter("nows", c.getTimeInMillis())
              .setParameter("nowe", c.getTimeInMillis())
              .setParameter("groupState", QuartzTriggerGroup.GroupState.NORMAL)
              .setParameter("instance", instance.getUuid())
              .setMaxResults(maxCount)
              .getResultList();
    }
    for (QuartzTrigger qt : triggers) {
      qt.refresh();
    }
    return triggers;
  }

  public static List<QuartzTrigger> findTriggers(String calendarName, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTrigger.findTriggerWithCalendar")
            .setParameter("name", calendarName)
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  @Override
  public synchronized QuartzTrigger update() {
    if (find(getUuid(), getClass()) == null) {
      LOG.log(Level.INFO, "Trigger {0} already deleted", getTriggerKey());
      return this;
    }
    Trigger t = trigger_;
    TriggerFireState fireState_ = fireState;
    TriggerState state_ = triggerState;
    setTrigger(t);
    if (!group.isPaused()) {
      //we only reset if the group is paused.
      fireState = fireState_;
      triggerState = state_;
      return (QuartzTrigger) super.update(this); //To change body of generated methods, choose Tools | Templates.
    }
    return this;
  }

  public synchronized QuartzTrigger update(boolean force) {
    //probably the trigger has been deleted.
    if (find(getUuid(), getClass()) == null) {
      LOG.log(Level.INFO, "Trigger {0} already deleted", getTriggerKey());
      return this;
    }
    Trigger t = trigger_;
    TriggerFireState fireState_ = fireState;
    TriggerState state_ = triggerState;
    setTrigger(t);
    if (!group.isPaused() || force) {
      //we only reset if the group is paused.
      fireState = fireState_;
      triggerState = state_;
      return (QuartzTrigger) super.update(this); //To change body of generated methods, choose Tools | Templates.
    }
    return this;
  }

  public static void pauseTriggersInPausedGroups(QuartzInstance instance) {
    for (QuartzTrigger t : findTriggers(instance)) {
      if (t.group.isPaused()) {
        t.setFireState(QuartzTrigger.TriggerFireState.STATE_PAUSED);
        t.update(true);
      }
    }
  }

  public static void resumeTriggersInResumedGroups(QuartzInstance instance) {
    for (QuartzTrigger t : findTriggers(instance)) {
      if (!t.group.isPaused()) {
        t.setFireState(QuartzTrigger.TriggerFireState.STATE_NORMAL);
        t.update(true);
      }
    }
  }

}
