/**
 * $Id$
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import java.util.List;
import java.util.TimeZone;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.quartz.Calendar;
import org.quartz.JobPersistenceException;
import org.quartz.impl.calendar.BaseCalendar;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;
import unihost.domain.quartz.trigger.QuartzTrigger;

@Entity
@NamedQueries({
  @NamedQuery(name = "QuartzCalendar.findCalendar",
          query = "SELECT c FROM QuartzCalendar c WHERE c.name = :name  AND c.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzCalendar.findCalendarNames",
          query = "SELECT c.name FROM QuartzCalendar c WHERE c.name = :name  AND c.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzCalendar.countCalendars",
          query = "SELECT c FROM QuartzCalendar c WHERE c.instance.uuid = :instance")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "U_SCHEDULER_CALENDARS")
@DiscriminatorColumn(length = 25)
public abstract class QuartzCalendar extends unihost.domain.quartz.QuartzObject implements Serializable {

  private static final long serialVersionUID = 938343483493431L;
  @Column(name = "CALENDAR_NAME")
  private String name;
  @ManyToOne
  private QuartzInstance instance;
  @ManyToOne(cascade = CascadeType.ALL)
  private QuartzCalendar baseCalendar;
  @Column(name = "DESCRIPTION")
  private String description;
  @Column(name = "TIMEZONE")
  @Lob
  protected TimeZone timeZone;
  @Transient
  protected Calendar calendar;

  public Calendar getCalendar() {
    return calendar;
  }

  public void setCalendar(Calendar calendar) {
    this.calendar = calendar;
  }

  @PrePersist
  @PreUpdate
  void onSave() {
    BaseCalendar bc = (BaseCalendar) this.calendar;
    this.description = bc.getDescription();
    this.timeZone = bc.getTimeZone();
    if (bc.getBaseCalendar() != null) {
      if (baseCalendar == null) {
        baseCalendar = createInstance(bc.getBaseCalendar(), "", instance);
      }
      //will be updated or creatd as necessary
      baseCalendar.calendar = bc.getBaseCalendar();
    }
    onSaveSelf();
  }

  @PostLoad
  void onLoad() {
    onLoadSelf();
    calendar.setDescription(description);
    if (this.baseCalendar != null) {
      calendar.setBaseCalendar(this.baseCalendar.calendar);
    }
    ((BaseCalendar) calendar).setTimeZone(timeZone);
  }

  protected abstract void onSaveSelf();

  /**
   * Must initialize the right quartz calendar type.
   */
  protected abstract void onLoadSelf();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setInstance(QuartzInstance Instance) {
    this.instance = Instance;
  }

  public QuartzInstance getInstance() {
    return instance;
  }

  public QuartzCalendar getBaseCalendar() {
    return baseCalendar;
  }

  public void setBaseCalendar(QuartzCalendar baseCalendar) {
    this.baseCalendar = baseCalendar;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  @SuppressWarnings("UseSpecificCatch")
  public static QuartzCalendar createInstance(Calendar cal, String name, QuartzInstance schedulerInstance) {
    String className = "unihost.domain.quartz.calendar.Quartz" + cal.getClass().getSimpleName();
    try {
      QuartzCalendar calendar = (QuartzCalendar) Class.forName(className).newInstance();
      calendar.setName(name);
      calendar.instance = schedulerInstance;
      return calendar;
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static QuartzCalendar findCalendar(String name, QuartzInstance schedulerInstance) {
    try {
      return Domain.getEntityManager()
              .createNamedQuery("QuartzCalendar.findCalendar", QuartzCalendar.class)
              .setParameter("name", name)
              .setParameter("instance", schedulerInstance.getUuid())
              .getSingleResult();
    } catch (NoResultException ex) {
      //ignore
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public static void removeCalendar(String name, QuartzInstance schedulerInstance) throws JobPersistenceException {
    if (QuartzTrigger.hasTriggers(name, schedulerInstance)) {
      throw new JobPersistenceException("Integrity violation exception. Calendar in use by triggers");
    }
    QuartzCalendar jc = findCalendar(name, schedulerInstance);
    Domain.getEntityManager()
            .remove(jc);
  }

  public static int getCalendarCount(QuartzInstance schedulerInstance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzCalendar.findCalendar", Long.class)
            .setParameter("instance", schedulerInstance.getUuid())
            .getSingleResult().intValue();
  }

  public static List<String> findCalendarNames(QuartzInstance schedulerInstance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzCalendar.findCalendarNames")
            .setParameter("instance", schedulerInstance.getUuid())
            .getResultList();
  }
}
