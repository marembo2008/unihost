/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import java.text.ParseException;
import java.util.TimeZone;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;

@Entity
@Table(name = "U_SCHEDULER_CRON_TRIGGERS")
public class QuartzCronTrigger extends QuartzTrigger implements Serializable {

  private static final long serialVersionUID = 13438439389483L;
  private String cronExpression;
  @Lob
  private TimeZone timeZone;

  @Override
  protected void edit(Trigger trigger) {
    if (trigger instanceof CronTrigger) {
      CronTrigger st = (CronTrigger) trigger;
      timeZone = st.getTimeZone();
      cronExpression = st.getCronExpression();
    }
  }

  @Override
  protected void initTrigger() {
    try {
      CronTriggerImpl st;
      trigger_ = st = new CronTriggerImpl();
      st.setCronExpression(cronExpression);
      st.setTimeZone(timeZone);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  @Override
  public boolean canFireNow() {
    return getMisfireInstruction() == CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
  }

}
