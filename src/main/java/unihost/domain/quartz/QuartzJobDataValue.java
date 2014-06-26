/**
 * $Id$
 */
package unihost.domain.quartz;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "U_SCHEDULER_JOB_DATA_VALUES")
public class QuartzJobDataValue extends unihost.domain.quartz.QuartzObject implements Serializable, Cloneable {

  private static final long serialVersionUID = 34894843843843L;
  @Column(name = "data_key")
  @NotNull
  private String key;
  @NotNull
  @Column(name = "data_value")
  @Lob
  private Serializable value;

  public QuartzJobDataValue(String key, Serializable value) {
    this.key = key;
    this.value = value;
  }

  public QuartzJobDataValue() {
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setValue(Serializable value) {
    this.value = value;
  }

  public Serializable getValue() {
    return value;
  }

  @Override
  public String toString() {
    return key + "=>" + value;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 79 * hash + Objects.hashCode(this.key);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final QuartzJobDataValue other = (QuartzJobDataValue) obj;
    return Objects.equals(this.key, other.key);
  }

}
