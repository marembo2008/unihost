/**
 * $Id$
 */
package unihost.domain.quartz.calendar;

import java.util.Calendar;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.quartz.impl.calendar.DailyCalendar;

@Entity
public class QuartzDailyCalendar extends QuartzCalendar {

  @Temporal(TemporalType.TIME)
  @NotNull
  @Column(name = "RANGE_START_TIME")
  private Calendar startingTime;
  @Temporal(TemporalType.TIME)
  @NotNull
  @Column(name = "RANGE_END_TIME")
  private Calendar endingTime;

  @Override
  protected void onLoadSelf() {
    this.calendar = new DailyCalendar(startingTime, endingTime);
  }

  @Override
  protected void onSaveSelf() {
    DailyCalendar cal = (DailyCalendar) this.calendar;
    Calendar now = Calendar.getInstance();
    now.set(Calendar.HOUR_OF_DAY, 0);
    now.set(Calendar.MINUTE, 0);
    now.set(Calendar.SECOND, 0);
    now.set(Calendar.MILLISECOND, 0);
    this.startingTime = Calendar.getInstance();
    this.endingTime = Calendar.getInstance();
    this.startingTime.setTimeInMillis(cal.getTimeRangeStartingTimeInMillis(now.getTimeInMillis()));
    this.endingTime.setTimeInMillis(cal.getTimeRangeEndingTimeInMillis(now.getTimeInMillis()));
  }

  public Calendar getStartingTime() {
    return startingTime;
  }

  public void setStartingTime(Calendar startingTime) {
    this.startingTime = startingTime;
  }

  public Calendar getEndingTime() {
    return endingTime;
  }

  public void setEndingTime(Calendar endingTime) {
    this.endingTime = endingTime;
  }
}
