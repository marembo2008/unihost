/**
 * $Id$
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.quartz.impl.calendar.AnnualCalendar;
import unihost.domain.quartz.calendar.QuartzHolidayCalendar.ExcludedDays;

@Entity
public class QuartzAnnualCalendar extends QuartzCalendar implements Serializable {

  @Lob
  private ExcludedDays excludeDays;
  // true, if excludeDays is sorted
  private boolean dataSorted;

  public QuartzAnnualCalendar() {
    excludeDays = new ExcludedDays();
    dataSorted = false;
  }

  @Override
  protected void onLoadSelf() {
    AnnualCalendar cal;
    this.calendar = cal = new AnnualCalendar();
    cal.setDaysExcluded(new ArrayList<>(excludeDays));
  }

  @Override
  protected void onSaveSelf() {
    //set the excluded days.
    AnnualCalendar cal = (AnnualCalendar) this.calendar;
    if (cal.getDaysExcluded() != null) {
      this.excludeDays = new ExcludedDays(cal.getDaysExcluded());
    }
  }

  public List<Calendar> getExcludeDays() {
    return excludeDays;
  }

  public void setExcludeDays(List<Calendar> excludeDays) {
    this.excludeDays = new ExcludedDays(excludeDays);
  }

  public boolean isDataSorted() {
    return dataSorted;
  }

  public void setDataSorted(boolean dataSorted) {
    this.dataSorted = dataSorted;
  }
}
