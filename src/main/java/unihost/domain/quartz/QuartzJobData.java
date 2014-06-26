/**
 * $Id$
 */
package unihost.domain.quartz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.quartz.JobDataMap;

@Entity
@Table(name = "U_SCHEDULER_JOB_DATAS")
public class QuartzJobData extends unihost.domain.quartz.QuartzObject implements Serializable, Cloneable {

  private static final long serialVersionUID = 6788556751L;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<QuartzJobDataValue> dataMap;
  private String description;
  @Transient
  private JobDataMap dataMap_;

  public QuartzJobData(JobDataMap dataMap, String description) {
    this.dataMap_ = dataMap;
    this.description = description;
  }

  public QuartzJobData(JobDataMap dataMap) {
    this.dataMap_ = dataMap;
  }

  public QuartzJobData() {
  }

  @PostLoad
  void setDataMap() {
    dataMap_ = new JobDataMap();
    for (QuartzJobDataValue dv : dataMap) {
      dataMap_.put(dv.getKey(), dv.getValue());
    }
  }

  @PrePersist
  @PreUpdate
  void save() {
    for (Map.Entry<String, java.lang.Object> value : dataMap_.entrySet()) {
      QuartzJobDataValue jjdv = new QuartzJobDataValue(value.getKey(), (Serializable) value.getValue());
      addOrUpdate(jjdv);
    }
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  private void addOrUpdate(QuartzJobDataValue jjdv) {
    if (dataMap == null) {
      dataMap = new ArrayList<>();
    }
    boolean updated = false;
    for (ListIterator<QuartzJobDataValue> it = dataMap.listIterator(); it.hasNext();) {
      QuartzJobDataValue jjdv1 = it.next();
      if (jjdv1.equals(jjdv)) {
        /**
         * We only update the value.
         */
        jjdv1.setValue(jjdv.getValue());
        updated = true;
        break;
      }
    }
    if (!updated) {
      dataMap.add(jjdv);
    }
  }

  public void setDataMap(JobDataMap dataMap_) {
    this.dataMap_ = dataMap_;
  }

  public JobDataMap getDataMap() {
    return dataMap_;
  }

  @Override
  public String toString() {
    return dataMap + "";
  }

}
