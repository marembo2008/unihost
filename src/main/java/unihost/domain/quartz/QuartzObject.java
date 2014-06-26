/**
 * $Id$
 */
package unihost.domain.quartz;

import java.util.logging.Logger;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import static unihost.Domain.getEntityManager;
import unihost.domain.Object;

/**
 * This class is here since we cannot change the unihost.domain.Object class, so as to add
 * functionalities to specific jpa quartz entities.
 */
@Entity
@NamedQueries({
  @NamedQuery(name = "QuartzObject.deleteAllQuartsSchedlingData", query = "DELETE FROM QuartzObject t")
})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "U_SCHEDULER_OBJECTS")
public abstract class QuartzObject extends unihost.domain.Object {

  private static final Logger LOG = Logger.getLogger(QuartzObject.class.getName());

  private static final int MAX_RETRY_COUNT = 3;
  @Transient
  private boolean deleted;

  public synchronized final void createOrUpdate() {
    if (find(this.getUuid(), getClass()) == null) {
      create();
    } else {
      update();
    }
  }

  public static <T extends unihost.domain.Object> T find(java.lang.Object id, Class<T> entityClass) {
    return getEntityManager().find(entityClass, id);
  }

  public static <T extends QuartzObject> int count(Class<T> entityClass) {
    /**
     * we could have used named queries here, but writing named queries on all entity classes just
     * to count their instances saved in the database is ridiculous.
     */
    javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
    javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
    cq.select(getEntityManager().getCriteriaBuilder().count(rt));
    javax.persistence.Query q = getEntityManager().createQuery(cq);
    return ((Long) q.getSingleResult()).intValue();
  }

  public synchronized static void deleteAllQuartsSchedlingData() {
    getEntityManager()
            .createNamedQuery("QuartzObject.deleteAllQuartsSchedlingData")
            .executeUpdate();
  }

  @Override
  public synchronized Object update() {
    if (!deleted) {
      return super.update();
    }
    return null;
  }

  @Override
  public synchronized void delete() {
    super.delete();
    this.deleted = true;
  }

}
