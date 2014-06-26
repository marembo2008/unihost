/**
 * $Id$
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import java.text.ParseException;
import java.util.TimeZone;
import javax.persistence.Column;
import javax.persistence.Entity;
import org.quartz.impl.calendar.CronCalendar;

@Entity
public class QuartzCronCalendar extends QuartzCalendar implements Serializable {

  @Column(name = "CRON_EXPR")
  private String expresssion;

  @Override
  protected void onLoadSelf() {
    try {
      if (timeZone == null) {
        timeZone = TimeZone.getDefault();
      }
      this.calendar = new CronCalendar(expresssion);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  protected void onSaveSelf() {
    this.expresssion = ((CronCalendar) this.calendar).getCronExpression().getCronExpression();
    this.timeZone = ((CronCalendar) this.calendar).getTimeZone();
  }

  public String getExpresssion() {
    return expresssion;
  }

  public void setExpresssion(String expresssion) {
    this.expresssion = expresssion;
  }
}
