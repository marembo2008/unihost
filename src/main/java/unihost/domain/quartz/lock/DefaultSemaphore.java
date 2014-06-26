/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import unihost.Domain;
import unihost.domain.quartz.QuartzInstance;

/**
 *
 * @author marembo
 */
public class DefaultSemaphore implements Semaphore {

  private static final Logger LOG = Logger.getLogger(DefaultSemaphore.class.getName());
  private final Object SYNC_LOCK = new Object();
  private final QuartzInstance instance;
  private static final ThreadLocal<Map<String, QuartzLock>> ACQUIRED_LOCKS = new ThreadLocal<>();

  public DefaultSemaphore(QuartzInstance instance) {
    this.instance = instance;
  }

  private QuartzLock tryAcquireLock(String lockName) {
    try {
      QuartzLock lock = Domain.getEntityManager()
              .createNamedQuery("QuartzLock.findLock", QuartzLock.class)
              .setParameter("lockName", lockName)
              .setParameter("instanceId", instance.getInstanceId())
              .getSingleResult();
      Domain.getEntityManager()
              .createNamedQuery("QuartzLock.tryLock", QuartzLock.class)
              .setParameter(1, lockName)
              .setParameter(2, instance.getInstanceId())
              .getSingleResult();
      return lock;
    } catch (NoResultException ex) {
      //it means the row is empty, insert it.
      QuartzLock lock = new QuartzLock(instance, lockName);
      lock.create();
      //we create the lock and then try to acquire it again.
      return tryAcquireLock(lockName);
    }
  }

  @Override
  @SuppressWarnings({"SleepWhileInLoop", "SleepWhileHoldingLock"})
  public void lock(String lockName) throws LockException {
    long start = System.currentTimeMillis();
    try {
      Throwable cause = null;
      int count = 0;
      int maxCount = 5;
      QuartzLock lock;
      do {
        count++;
        try {
          LOG.log(Level.INFO, "{0} is attempting to acquire lock: {1}", new Object[]{Thread.currentThread().getName(), lockName});
          if (!hasLock(lockName)) {
            //the only thing that can happen is that it does not get the lock/
            lock = tryAcquireLock(lockName);
            if (lock == null) {
              if (count < maxCount) {
                // pause a bit to give another thread some time to commit the insert of the new lock row
                try {
                  Thread.sleep(1000L);
                } catch (InterruptedException ignore) {
                  Thread.currentThread().interrupt();
                }
                // try again ...
                continue;
              } else {
                throw new LockException("Unable to acquire lock after 5 retires: " + Thread.currentThread().getName());
              }
            }
            Map<String, QuartzLock> locks = ACQUIRED_LOCKS.get();
            if (locks == null) {
              locks = new HashMap<>();
              ACQUIRED_LOCKS.set(locks);
            }
            locks.put(lockName, lock);
            LOG.log(Level.INFO, "{0} acquired lock: {1}", new Object[]{Thread.currentThread().getName(), lock});
            //lock obtained, simply return.
            return;
          } else {
            LOG.log(Level.INFO, "Lock {0} is already owned by: {1}", new Object[]{lockName, Thread.currentThread().getName()});
            return; //already owning the lock
          }
        } catch (Exception ex) {
          if (cause == null) {
            cause = ex;
          }
          if (count < maxCount) {
            // pause a bit to give another thread some time to commit the insert of the new lock row
            try {
              Thread.sleep(1000L);
            } catch (InterruptedException ignore) {
              Thread.currentThread().interrupt();
            }
            // try again ...
            continue;
          }
          throw new LockException("Unable to obtain lock after 5 retries", cause);
        }
      } while (count < maxCount);
      throw new LockException("Unable to obtain lock after 5 retries", cause);
    } finally {
      long last = System.currentTimeMillis();
      LOG.log(Level.INFO, "Lock acquisition time: {0}ms", (last - start));
    }
  }

  @Override
  public void release(String lockName) throws LockException {
    try {
      if (hasLock(lockName)) {
        LOG.log(Level.INFO, "Releasing lock {0} acquired by: {1}", new Object[]{lockName, Thread.currentThread().getName()});
        //get the row to update.
        QuartzLock lock = ACQUIRED_LOCKS.get().get(lockName);
        //should not be null. if null, there is a problem on lock!
        lock.update(lock); //on updating the row, we remove the lock.
        boolean released = ACQUIRED_LOCKS.get().remove(lockName) != null;
        LOG.log(Level.INFO, "Released lock {0}=({1}) acquired by: {2}", new Object[]{lock, released, Thread.currentThread().getName()});
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, lockName, ex);
      throw new LockException(ex);
    }
  }

  @Override
  public boolean hasLock(String lockName) {
    return ACQUIRED_LOCKS.get() != null && ACQUIRED_LOCKS.get().containsKey(lockName);
  }

}
