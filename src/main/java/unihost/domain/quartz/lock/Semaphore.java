/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.lock;

/**
 *
 * @author marembo
 */
public interface Semaphore {

  void lock(String lockName) throws LockException;

  void release(String lockName) throws LockException;

  boolean hasLock(String lockName);
}
