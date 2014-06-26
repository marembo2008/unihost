/**
 * $Id$
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.quartz.impl.calendar.HolidayCalendar;

@Entity
public class QuartzHolidayCalendar extends QuartzCalendar {

  public static class ExcludedDays extends ArrayList<Calendar> implements Serializable {

    public ExcludedDays(int initialCapacity) {
      super(initialCapacity);
    }

    public ExcludedDays() {
    }

    public ExcludedDays(Collection<? extends Calendar> c) {
      super(c);
    }

  }
  @Lob
  private ExcludedDays excludedDays;

  @Override
  protected void onLoadSelf() {
    HolidayCalendar hc;
    this.calendar = hc = new HolidayCalendar();
    if (excludedDays != null) {
      for (Calendar c : excludedDays) {
        hc.addExcludedDate(c.getTime());
      }
    }
  }

  @Override
  protected void onSaveSelf() {
    HolidayCalendar hc = (HolidayCalendar) this.calendar;
    if (excludedDays == null) {
      excludedDays = new ExcludedDays();
    }
    for (Date d : hc.getExcludedDates()) {
      Calendar c = Calendar.getInstance();
      c.setTime(d);
      excludedDays.add(c);
    }
  }

  public QuartzHolidayCalendar() {
    excludedDays = new ExcludedDays();
  }

  public List<Calendar> getExcludedDays() {
    return excludedDays;
  }

  public void setExcludedDays(List<Calendar> excludedDays) {
    this.excludedDays = new ExcludedDays(excludedDays);
  }
}
