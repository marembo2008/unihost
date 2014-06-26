/**
 * $Id$
 */
package unihost.domain.quartz;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.quartz.JobKey;

@Embeddable
@Table(name = "U_SCHEDULER_JOB_KEYS")
public class QuartzJobKey implements Serializable, Cloneable {

    private static final long serialVersionUID = -3473474734391L;

    @Column(length = 80, name = "KEY_NAME")
    @NotNull(message = "You must specify the job key name")
    private String name;
    @Column(name = "KEY_GROUP")
    private String group;

    public QuartzJobKey(String name, String group) {
        this.name = name;
        this.group = group;
    }

    public QuartzJobKey() {
        name = UUID.randomUUID().toString();
        group = "DEFAULT";
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
        int hash = 3;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 97 * hash + (this.group != null ? this.group.hashCode() : 0);
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
        final QuartzJobKey other = (QuartzJobKey) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return !((this.group == null) ? (other.group != null) : !this.group.equals(other.group));
    }

    @Override
    public String toString() {
        return group + "." + name;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    protected QuartzJobKey clone() throws CloneNotSupportedException {
        return new QuartzJobKey(name, group);
    }

    public static QuartzJobKey getInstance(JobKey key) {
        return new QuartzJobKey(key.getName(), key.getGroup());
    }

    public JobKey getKey() {
        return new JobKey(name, group);
    }
}
