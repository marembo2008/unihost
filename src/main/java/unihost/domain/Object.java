/*
 * $Id: Object.java 554 2014-02-16 19:23:03Z vollbsve $
 */
package unihost.domain;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import unihost.Domain;

/**
 * @author Sven Vollbehr
 */
@MappedSuperclass
@XmlType
public abstract class Object implements Serializable {

  @Id
  @Column(name = "UUID", length = 36, updatable = false, nullable = false)
  @XmlAttribute
  @XmlSchemaType(name = "uuid")
  private String uuid;
  @Version
  @Column(name = "VERSION")
  @XmlTransient
  private int version;

  public String getUuid() {
    return uuid;
  }

  public Object setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @PrePersist
  private void randomUuid() {
    if (uuid == null) {
      uuid = UUID.randomUUID().toString();
    }
  }

  public synchronized final Object create() {
    Boolean transactionUnderExternalControl = Domain.getEntityManager().getTransaction().isActive();
    try {
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().begin();
      }
      Domain.getEntityManager().persist(this);
      Domain.getEntityManager().flush();
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().commit();
      }
    } finally {
      if (!transactionUnderExternalControl && Domain.getEntityManager().getTransaction().isActive()) {
        Domain.getEntityManager().getTransaction().rollback();
      }
    }
    return this;
  }

  public synchronized Object update() {
    Boolean transactionUnderExternalControl = Domain.getEntityManager().getTransaction().isActive();
    try {
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().begin();
      }
      Domain.getEntityManager().lock(this, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
      Domain.getEntityManager().merge(this);
      Domain.getEntityManager().flush();
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().commit();
      }
    } finally {
      if (!transactionUnderExternalControl && Domain.getEntityManager().getTransaction().isActive()) {
        Domain.getEntityManager().getTransaction().rollback();
      }
    }
    return this;
  }

  public synchronized final Object update(Object domainObject) {
    Boolean transactionUnderExternalControl = Domain.getEntityManager().getTransaction().isActive();
    try {
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().begin();
      }
      Domain.getEntityManager().merge(domainObject);
      Domain.getEntityManager().flush();
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().commit();
      }
    } finally {
      if (!transactionUnderExternalControl && Domain.getEntityManager().getTransaction().isActive()) {
        Domain.getEntityManager().getTransaction().rollback();
      }
    }
    return this;
  }

  public synchronized final Object refresh() {
    return refresh(null);
  }

  public synchronized final Object refresh(final LockModeType lockModeType) {
    Boolean transactionUnderExternalControl = Domain.getEntityManager().getTransaction().isActive();
    try {
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().begin();
      }
      if (lockModeType != null) {
        Domain.getEntityManager().refresh(this, lockModeType);
      } else {
        Domain.getEntityManager().refresh(this);
      }
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().commit();
      }
    } finally {
      if (!transactionUnderExternalControl && Domain.getEntityManager().getTransaction().isActive()) {
        Domain.getEntityManager().getTransaction().rollback();
      }
    }
    return this;
  }

  public synchronized void delete() {
    Boolean transactionUnderExternalControl = Domain.getEntityManager().getTransaction().isActive();
    try {
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().begin();
      }
      Domain.getEntityManager().remove(this);
      if (!transactionUnderExternalControl) {
        Domain.getEntityManager().getTransaction().commit();
      }
    } finally {
      if (!transactionUnderExternalControl && Domain.getEntityManager().getTransaction().isActive()) {
        Domain.getEntityManager().getTransaction().rollback();
      }
    }
  }

  public static synchronized final void beginTransaction() {
    if (!Domain.getEntityManager().getTransaction().isActive()) {
      Domain.getEntityManager().getTransaction().begin();
    }
  }

  public static synchronized final void commitTransaction() {
    if (!Domain.getEntityManager().getTransaction().isActive()) {
      Domain.getEntityManager().getTransaction().commit();
    }
  }

  public static final void rollbackTransaction() {
    if (!Domain.getEntityManager().getTransaction().isActive()) {
      Domain.getEntityManager().getTransaction().rollback();
    }
  }

  @Override
  public boolean equals(final java.lang.Object o) {
    if (o instanceof Object && ((Object) o).getUuid() != null) {
      return ((Object) o).getUuid().equals(this.uuid);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (uuid == null) {
      uuid = UUID.randomUUID().toString();
    }
    return this.uuid.hashCode();
  }
}
