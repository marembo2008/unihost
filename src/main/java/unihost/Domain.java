/**
 * $Id$
 */
package unihost;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import unihost.util.Properties;

public final class Domain {

  private static final EntityManagerFactory entityManagerFactory;

  static {
    entityManagerFactory = Persistence.createEntityManagerFactory("unihost", Properties.getAll("^openjpa.*"));
  }

  private Domain() {
  }
  private static final ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<>();

  public static EntityManagerFactory getEntityManagerFactory() {
    return entityManagerFactory;
  }

  public static EntityManager getEntityManager() {
    EntityManager entityManager = Domain.threadLocalEntityManager.get();
    if (entityManager == null) {
      entityManager = entityManagerFactory.createEntityManager();
      threadLocalEntityManager.set(entityManager);
    }
    return entityManager;
  }

  /**
   * Must be called at the end of each servlet request to free any resources used.
   */
  public static void closeEntityManager() {
    EntityManager entityManager = Domain.threadLocalEntityManager.get();
    if (entityManager != null) {
      entityManager.close();
      threadLocalEntityManager.set(null);
    }
  }

  public static void closeEntityManagerFactory() {
    entityManagerFactory.close();
  }

}
