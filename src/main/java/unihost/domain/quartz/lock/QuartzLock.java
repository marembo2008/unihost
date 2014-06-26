/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.lock;

import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;
import unihost.domain.quartz.QuartzObject;

/**
 *
 * @author marembo
 */
@Entity
@NamedQueries({
  @NamedQuery(name = "QuartzLock.findLock", query = "SELECT l FROM QuartzLock l WHERE l.lockName = :lockName AND l.instanceId = :instanceId")
})
@NamedNativeQueries({
  @NamedNativeQuery(name = "QuartzLock.tryLock", query = "SELECT * FROM U_SCHEDULER_LOCKS WHERE LOCKNAME = ?1 AND INSTANCE_ID = ?2 FOR UPDATE")
})
@Table(name = "U_SCHEDULER_LOCKS")
@Cacheable(false)
public class QuartzLock extends QuartzObject implements Serializable {

  @Column(name = "INSTANCE_ID", length = 40)
  private String instanceId;
  @Column(name = "SCHED_NAME")
  private String instanceName;
  private String lockName;

  public QuartzLock() {
  }

  public QuartzLock(QuartzInstance instance, String lockName) {
    this.instanceId = instance.getInstanceId();
    this.instanceName = instance.getInstanceName();
    this.lockName = lockName;
  }

  @Override
  public String toString() {
    return "QuartzLock{" + "instanceId=" + instanceId + ", instanceName=" + instanceName + ", lockName=" + lockName + '}';
  }

  public static QuartzLock find(String lockName, String instanceId) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzLock.findLock", QuartzLock.class)
            .setParameter("lockName", lockName)
            .setParameter("instanceId", instanceId)
            .getSingleResult();
  }

}
