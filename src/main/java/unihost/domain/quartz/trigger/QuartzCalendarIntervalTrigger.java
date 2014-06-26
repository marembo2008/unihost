/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.trigger;

import java.util.TimeZone;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;

/**
 *
 * @author marembo
 */
@Entity
@Table(name = "U_SCHEDULER_CALENDAR_INTERVAL_TRIGGERS")
public class QuartzCalendarIntervalTrigger extends QuartzTrigger {

  private int repeatInterval;
  private DateBuilder.IntervalUnit repeatIntervalUnit = DateBuilder.IntervalUnit.DAY;
  private int timesTriggered;
  private boolean preserveHourOfDayAcrossDaylightSavings;
  private boolean skipDayIfHourDoesNotExist;
  @Lob
  private TimeZone timeZone;

  @Override
  protected void initTrigger() {
    CalendarIntervalTriggerImpl st;
    trigger_ = st = new CalendarIntervalTriggerImpl();
    st.setRepeatInterval(repeatInterval);
    st.setTimesTriggered(timesTriggered);
    st.setRepeatIntervalUnit(repeatIntervalUnit);
    st.setPreserveHourOfDayAcrossDaylightSavings(preserveHourOfDayAcrossDaylightSavings);
    st.setSkipDayIfHourDoesNotExist(skipDayIfHourDoesNotExist);
    st.setTimeZone(timeZone);
  }

  @Override
  protected void edit(Trigger trigger) {
    if (trigger instanceof CalendarIntervalTrigger) {
      CalendarIntervalTrigger st = (CalendarIntervalTrigger) trigger;
      repeatInterval = st.getRepeatInterval();
      repeatIntervalUnit = st.getRepeatIntervalUnit();
      timesTriggered = st.getTimesTriggered();
      preserveHourOfDayAcrossDaylightSavings = st.isPreserveHourOfDayAcrossDaylightSavings();
      skipDayIfHourDoesNotExist = st.isSkipDayIfHourDoesNotExist();
      timeZone = st.getTimeZone();
    }
  }

  @Override
  public boolean canFireNow() {
    return getMisfireInstruction() == CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
  }

}
