/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import org.quartz.TriggerKey;

@Embeddable
public class QuartzTriggerKey implements Serializable {

  public static QuartzTriggerKey getInstance(TriggerKey trKey) {
    return new QuartzTriggerKey(trKey.getName(), trKey.getGroup());
  }

  @Column(name = "TRIGGER_NAME", length = 50)
  @NotNull
  private String name;
  @Column(name = "TRIGGER_GROUP", length = 50)
  @NotNull
  private String group;

  public QuartzTriggerKey(String name, String group) {
    this.name = name;
    this.group = group;
  }

  public QuartzTriggerKey() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  @SuppressWarnings("null")
  public int hashCode() {
    int hash = 5;
    hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
    hash = 59 * hash + (this.group != null ? this.group.hashCode() : 0);
    return hash;
  }

  @Override
  @SuppressWarnings("null")
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final QuartzTriggerKey other = (QuartzTriggerKey) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
      return false;
    }
    return !((this.group == null) ? (other.group != null) : !this.group.equals(other.group));
  }

  public TriggerKey getKey() {
    return new TriggerKey(name, group);
  }
}
