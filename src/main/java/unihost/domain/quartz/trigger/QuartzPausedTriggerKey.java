/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
public class QuartzPausedTriggerKey implements Serializable {

    private static final long serialVersionUID = 134900898L;
    private String schedulerName;
    private String groupName;

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.schedulerName != null ? this.schedulerName.hashCode() : 0);
        hash = 59 * hash + (this.groupName != null ? this.groupName.hashCode() : 0);
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
        final QuartzPausedTriggerKey other = (QuartzPausedTriggerKey) obj;
        if ((this.schedulerName == null) ? (other.schedulerName != null) : !this.schedulerName.equals(other.schedulerName)) {
            return false;
        }
        return !((this.groupName == null) ? (other.groupName != null) : !this.groupName.equals(other.groupName));
    }
}
