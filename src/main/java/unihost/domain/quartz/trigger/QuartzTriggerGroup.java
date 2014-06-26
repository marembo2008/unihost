/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;

@Entity
@Table(name = "U_SCHEDULER_TRIGGER_GROUPS")
@NamedQueries({
  @NamedQuery(name = "QuartzTriggerGroup.findTriggerGroup",
          query = "SELECT g FROM QuartzTriggerGroup g WHERE g.name = :name AND g.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTriggerGroup.findTriggerGroups",
          query = "SELECT g FROM QuartzTriggerGroup g WHERE g.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTriggerGroup.findTriggerGroupsByName",
          query = "SELECT g.name FROM QuartzTriggerGroup g WHERE g.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzTriggerGroup.isTriggerGroupPaused",
          query = "SELECT g.state FROM QuartzTriggerGroup g WHERE g.name = :name AND g.instance.uuid = :instance")
})
public class QuartzTriggerGroup extends unihost.domain.quartz.QuartzObject implements Serializable {

  public static enum GroupState {

    PAUSED,
    NORMAL;
  }
  private static final long serialVersionUID = 1349480394L;
  @ManyToOne
  private QuartzInstance instance;
  private String name;
  @Column(name = "GROUP_STATE")
  private GroupState state;

  public QuartzTriggerGroup() {
  }

  public QuartzTriggerGroup(QuartzInstance instance, String name) {
    this.instance = instance;
    this.name = name;
    this.state = GroupState.NORMAL;
  }

  public void pauseGroup() {
    state = GroupState.PAUSED;
    update();
  }

  public void resumeGroup() {
    state = GroupState.NORMAL;
    update();
  }

  public boolean isPaused() {
    return state == GroupState.PAUSED;
  }

  public static QuartzTriggerGroup getTriggerGroup(String groupName, QuartzInstance schedulerInstance) {
    try {
      return Domain.getEntityManager()
              .createNamedQuery("QuartzTriggerGroup.findTriggerGroup", QuartzTriggerGroup.class)
              .setParameter("name", groupName)
              .setParameter("instance", schedulerInstance.getUuid())
              .getSingleResult();
    } catch (NoResultException ex) {
      //ignore
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<QuartzTriggerGroup> findAllTriggerGroups(QuartzInstance schedulerInstance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTriggerGroup.findTriggerGroups")
            .setParameter("instance", schedulerInstance.getUuid())
            .getResultList();
  }

  public static void pauseTriggerGroups(QuartzInstance schedulerInstance) {
    List<QuartzTriggerGroup> groups = findAllTriggerGroups(schedulerInstance);
    for (QuartzTriggerGroup g : groups) {
      g.pauseGroup();
    }
//    QuartzTrigger.pauseTriggersInPausedGroups(schedulerInstance);
  }

  public static void resumeTriggerGroups(QuartzInstance schedulerInstance) {
    List<QuartzTriggerGroup> groups = findAllTriggerGroups(schedulerInstance);
    for (QuartzTriggerGroup g : groups) {
      g.resumeGroup();
    }
//    QuartzTrigger.resumeTriggersInResumedGroups(schedulerInstance);
  }

  public static boolean isTriggerGroupPaused(String groupName, QuartzInstance schedulerInstance) {
    try {
      return getTriggerGroup(groupName, schedulerInstance).isPaused();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> findTriggerGroups(QuartzInstance schedulerInstance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzTriggerGroup.findTriggerGroupsByName")
            .setParameter("instance", schedulerInstance.getUuid())
            .getResultList();
  }
}
