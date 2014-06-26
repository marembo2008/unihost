/**
 * $Id$
 */
package unihost.domain.quartz;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import unihost.Domain;

/**
 * The quartz scheduler operates per instance. By default there is only one instance, but it seems
 * several is possible.
 */
@Entity
@Table(name = "U_SCHEDULER_INSTANCES")
@NamedQueries({
  @NamedQuery(name = "QuartzInstance.findQuartzInstance", query = "SELECT i FROM QuartzInstance i WHERE i.instanceId = :instanceId")
})
public class QuartzInstance extends QuartzObject implements Serializable {

  private static final long serialVersionUID = 47384783493L;
  @Column(name = "INSTANCE_ID", unique = true, length = 40)
  private String instanceId;
  @Column(name = "SCHED_NAME")
  private String instanceName;
  private int threadPoolSize;

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  public static QuartzInstance find(String instanceId) {
    try {
      return Domain.getEntityManager()
              .createNamedQuery("QuartzInstance.findQuartzInstance", QuartzInstance.class)
              .setParameter("instanceId", instanceId)
              .getSingleResult();
    } catch (Exception e) {
      if (!NoResultException.class.isAssignableFrom(e.getClass())) {
        throw new RuntimeException(e);
      }
      return null;
    }
  }

  @Override
  public String toString() {
    return "QuartzInstance{" + "instanceId=" + instanceId + ", instanceName=" + instanceName + '}';
  }

}
