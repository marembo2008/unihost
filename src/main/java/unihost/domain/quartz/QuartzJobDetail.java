/**
 * $Id$
 */
package unihost.domain.quartz;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.impl.JobDetailImpl;
import unihost.Domain;

@Entity
@NamedQueries({
  @NamedQuery(name = "QuartzJobDetail.findJobDetails",
          query = "SELECT jd FROM QuartzJobDetail jd WHERE jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzJobDetail.findJobKeys",
          query = "SELECT jd.jobKey FROM QuartzJobDetail jd WHERE jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzJobDetail.findJobGroups",
          query = "SELECT jd.jobKey.group FROM QuartzJobDetail jd WHERE jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzJobDetail.findJobDetail",
          query = "SELECT jd FROM QuartzJobDetail jd "
          + "WHERE jd.group = :jobGroup AND jd.name = :jobName and jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzJobDetail.checkJobDetailExists",
          query = "SELECT COUNT(jd) FROM QuartzJobDetail jd "
          + "WHERE jd.group = :jobGroup AND jd.name = :jobName and jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance"),
  @NamedQuery(name = "QuartzJobDetail.countJobDetails",
          query = "SELECT COUNT(jd) FROM QuartzJobDetail jd WHERE jd.jobClass IS NOT NULL AND jd.instance.uuid = :instance")
})
@Table(name = "U_SCHEDULER_JOB_DETAILS")
public class QuartzJobDetail extends unihost.domain.quartz.QuartzObject {

  private static final long serialVersionUID = 589359585749L;
  private QuartzJobKey jobKey;
  @Column(length = 80, name = "KEY_NAME")
  private String name;
  @Column(name = "KEY_GROUP")
  private String group;
  private String description;
  @OneToOne(cascade = CascadeType.ALL)
  private QuartzJobData jobData;
  private boolean shouldRecover;
  private boolean durable;
  private Class jobClass;
  @OneToOne
  private QuartzInstance instance;
  @Transient
  private JobKey jobKey_;
  @Transient
  private JobDetail jobDetail;
  private boolean allowConcurrentExecution;

  public QuartzJobDetail() {
    allowConcurrentExecution = true;
  }

  public QuartzJobDetail(JobDetail jobDetail, QuartzInstance instance) {
    this();
    setJobDetail(jobDetail);
    this.instance = instance;
  }

  @PrePersist
  @PreUpdate
  void onSave() {
    this.jobClass = jobDetail.getJobClass();
    this.description = jobDetail.getDescription();
    if (this.jobData != null) {
      this.jobData.setDataMap(jobDetail.getJobDataMap());
    } else {
      this.jobData = new QuartzJobData(jobDetail.getJobDataMap(), description);
    }
    this.jobKey_ = jobDetail.getKey();
    this.jobKey = QuartzJobKey.getInstance(jobKey_);
    this.group = jobKey.getGroup();
    this.name = jobKey.getName();
    this.shouldRecover = jobDetail.requestsRecovery();
    this.durable = jobDetail.isDurable();
    this.allowConcurrentExecution = !jobDetail.isConcurrentExectionDisallowed();
  }

  @PostLoad
  void initJobKey() {
    jobKey_ = new JobKey(jobKey.getName(), jobKey.getGroup());
    JobDataMap dataMap_ = null;
    if (jobData != null) {
      dataMap_ = jobData.getDataMap();
    }
    JobDetailImpl job;
    jobDetail = job = new JobDetailImpl();
    job.setDescription(description);
    job.setGroup(jobKey.getGroup());
    job.setJobClass(jobClass);
    job.setJobDataMap(dataMap_);
    job.setKey(jobKey_);
    job.setName(jobKey.getName());
    job.setRequestsRecovery(shouldRecover);
    job.setDurability(durable);
  }

  public static QuartzJobDetail createInstance(JobDetail jobDetail, QuartzInstance instance) {
    return new QuartzJobDetail(jobDetail, instance);
  }

  public final void setJobDetail(JobDetail jobDetail) {
    this.jobDetail = jobDetail;
  }

  public JobDetail getJobDetail() {
    return jobDetail;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
    getJobData().setDescription(description);
  }

  public Class<? extends Job> getJobClass() {
    return jobClass;
  }

  public JobDataMap getJobDataMap() {
    return jobData != null ? jobData.getDataMap() : null;
  }

  public JobKey getJobKey() {
    return jobKey_;
  }

  public void setJobKey(JobKey jobKey_) {
    this.jobKey_ = jobKey_;
    this.jobKey = QuartzJobKey.getInstance(jobKey_);
  }

  public void setJobClass(Class jobClass) {
    this.jobClass = jobClass;
  }

  public void setJobDataMap(JobDataMap dataMap) {
    getJobData().setDataMap(dataMap);
  }

  private QuartzJobData getJobData() {
    if (jobData == null) {
      jobData = new QuartzJobData();
    }
    return jobData;
  }

  public static QuartzJobDetail find(JobKey key, QuartzInstance instance) {
    List<QuartzJobDetail> jobDetails = Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.findJobDetail", QuartzJobDetail.class)
            .setParameter("jobGroup", key.getGroup())
            .setParameter("jobName", key.getName())
            .setParameter("instance", instance.getUuid())
            .getResultList();
    QuartzJobDetail jd = jobDetails.isEmpty() ? null : jobDetails.get(0);
    if (jd != null) {
      jd.refresh(LockModeType.READ);
    }
    return jd;
  }

  public static void removeJob(JobKey key, QuartzInstance instance) throws JobPersistenceException {
    QuartzJobDetail jobDetail = find(key, instance);
    if (jobDetail == null) {
      throw new JobPersistenceException("Job " + key + "+does not exists");
    }
    Domain.getEntityManager().remove(jobDetail);
  }

  public static boolean exists(JobKey key, QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.checkJobDetailExists", Long.class)
            .setParameter("jobGroup", key.getGroup())
            .setParameter("jobName", key.getName())
            .setParameter("instance", instance.getUuid())
            .getSingleResult() > 0;
  }

  public static int getJobCount(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.countJobDetails", Long.class)
            .setParameter("instance", instance.getUuid())
            .getSingleResult().intValue();
  }

  public static List<QuartzJobDetail> findJobDetails(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.findJobDetails")
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  public static List<QuartzJobKey> findJobKeys(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.findJobKeys")
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  public static List<String> findJobGroups(QuartzInstance instance) {
    return Domain.getEntityManager()
            .createNamedQuery("QuartzJobDetail.findJobGroups")
            .setParameter("instance", instance.getUuid())
            .getResultList();
  }

  @Override
  public synchronized QuartzJobDetail update() {
    JobDetail td = jobDetail;
    refresh(LockModeType.READ);
    setJobDetail(td);
    return (QuartzJobDetail) super.update();
  }

}
